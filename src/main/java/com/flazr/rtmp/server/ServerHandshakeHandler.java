/*
 * Flazr <http://flazr.com> Copyright (C) 2009  Peter Thomas.
 *
 * This file is part of Flazr.
 *
 * Flazr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Flazr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Flazr.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.flazr.rtmp.server;

import com.flazr.rtmp.RtmpHandshake;
import com.flazr.rtmp.RtmpPublisher;
import com.flazr.util.Utils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ServerHandshakeHandler extends FrameDecoder implements ChannelDownstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandshakeHandler.class);
    
    private boolean rtmpe;
    private final RtmpHandshake handshake;
    private boolean partOneDone;
    private boolean handshakeDone;

    public ServerHandshakeHandler() {
        handshake = new RtmpHandshake();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer in) {
        logger.debug("### [READ BUFFER] len: {}", in.readableBytes());
        if(!partOneDone) {
            if(in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE + 1) {
                return null;
            }
            handshake.decodeClient0And1(in);
            rtmpe = handshake.isRtmpe();
            ChannelFuture future = Channels.succeededFuture(channel);

            ChannelBuffer s0Buffer = handshake.encodeServer0();
            logger.debug("s0Buffer len: {}", s0Buffer.readableBytes());
            Channels.write(ctx, future, s0Buffer);

            ChannelBuffer s1Buffer = handshake.encodeServer1();
            logger.debug("s1Buffer len: {}", s1Buffer.readableBytes());
            Channels.write(ctx, future, s1Buffer);

            ChannelBuffer s2Buffer = handshake.encodeServer2();
            logger.debug("s2Buffer len: {}", s2Buffer.readableBytes());
            Channels.write(ctx, future, s2Buffer);

            partOneDone = true;
        } else {
            if (!handshakeDone) {
                if (in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE) {
                    return null;
                }

                handshake.decodeClient2(in);
                handshakeDone = true;
                logger.info("handshake done, rtmpe: {}", rtmpe);
                if (Arrays.equals(handshake.getPeerVersion(), Utils.fromHex("00000000"))) {
                    final ServerHandler serverHandler = ctx.getPipeline().get(ServerHandler.class);
                    serverHandler.setAggregateModeEnabled(false);
                    logger.info("old client version, disabled 'aggregate' mode");
                }

                if (!rtmpe) {
                    channel.getPipeline().remove(this);
                }
            }

            /*ChannelFuture future = Channels.succeededFuture(channel);
            ChannelBuffer s2Buffer = handshake.encodeServer2();
            logger.debug("s2Buffer len: {}", s2Buffer.readableBytes());
            Channels.write(ctx, future, s2Buffer);*/
        }

        return in;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent ce) throws Exception {        
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            super.handleUpstream(ctx, ce);
            return;
        }
        final MessageEvent me = (MessageEvent) ce;
        if(me.getMessage() instanceof RtmpPublisher.Event) {
            super.handleUpstream(ctx, ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateIn(in);
        Channels.fireMessageReceived(ctx, in);
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent ce) {        
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            ctx.sendDownstream(ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateOut(in);
        ctx.sendDownstream(ce);
    }

}
