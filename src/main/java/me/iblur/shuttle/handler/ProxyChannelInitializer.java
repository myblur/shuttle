package me.iblur.shuttle.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import me.iblur.shuttle.conf.Configuration;

/**
 * @since 2021-04-15 15:01
 */
public class ProxyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Configuration configuration;

    public ProxyChannelInitializer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (configuration.isDebug()) {
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
        }
        pipeline.addLast(new ProxySelectorHandler());
    }
}
