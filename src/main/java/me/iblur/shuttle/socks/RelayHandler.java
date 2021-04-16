package me.iblur.shuttle.socks;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @since 2021-04-15 17:21
 */
public class RelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel relayChannel;


    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        relayChannel.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
