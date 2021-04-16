package me.iblur.shuttle.socks;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;

/**
 * @since 2021-04-15 16:24
 */
@ChannelHandler.Sharable
public class SocksProxyRequestHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        switch (message.version()) {
            case SOCKS4a:
                Socks4CommandRequest request = (Socks4CommandRequest) message;
                if (request.type() == Socks4CommandType.CONNECT) {
                    ctx.pipeline().addLast(new SocksConnectHandler());
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(message);
                } else {
                    ctx.close();
                }
                break;
            case SOCKS5:
                if (message instanceof Socks5InitialRequest) {
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.channel().writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                } else if (message instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) message;
                    if (socks5CommandRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new SocksConnectHandler());
                        ctx.pipeline().remove(this);
                        ctx.fireChannelRead(message);
                    } else {
                        ctx.close();
                    }
                }
                break;
            default:
                ctx.close();
                break;
        }
    }
}
