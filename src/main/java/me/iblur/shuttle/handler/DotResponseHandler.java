package me.iblur.shuttle.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.*;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * @author iblur 2021-08-24
 */
public class DotResponseHandler extends SimpleChannelInboundHandler<DefaultDnsResponse> {

    private final Logger log = LoggerFactory.getLogger(DotResponseHandler.class);

    private final Promise<String> promise;

    public DotResponseHandler(Promise<String> promise) {
        this.promise = promise;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final DefaultDnsResponse msg) throws Exception {
        try {
            String address = null;
            final DnsQuestion qRecord = msg.recordAt(DnsSection.QUESTION, 0);
            final int count = msg.count(DnsSection.ANSWER);
            for (int i = 0; i < count; i++) {
                final DnsRecord aRecord = msg.recordAt(DnsSection.ANSWER, i);
                if (aRecord.type() == DnsRecordType.A) {
                    DnsRawRecord raw = (DnsRawRecord) aRecord;
                    address = NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content()));
                    if (log.isDebugEnabled()) {
                        log.debug("domain name resolution result: {}->{}", qRecord.name(), address);
                    }
                    break;
                }
            }
            if (null == address) {
                promise.setFailure(new UnknownHostException(qRecord.name()));
            } else {
                promise.setSuccess(address);
            }
        } finally {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        promise.setFailure(cause);
        ctx.close();
    }
}
