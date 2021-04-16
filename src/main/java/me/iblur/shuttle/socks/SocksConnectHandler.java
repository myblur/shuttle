package me.iblur.shuttle.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * @since 2021-04-15 16:54
 */
public class SocksConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) throws Exception {
        final Promise<Channel> promise = ctx.executor().newPromise();
        final Channel inboundChannel = ctx.channel();
        String host;
        int port;
        ChannelFutureListener connectListener;
        if (msg.version() == SocksVersion.SOCKS4a) {
            Socks4CommandRequest socks4CommandRequest = (Socks4CommandRequest) msg;
            host = socks4CommandRequest.dstAddr();
            port = socks4CommandRequest.dstPort();
            promise.addListener((GenericFutureListener<Future<Channel>>) f1 -> {
                if (f1.isSuccess()) {
                    final Channel outboundChannel = f1.getNow();
                    inboundChannel.writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS))
                            .addListener(
                                    (ChannelFutureListener) f2 -> {
                                        outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));
                                        ctx.pipeline().remove(SocksConnectHandler.this);
                                        ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                    });
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(
                            Socks4CommandStatus.REJECTED_OR_FAILED));
                    ctx.channel().closeFuture().addListener(ChannelFutureListener.CLOSE);
                }
            });
            connectListener = future -> {
                if (!future.isSuccess()) {
                    ctx.channel()
                            .writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            };
        } else {
            Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) msg;
            host = socks5CommandRequest.dstAddr();
            port = socks5CommandRequest.dstPort();
            promise.addListener((GenericFutureListener<Future<Channel>>) f1 -> {
                if (f1.isSuccess()) {
                    final Channel outboundChannel = f1.getNow();
                    inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS,
                            socks5CommandRequest.dstAddrType())).addListener((ChannelFutureListener) f2 -> {
                        outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));
                        ctx.pipeline().remove(SocksConnectHandler.this);
                        ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                    });
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.FAILURE, socks5CommandRequest.dstAddrType()));
                    ctx.channel().closeFuture().addListener(ChannelFutureListener.CLOSE);
                }
            });
            connectListener = future -> {
                if (!future.isSuccess()) {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                            socks5CommandRequest.dstAddrType()));
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            };
        }
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));
        bootstrap.connect(host, port).addListener(connectListener);
    }
}