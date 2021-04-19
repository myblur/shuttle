package me.iblur.shuttle.handler.socks;

import io.netty.util.AttributeKey;
import me.iblur.shuttle.conf.Configuration;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @since 2021-04-15 15:01
 */
public class SocksProxyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Configuration configuration;

    public SocksProxyChannelInitializer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (configuration.isDebug()) {
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
        }
        pipeline.addLast(new SocksPortUnificationServerHandler());
        pipeline.addLast(SocksProxyRequestHandler.INSTANCE);
        ch.attr(AttributeKey.valueOf("configuration")).set(configuration);
    }
}
