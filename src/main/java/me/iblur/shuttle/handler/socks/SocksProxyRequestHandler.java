package me.iblur.shuttle.handler.socks;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;

/**
 * @since 2021-04-15 16:24
 */
@ChannelHandler.Sharable
public class SocksProxyRequestHandler extends SimpleChannelInboundHandler<SocksMessage> {

    public static final SocksProxyRequestHandler INSTANCE = new SocksProxyRequestHandler();

    private SocksProxyRequestHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        if (message.version() == SocksVersion.SOCKS5) {
            if (message instanceof Socks5InitialRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.channel().writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH)).addListener(
                        (ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
                            }
                        });
            } else if (message instanceof Socks5CommandRequest) {
                Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) message;
                if (socks5CommandRequest.type() == Socks5CommandType.CONNECT) {
                    ctx.pipeline().addLast(new SocksProxyConnectHandler());
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(message);
                } else {
                    ctx.close();
                }
            }
        } else {
            ctx.close();
        }
    }
}
