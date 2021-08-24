package me.iblur.shuttle.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.*;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.PlatformDependent;
import me.iblur.shuttle.conf.AttributeKeys;
import me.iblur.shuttle.conf.Configuration;

public abstract class AbstractProxyConnectHandler<I> extends SimpleChannelInboundHandler<I> {


    protected void resolveDomain(Channel inboundChannel, Promise<String> promise, String queryDomain) {
        Configuration configuration = inboundChannel.attr(AttributeKeys.CONFIGURATION_ATTR_KEY).get();
        SslContext sslContext = inboundChannel.attr(AttributeKeys.SSL_CONTEXT_ATTR_KEY).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(inboundChannel.eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(sslContext.newHandler(ch.alloc(), configuration.getDot(),
                                Configuration.DEFAULT_DNS_OVER_TLS_PORT));
                        pipeline.addLast(new TcpDnsQueryEncoder());
                        pipeline.addLast(new TcpDnsResponseDecoder());
                        pipeline.addLast(new DoTResponseHandler(promise));
                    }
                });
        bootstrap.connect(configuration.getDot(), Configuration.DEFAULT_DNS_OVER_TLS_PORT).addListener(
                (ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        final Channel channel = future.channel();
                        final int randomId = PlatformDependent.threadLocalRandom().nextInt(65536 - 1) + 1;
                        DnsQuery dnsQuery = new DefaultDnsQuery(randomId, DnsOpCode.QUERY).setRecord(
                                DnsSection.QUESTION, new DefaultDnsQuestion(queryDomain, DnsRecordType.A));
                        channel.writeAndFlush(dnsQuery).addListener((ChannelFutureListener) future1 -> {
                            if (!future1.isSuccess()) {
                                promise.setFailure(future1.cause());
                                future1.channel().close();
                            }
                        });
                    } else {
                        promise.setFailure(future.cause());
                    }
                });

    }

    protected void connectRemoteAddress(Channel inboundChannel, String remoteHost, int remotePort,
            ChannelFutureListener listener, Promise<Channel> promise) {
        Configuration configuration = inboundChannel.attr(AttributeKeys.CONFIGURATION_ATTR_KEY).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeout())
                .handler(new DirectClientHandler(promise));
        if (inboundChannel.hasAttr(AttributeKeys.ADDRESS_RESOLVER_GROUP_ATTR_KEY)) {
            bootstrap.resolver(inboundChannel.attr(AttributeKeys.ADDRESS_RESOLVER_GROUP_ATTR_KEY).get());
        }
        bootstrap.connect(remoteHost, remotePort).addListener(listener);
    }

}
