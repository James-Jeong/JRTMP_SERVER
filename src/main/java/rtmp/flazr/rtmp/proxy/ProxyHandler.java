package rtmp.flazr.rtmp.proxy;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;

import java.net.InetSocketAddress;

public class ProxyHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private final ClientSocketChannelFactory cf;
    private final String remoteHost;
    private final int remotePort;

    private volatile Channel outboundChannel;

    public ProxyHandler(ClientSocketChannelFactory cf, String remoteHost, int remotePort) {
        this.cf = cf;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        try {
            final Channel inboundChannel = e.getChannel();
            RtmpManager.getInstance().getChannels().add(inboundChannel);
            inboundChannel.setReadable(false);

            ClientBootstrap cb = new ClientBootstrap(cf);
            cb.getPipeline().addLast("handshaker", new ProxyHandshakeHandler());
            cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel()));

            ChannelFuture f = cb.connect(new InetSocketAddress(remoteHost, remotePort));
            outboundChannel = f.getChannel();
            f.addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("connected to remote host: {}, port: {}", remoteHost, remotePort);
                    inboundChannel.setReadable(true);
                } else {
                    inboundChannel.close();
                }
            });
        } catch (Exception ex) {
            logger.warn("ProxyHandler.channelOpen.Exception", ex);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelBuffer in = (ChannelBuffer) e.getMessage();
        //logger.debug(">>> [{}] {}", in.readableBytes(), ChannelBuffers.hexDump(in));
        outboundChannel.write(in);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        logger.info("closing inbound channel");
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.info("inbound exception: {}", e.getCause().getMessage());
        closeOnFlush(e.getChannel());
    }

    private static class OutboundHandler extends SimpleChannelUpstreamHandler {

        private final Channel inboundChannel;

        public OutboundHandler(Channel inboundChannel) {
            logger.info("opening outbound channel");
            this.inboundChannel = inboundChannel;
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
            ChannelBuffer in = (ChannelBuffer) e.getMessage();
            //logger.debug("<<< [{}] {}", in.readableBytes(), ChannelBuffers.hexDump(in));
            inboundChannel.write(in);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
            logger.info("closing outbound channel");
            closeOnFlush(inboundChannel);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
            logger.info("outbound exception: {}", e.getCause().getMessage());
            closeOnFlush(e.getChannel());
        }

    }

    static void closeOnFlush(Channel ch) {
        if (ch.isConnected()) {
            ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}