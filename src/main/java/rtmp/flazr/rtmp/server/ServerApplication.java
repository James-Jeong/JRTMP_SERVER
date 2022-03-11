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

import config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import rtmp.flazr.io.f4v.F4vReader;
import rtmp.flazr.io.flv.FlvReader;
import rtmp.flazr.io.flv.FlvWriter;
import rtmp.flazr.rtmp.RtmpConfig;
import rtmp.flazr.rtmp.RtmpReader;
import rtmp.flazr.rtmp.RtmpWriter;
import rtmp.flazr.util.Utils;
import service.AppInstance;
import util.FileManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    private final String name;
    private final Map<String, ServerStream> streams;

    public ServerApplication(final String rawName) {
        this.name = cleanName(rawName);        
        streams = new ConcurrentHashMap<>();
    }

    public String getName() {
        return name;
    }

    public RtmpReader getReader(final String rawName) {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();

        final String streamName = Utils.trimSlashes(rawName);
        final String basePath = FileManager.concatFilePath(RtmpConfig.SERVER_HOME_DIR, configManager.getRtmpMediaBaseName());
        final String path = FileManager.concatFilePath(basePath, name);
        final String readerPlayName;
        try {
            if(streamName.startsWith("mp4:")) {
                readerPlayName = streamName.substring(4);
                return new F4vReader(FileManager.concatFilePath(path, readerPlayName));
            } else {                
                if(streamName.lastIndexOf('.') < streamName.length() - 4) {
                    readerPlayName = streamName + ".flv";
                } else {
                    readerPlayName = streamName;
                }
                return new FlvReader(FileManager.concatFilePath(path, readerPlayName));
            }
        } catch(Exception e) {
            logger.info("reader creation failed: {}", e.getMessage());
            return null;
        }
    }

    public RtmpWriter getWriter(final String rawName) {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();

        final String streamName = Utils.trimSlashes(rawName);
        final String basePath = FileManager.concatFilePath(RtmpConfig.SERVER_HOME_DIR, configManager.getRtmpMediaBaseName());
        final String path = FileManager.concatFilePath(basePath, name);
        return new FlvWriter(FileManager.concatFilePath(path, streamName) + ".flv");
    }

    public static ServerApplication get(final String rawName) {
        final String appName = cleanName(rawName);
        //ServerApplication app = RtmpServer.APPLICATIONS.get(appName);
        ServerApplication app = RtmpManager.getInstance().getAPPLICATIONS().get(appName);
        if (app == null) {
            app = new ServerApplication(appName);
            //RtmpServer.APPLICATIONS.put(appName, app);
            RtmpManager.getInstance().getAPPLICATIONS().put(appName, app);
        }
        return app;
    }

    public ServerStream getStream(final String rawName) {        
        return getStream(rawName, null);
    }

    public ServerStream getStream(final String rawName, final String type) {
        final String streamName = cleanName(rawName);
        ServerStream stream = streams.get(streamName);
        if(stream == null) {
            stream = new ServerStream(streamName, type);
            streams.put(streamName, stream);
        }
        return stream;
    }

    private static String cleanName(final String raw) {
        return Utils.trimSlashes(raw).toLowerCase();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[name: '").append(name);
        sb.append("' streams: ").append(streams);
        sb.append(']');
        return sb.toString();
    }

}
