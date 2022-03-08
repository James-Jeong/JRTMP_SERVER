package rtmp.server.jmx.mxbeans;

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

import javax.management.MXBean;
import java.util.Map;
import java.util.Set;

/**
 * Base interface for all API objects with attributes
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@MXBean
public interface AttributeStoreMXBean {

	public Set<String> getAttributeNames();

	public Map<String, Object> getAttributes();

	public boolean setAttribute(String name, Object value);

	public Object getAttribute(String name);

	public Object getAttribute(String name, Object defaultValue);

	public boolean hasAttribute(String name);

	public boolean removeAttribute(String name);

	public void removeAttributes();

}