package rtmp.flazr.rtmp.message;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import rtmp.flazr.rtmp.RtmpHeader;

public class Abort extends AbstractMessage {

    private int streamId;

    public Abort(final int streamId) {
        this.streamId = streamId;
    }

    public Abort(final RtmpHeader header, final ChannelBuffer in) {
        super(header, in);
    }

    public int getStreamId() {
        return streamId;
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.ABORT;
    }

    @Override
    public ChannelBuffer encode() {
        final ChannelBuffer out = ChannelBuffers.buffer(4);
        out.writeInt(streamId);
        return out;
    }

    @Override
    public void decode(ChannelBuffer in) {
        streamId = in.readInt();
    }

    @Override
    public String toString() {
        return super.toString() + "streamId: " + streamId;
    }

}
