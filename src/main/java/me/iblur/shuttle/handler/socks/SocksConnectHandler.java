package me.iblur.shuttle.handler.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import me.iblur.shuttle.conf.Configuration;
import me.iblur.shuttle.handler.DirectClientHandler;
import me.iblur.shuttle.handler.RelayHandler;

import java.net.InetSocketAddress;

/**
 * @since 2021-04-15 16:54
 */
@ChannelHandler.Sharable
public class SocksConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Bootstrap bootstrap = new Bootstrap();

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
                ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS,
                        socks5CommandRequest.dstAddrType())).addListener((ChannelFutureListener) f2 -> {
                    if (f2.isSuccess()) {
                        outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));
                        ctx.pipeline().remove(SocksConnectHandler.this);
                        ctx.pipeline().addLast(new RelayHandler(outboundChannel));
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

        Configuration configuration = inboundChannel.attr(AttributeKey.<Configuration>valueOf("configuration")).get();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeout())
                .handler(new DirectClientHandler(promise));
        if (null != configuration.getDnsServer() && configuration.getDnsServer().length() > 0) {
            DnsNameResolverBuilder dnsResolverBuilder = new DnsNameResolverBuilder(inboundChannel.eventLoop());
            dnsResolverBuilder.channelType(NioDatagramChannel.class)
                    .nameServerProvider(new SingletonDnsServerAddressStreamProvider(
                            new InetSocketAddress(configuration.getDnsServer(), Configuration.DNS_PORT)));
            bootstrap.resolver(new DnsAddressResolverGroup(dnsResolverBuilder));
        }
        bootstrap.connect(host, port).addListener(connectListener);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
