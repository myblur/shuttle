package me.iblur.shuttle.handler.http;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import me.iblur.shuttle.handler.ProxyConnectHandler;
import me.iblur.shuttle.handler.ProxyRelayHandler;

@ChannelHandler.Sharable
public class HttpProxyConnectHandler extends ProxyConnectHandler<HttpObject> {

    private static final DefaultHttpResponse TUNNELING_OK = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(HttpResponseStatus.OK.code(), "Connection Established"));

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final HttpRequest httpRequest = (HttpRequest) msg;
            String remoteAddress = httpRequest.headers().get(HttpHeaderNames.HOST);
            if (null == remoteAddress || !remoteAddress.contains(":")) {
                remoteAddress = httpRequest.uri();
            }
            String[] hostAndPort = remoteAddress.split(":");
            Promise<Channel> promise = ctx.executor().newPromise();
            Channel inboundChannel = ctx.channel();
            if (httpRequest.method() == HttpMethod.CONNECT) {
                promise.addListener((GenericFutureListener<Future<Channel>>) f1 -> {
                    if (f1.isSuccess()) {
                        Channel outboundChannel = f1.getNow();
                        inboundChannel.writeAndFlush(TUNNELING_OK).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(final ChannelFuture future) throws Exception {
                                if (future.isSuccess()) {
                                    outboundChannel.pipeline().addLast(new ProxyRelayHandler(inboundChannel));
                                    inboundChannel.pipeline().remove(HttpProxyConnectHandler.this);
                                    inboundChannel.pipeline().addLast(new ProxyRelayHandler(outboundChannel));
                                    inboundChannel.pipeline().remove(HttpServerCodec.class);
                                } else {
                                    outboundChannel.close();
                                }
                            }
                        });
                    } else {
                        inboundChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.SERVICE_UNAVAILABLE)).addListener(ChannelFutureListener.CLOSE);
                    }
                });
            } else {
                promise.addListener((GenericFutureListener<Future<Channel>>) f1 -> {
                    if (f1.isSuccess()) {
                        Channel outboundChannel = f1.getNow();
                        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
                        embeddedChannel.writeOutbound(msg);
                        Object newRequest = embeddedChannel.readInbound();
                        outboundChannel.writeAndFlush(newRequest).addListener(f2 -> {
                            if (f2.isSuccess()) {
                                outboundChannel.pipeline().addLast(new ProxyRelayHandler(inboundChannel));
                                inboundChannel.pipeline().remove(HttpProxyConnectHandler.this);
                                inboundChannel.pipeline().addLast(new ProxyRelayHandler(outboundChannel));
                                inboundChannel.pipeline().remove(HttpServerCodec.class);
                            } else {
                                inboundChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.SERVICE_UNAVAILABLE))
                                        .addListener(ChannelFutureListener.CLOSE);
                            }
                        });
                    } else {
                        inboundChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.SERVICE_UNAVAILABLE)).addListener(ChannelFutureListener.CLOSE);
                    }
                });
            }
            ChannelFutureListener connectListener = future -> {
                if (!future.isSuccess()) {
                    inboundChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.SERVICE_UNAVAILABLE))
                            .addListener(ChannelFutureListener.CLOSE);
                }
            };
            String remoteHost = hostAndPort[0];
            int remotePort = Integer.parseInt(hostAndPort[1]);
            connectRemoteAddress(inboundChannel, remoteHost, remotePort, connectListener, promise);
        }
    }
}
