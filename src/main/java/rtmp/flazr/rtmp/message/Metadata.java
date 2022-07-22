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
import rtmp.flazr.rtmp.RtmpHeader;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author yama
 *
 */
public abstract class Metadata extends AbstractMessage {

    protected String name;
    protected Object[] data;

    protected Metadata(String name, Object... data) {
        this.name = name;
        this.data = data;
        header.setSize(encode().readableBytes());
    }

    protected Metadata(RtmpHeader header, ChannelBuffer in) {
        super(header, in);
    }

    public Object getData(int index) {
        if(data == null || data.length < index + 1) {
            return null;
        }
        return data[index];
    }

    private Object getValue(String key) {
        final Map<String, Object> map = getMap(0);
        if(map == null) {
            return null;
        }
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
	public void setValue(String key, Object value) {
        if(data == null || data.length == 0) {
            data = new Object[]{new LinkedHashMap<String, Object>()};
        }
        if(data[0] == null) {
            data[0] = new LinkedHashMap<String, Object>();
        }
        final Map<String, Object> map = (Map<String, Object>) data[0];
        map.put(key, value);
    }

    @SuppressWarnings("unchecked")
	public Map<String, Object> getMap(int index) {
        return (Map<String, Object>) getData(index);
    }

    public String getString(String key) {
        return (String)getValue(key);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) getValue(key);
    }

    public Double getDouble(String key) {
        return (Double) getValue(key);
    }
    //
    public String getInnerValue(String key){
    	if(data == null || data.length == 1) {
    		 return "null";
        }
        final Map<String, Object> map = getMap(1);
        if(map == null) {
            return "null";
        }
        final Object o = map.get(key);
        if(o == null) {
            return "null";
        }
        return o+"";
    }
    //
    public double getDuration() {
        if(data == null || data.length == 0) {
            return -1;
        }
        final Map<String, Object> map = getMap(0);
        if(map == null) {
            return -1;
        }
        final Object o = map.get("duration");
        if(o == null) {
            return -1;
        }
        return ((Double) o).longValue();
    }

    @SuppressWarnings("unchecked")
	public void setDuration(final double duration) {
        if(data == null || data.length == 0) {
            data = new Object[] {map(pair("duration", duration))};
        }
        final Object meta = data[0];
        final Map<String, Object> map = (Map<String, Object>) meta;
        if(map == null) {
            data[0] = map(pair("duration", duration));
            return;
        }
        map.put("duration", duration);
    }

    //==========================================================================

    public static Metadata onPlayStatus(double duration, double bytes) {
        Map<String, Object> map = Command.onStatus(Command.OnStatus.STATUS,
                "NetStream.Play.Complete",
                pair("duration", duration),
                pair("bytes", bytes));
        return new MetadataAmf0("onPlayStatus", map);
    }

    public static Metadata rtmpSampleAccess() {
        return new MetadataAmf0("|RtmpSampleAccess", false, false);
    }

    public static Metadata dataStart() {
        return new MetadataAmf0("onStatus", object(pair("code", "NetStream.Data.Start")));
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("name: ").append(name);
        sb.append(" data: ").append(Arrays.toString(data));
        return sb.toString();
    }

}
