package me.iblur.shuttle.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import me.iblur.shuttle.conf.Configuration;

import java.net.InetSocketAddress;

public abstract class ProxyConnectHandler<I> extends SimpleChannelInboundHandler<I> {

    private final Bootstrap bootstrap = new Bootstrap();

    protected void connectRemoteAddress(Channel inboundChannel, String remoteHost, int remotePort,
            ChannelFutureListener listener, Promise<Channel> promise) {
        Configuration configuration = inboundChannel.<Configuration>attr(AttributeKey.valueOf("configuration")).get();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeout())
                .handler(new DirectClientHandler(promise));
        if (null != configuration.getDns() && configuration.getDns().length() > 0) {
            DnsNameResolverBuilder dnsResolverBuilder = new DnsNameResolverBuilder(inboundChannel.eventLoop());
            dnsResolverBuilder.channelType(NioDatagramChannel.class).eventLoop(inboundChannel.eventLoop())
                    .nameServerProvider(new SingletonDnsServerAddressStreamProvider(
                            new InetSocketAddress(configuration.getDns(), Configuration.DNS_PORT)));
            bootstrap.resolver(new DnsAddressResolverGroup(dnsResolverBuilder));
        }
        bootstrap.connect(remoteHost, remotePort).addListener(listener);
    }

}
