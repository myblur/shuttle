package me.iblur.shuttle.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import me.iblur.shuttle.handler.http.HttpProxyConnectHandler;
import me.iblur.shuttle.handler.socks.SocksProxyRequestHandler;

import java.util.List;

public class ProxySelectorHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        final int readerIndex = in.readerIndex();
        if (in.writerIndex() == readerIndex) {
            return;
        }
        byte versionValue = in.getByte(readerIndex);
        SocksVersion socksVersion = SocksVersion.valueOf(versionValue);
        ChannelPipeline pipeline = ctx.pipeline();
        switch (socksVersion) {
            case SOCKS4a:
            case SOCKS5:
                pipeline.addLast(new SocksPortUnificationServerHandler());
                pipeline.addLast(SocksProxyRequestHandler.INSTANCE);
                break;
            default:
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpProxyConnectHandler());
                break;
        }
        pipeline.remove(this);
    }
}
