package me.iblur.shuttle.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Promise;
import me.iblur.shuttle.conf.AttributeKeys;
import me.iblur.shuttle.conf.Configuration;

public abstract class ProxyConnectHandler<I> extends SimpleChannelInboundHandler<I> {

    private final Bootstrap bootstrap = new Bootstrap();

    protected void connectRemoteAddress(Channel inboundChannel, String remoteHost, int remotePort,
            ChannelFutureListener listener, Promise<Channel> promise) {
        Configuration configuration = inboundChannel.attr(AttributeKeys.CONFIGURATION_ATTR_KEY).get();
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
