package me.iblur.shuttle.socks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @since 2021-04-15 17:21
 */
public class RelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel channel;


    public RelayHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (channel.isActive()) {
            channel.writeAndFlush(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.channel.closeFuture();
    }
}
