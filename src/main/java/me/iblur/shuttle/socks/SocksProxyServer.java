package me.iblur.shuttle.socks;

import me.iblur.shuttle.conf.Configuration;
import me.iblur.shuttle.thread.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2021-04-15 14:49
 */
public class SocksProxyServer {

    private final Logger log = LoggerFactory.getLogger(SocksProxyServer.class);

    private final ServerBootstrap serverBootstrap;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final Configuration configuration;


    public SocksProxyServer(Configuration configuration) {
        this.serverBootstrap = new ServerBootstrap();
        this.configuration = configuration;
        this.bossGroup = new NioEventLoopGroup(1, new NamedThreadFactory(
                "Shuttle SocksProxy Boss[" + configuration.getHost() + ":" + configuration.getPort() + "]"));
        this.workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1, new NamedThreadFactory(
                "Shuttle SocksProxy Worker[" + configuration.getHost() + ":" + configuration.getPort() + "]"));
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 256)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new SocksProxyChannelInitializer(configuration));
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
