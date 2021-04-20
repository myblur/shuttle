package me.iblur.shuttle.handler.socks;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import me.iblur.shuttle.handler.ProxyConnectHandler;
import me.iblur.shuttle.handler.ProxyRelayHandler;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @since 2021-04-15 16:54
 */
@ChannelHandler.Sharable
public class SocksProxyConnectHandler extends ProxyConnectHandler<SocksMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) throws Exception {
        final Channel inboundChannel = ctx.channel();
        Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) msg;
        String host = socks5CommandRequest.dstAddr();
        int port = socks5CommandRequest.dstPort();
        final Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener((GenericFutureListener<Future<Channel>>) f1 -> {
            if (f1.isSuccess()) {
                final Channel outboundChannel = f1.getNow();
                InetAddress remoteAddress = ((InetSocketAddress) outboundChannel.remoteAddress()).getAddress();
                Socks5AddressType bndAddrType = remoteAddress instanceof Inet4Address ? Socks5AddressType.IPv4 :
                        Socks5AddressType.IPv6;
                String bndAddr = remoteAddress.getHostAddress();
                ctx.writeAndFlush(
                        new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndAddrType, bndAddr, port))
                        .addListener((ChannelFutureListener) f2 -> {
                            if (f2.isSuccess()) {
                                outboundChannel.pipeline().addLast(new ProxyRelayHandler(inboundChannel));
                                ctx.pipeline().remove(SocksProxyConnectHandler.this);
                                ctx.pipeline().addLast(new ProxyRelayHandler(outboundChannel));
                            } else {
                                ctx.close();
                            }
                        });
            } else {
                inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.FAILURE, socks5CommandRequest.dstAddrType()));
                inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        });
        ChannelFutureListener connectListener = future -> {
            if (!future.isSuccess()) {
                inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                        socks5CommandRequest.dstAddrType()));
                inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        };
        connectRemoteAddress(inboundChannel, host, port, connectListener, promise);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
