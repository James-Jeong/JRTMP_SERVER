package org.red5.server.net.rtmp.event;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import com.flazr.rtmp.RtmpHeader;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;

public interface IRTMPEvent extends IEvent {

	/**
     * Getter for data type
     *
     * @return  Data type
     */
    public byte getDataType();

	/**
     * Setter for source
     *
     * @param source Source
     */
    public void setSource(IEventListener source);

	/**
     * Getter for header
     *
     * @return  RTMP packet header
     */
    public RtmpHeader getHeader();

	/**
     * Setter for header
     *
     * @param header RTMP packet header
     */
    public void setHeader(RtmpHeader header);

	/**
     * Getter for timestamp
     *
     * @return  Event timestamp
     */
    public int getTimestamp();

	/**
     * Setter for timestamp
     *
     * @param timestamp  New event timestamp
     */
    public void setTimestamp(int timestamp);
    
	/**
     * Getter for source type
     *
     * @return  Source type
     */
    public byte getSourceType();    

	/**
     * Setter for source type
     *
     * @param sourceType 
     */
    public void setSourceType(byte sourceType);
        
    /**
     * Retain event
     */
    public void retain();

	/**
	 * Hook to free buffers allocated by the event.
	 */
	public void release();

}