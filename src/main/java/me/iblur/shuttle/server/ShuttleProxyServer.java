package me.iblur.shuttle.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;
import me.iblur.shuttle.conf.AttributeKeys;
import me.iblur.shuttle.conf.Configuration;
import me.iblur.shuttle.handler.ProxyChannelInitializer;
import me.iblur.shuttle.thread.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @since 2021-04-15 14:49
 */
public class ShuttleProxyServer {

    private final Logger log = LoggerFactory.getLogger(ShuttleProxyServer.class);

    private final ServerBootstrap serverBootstrap;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final Configuration configuration;

    public ShuttleProxyServer(Configuration configuration) {
        this.serverBootstrap = new ServerBootstrap();
        this.configuration = configuration;
        this.bossGroup = new NioEventLoopGroup(1, new NamedThreadFactory(
                "Shuttle SocksProxy Boss[" + configuration.getHost() + ":" + configuration.getPort() + "]"));
        this.workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1, new NamedThreadFactory(
                "Shuttle SocksProxy Worker[" + configuration.getHost() + ":" + configuration.getPort() + "]"));
        this.serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ProxyChannelInitializer(configuration));
        setupChannelOption(configuration);
        setupChannelAttr(configuration);
    }

    private void setupChannelOption(Configuration configuration) {
        this.serverBootstrap
                .option(ChannelOption.SO_BACKLOG, configuration.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                //.childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
    }

    public void setupChannelAttr(Configuration configuration) {
        this.serverBootstrap.childAttr(AttributeKeys.CONFIGURATION_ATTR_KEY, configuration);
        if (null != configuration.getDns() && configuration.getDns().length() > 0) {
            DnsAddressResolverGroup addressResolverGroup =
                    new DnsAddressResolverGroup(NioDatagramChannel.class, new SingletonDnsServerAddressStreamProvider(
                            new InetSocketAddress(configuration.getDns(), Configuration.DNS_PORT)));
            this.serverBootstrap.childAttr(AttributeKeys.ADDRESS_RESOLVER_GROUP_ATTR_KEY, addressResolverGroup);
        }
    }

    public void start() throws InterruptedException {
        final ChannelFuture channelFuture = serverBootstrap.bind(configuration.getHost(), configuration.getPort());
        final Channel channel = channelFuture.sync().channel();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        log.info("Shuttle bind address[{}], start success...", channel.localAddress());
        channel.closeFuture().sync();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
