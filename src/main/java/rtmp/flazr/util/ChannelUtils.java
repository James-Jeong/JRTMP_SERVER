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

package rtmp.flazr.util;

import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ChannelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    public static void exceptionCaught(final ExceptionEvent e) {
        if (e == null) { return; }

        logger.warn("exception: {}", e);
        if (e.toString().contains("Connection reset by peer")
                || e.toString().contains("ClosedChannelException")) {
            return;
        }

        if (e.getCause() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.getCause().printStackTrace(pw);
            //logger.warn("cause: {}", e.getCause().toString());
            logger.warn("message: {}", e.getCause().getMessage());
            //logger.warn("stacktrace: {}", sw);
        }
    }

}
