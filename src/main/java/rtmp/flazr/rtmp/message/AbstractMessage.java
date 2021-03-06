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

package rtmp.flazr.rtmp.message;

import org.jboss.netty.buffer.ChannelBuffer;
import rtmp.flazr.amf.Amf0Object;
import rtmp.flazr.rtmp.RtmpHeader;
import rtmp.flazr.rtmp.RtmpMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractMessage implements RtmpMessage {
    
    protected final RtmpHeader header;

    protected AbstractMessage() {
        header = new RtmpHeader(getMessageType());
    }

    protected AbstractMessage(RtmpHeader header, ChannelBuffer in) {
        this.header = header;
        decode(in);
    }

    @Override
    public RtmpHeader getHeader() {
        return header;
    }

    protected abstract MessageType getMessageType();

    @Override
    public String toString() {
        return header.toString() + ' ';
    }

    //==========================================================================

    public static Amf0Object object(Amf0Object object, Pair ... pairs) {
        if(pairs != null) {
            for(Pair pair : pairs) {
                object.put(pair.name, pair.value);
            }
        }
        return object;
    }

    public static Amf0Object object(Pair ... pairs) {
        return object(new Amf0Object(), pairs);
    }

    public static Map<String, Object> map(Map<String, Object> map, Pair ... pairs) {
        if(pairs != null) {
            for(Pair pair : pairs) {
                map.put(pair.name, pair.value);
            }
        }
        return map;
    }

    public static Map<String, Object> map(Pair ... pairs) {
        return map(new LinkedHashMap<String, Object>(), pairs);
    }

    public static class Pair {
        String name;
        Object value;
    }

    public static Pair pair(String name, Object value) {
        Pair pair = new Pair();
        pair.name = name;
        pair.value = value;
        return pair;
    }



}
