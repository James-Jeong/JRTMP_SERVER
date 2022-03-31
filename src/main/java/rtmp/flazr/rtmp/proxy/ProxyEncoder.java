package rtmp.flazr.rtmp.proxy;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import rtmp.flazr.rtmp.RtmpEncoder;
import rtmp.flazr.rtmp.RtmpMessage;

public class ProxyEncoder extends SimpleChannelUpstreamHandler {

    private final RtmpEncoder encoder = new RtmpEncoder();

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        Channels.fireMessageReceived(ctx, encoder.encode((RtmpMessage) e.getMessage()));
    }

}