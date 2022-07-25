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

import jdk.internal.loader.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.PublishType;
import rtmp.flazr.rtmp.StreamType;
import rtmp.flazr.util.Utils;
import service.resource.ResourceManager;
import service.resource.ResourceReleaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    private final String appName;

    private final Map<String, ServerStream> streams;

    public ServerApplication(final String rawAppName) {
        this.appName = cleanName(rawAppName);
        streams = new ConcurrentHashMap<>();
    }

    public String getAppName() {
        return appName;
    }

    public ServerStream addStream(int streamId, final String rawStreamName, final String publishType) {
        final String streamName = cleanName(rawStreamName);

        if (hasStream(streamName)) {
            logger.error("[ServerApp({})] ServerStream [{}:{}] ALREADY EXIST", appName, publishType, streamName);
            return null;
        }

        ServerStream stream = new ServerStream(streamId, streamName, publishType);
        streams.put(streamName, stream);
        logger.warn("[ServerApp({})] ServerStream [{}:{}] (+)CREATED", appName, publishType, streamName);
        return stream;
    }

    public ServerStream getStream(final String rawStreamName) {
        final String streamName = cleanName(rawStreamName);

        if (!hasStream(streamName)) {
            logger.warn("[ServerApp({})] ServerStream [{}] NOT EXIST", appName, streamName);
            return null;
        }

        return streams.get(streamName);
    }

    public void deleteStream(final String rawName) {
        final String streamName = cleanName(rawName);

        ServerStream serverStream = streams.remove(streamName);
        if (serverStream != null) {
            logger.warn("[ServerApplication] ServerStream [{}] (-)DELETED", streamName);
            logger.debug("[ServerApplication] [(-)DELETED] \n{}", serverStream);
        }
    }

    public boolean hasStream(final String rawName) {
        final String streamName = cleanName(rawName);
        return streams.containsKey(streamName);
    }

    public int getStreamSize() {
        return streams.size();
    }

    public List<String> getStreamNames() {
        synchronized (streams) {
            return new ArrayList<>(streams.keySet());
        }
    }

    public void deleteAllServerStreams() {
        synchronized (streams) {
            for (Map.Entry<String, ServerStream> streamEntry : streams.entrySet()) {
                ServerStream serverStream = streamEntry.getValue();
                if (serverStream != null && serverStream.isLive() && !serverStream.isPlayStream()) {
                    ResourceReleaseManager.getInstance().unPublishIfLive(
                            ResourceManager.getInstance().getServerApp(appName),
                            serverStream
                    );
                }
            }

            streams.entrySet().removeIf(Objects::nonNull);
        }
    }
    private static String cleanName(final String raw) {
        return Utils.trimSlashes(raw).toLowerCase();
    }

    @Override
    public String toString() {
        return "[name: '" + appName +
                "' streams: " + streams +
                ']';
    }

}
