package rtmp.flazr.rtmp.proxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.flazr.rtmp.RtmpDecoder;

public class ProxyHandshakeHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandshakeHandler.class);

    private int bytesWritten;
    private boolean handshakeDone;

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final ChannelBuffer in = (ChannelBuffer) e.getMessage();
        bytesWritten += in.readableBytes();
        if(!handshakeDone && bytesWritten >= 3073) {
            final int remaining = bytesWritten - 3073;
            if(remaining > 0) {
                Channels.fireMessageReceived(ctx, in.readBytes(remaining));
            }
            handshakeDone = true;
            logger.debug("bytes written {}, handshake complete, switching pipeline", bytesWritten);
            ctx.getPipeline().addFirst("encoder", new ProxyEncoder());
            ctx.getPipeline().addFirst("decoder", new RtmpDecoder());
            ctx.getPipeline().remove(this);
        }
        super.messageReceived(ctx, e);
    }

}