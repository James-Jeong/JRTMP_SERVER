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

package rtmp.flazr.rtmp.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.flazr.rtmp.RtmpHandshake;
import rtmp.flazr.util.Utils;

import java.util.Arrays;

public class ServerHandshakeHandler extends FrameDecoder implements ChannelDownstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandshakeHandler.class);

    private boolean rtmpe;
    private final RtmpHandshake handshake;
    private boolean partOneDone;
    private boolean handshakeDone;

    public ServerHandshakeHandler() {
        handshake = new RtmpHandshake();
        logger.debug("[ServerHandshakeHandler] NEW HANDSHAKE: {}", handshake);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer in) {
        if(!partOneDone) {
            if(in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE + 1) {
                return null;
            }
            handshake.decodeClient0And1(in);
            rtmpe = handshake.isRtmpe();
            ChannelFuture future = Channels.succeededFuture(channel);
            Channels.write(ctx, future, handshake.encodeServer0());
            Channels.write(ctx, future, handshake.encodeServer1());
            Channels.write(ctx, future, handshake.encodeServer2());
            partOneDone = true;
        }
        if (!handshakeDone) {
            if (in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE) {
                return null;
            }
            handshake.decodeClient2(in);
            handshakeDone = true;
            logger.debug("handshake done, rtmpe: {}", rtmpe);
            if (Arrays.equals(handshake.getPeerVersion(), Utils.fromHex("00000000"))) {
                final ServerHandler serverHandler = ctx.getPipeline().get(ServerHandler.class);
                serverHandler.setAggregateModeEnabled(false);
                logger.warn("old client version, disabled 'aggregate' mode");
            }

            if (!rtmpe) {
                channel.getPipeline().remove(this);
            }
        }
        return in;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent ce) throws Exception {
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
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
