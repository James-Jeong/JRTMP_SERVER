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

import rtmp.flazr.rtmp.RtmpConfig;
import rtmp.flazr.util.StopMonitor;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.flazr.util.Utils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RtmpServer {

    private static final Logger logger = LoggerFactory.getLogger(RtmpServer.class);

    static {
        RtmpConfig.configureServer("/Users/jamesj/GIT_PROJECTS/JRTMP_SERVER/src/main/resources/config/flazr.properties");
        CHANNELS = new DefaultChannelGroup("server-channels");
        APPLICATIONS = new ConcurrentHashMap<>();
        TIMER = new HashedWheelTimer(RtmpConfig.TIMER_TICK_SIZE, TimeUnit.MILLISECONDS);
    }
    
    protected static final ChannelGroup CHANNELS;
    protected static final Map<String, ServerApplication> APPLICATIONS;
    public static final Timer TIMER;

    static ServerApplication getApplication(final String rawName) {
        final String appName = Utils.trimSlashes(rawName).toLowerCase();
        ServerApplication app = RtmpServer.APPLICATIONS.get(appName);
        if (app == null) {
            app = new ServerApplication(appName);
            RtmpServer.APPLICATIONS.put(appName, app);
        }
        return app;
    }

    public static void main(String[] args) throws Exception {

        final ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        final ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        final InetSocketAddress socketAddress = new InetSocketAddress(RtmpConfig.SERVER_PORT);
        bootstrap.bind(socketAddress);
        logger.info("server started, listening on: {}", socketAddress);

        final Thread monitor = new StopMonitor(RtmpConfig.SERVER_STOP_PORT);
        monitor.start();        
        monitor.join();

        TIMER.stop();
        final ChannelGroupFuture future = CHANNELS.close();
        logger.info("closing channels");
        future.awaitUninterruptibly();
        logger.info("releasing resources");
        factory.releaseExternalResources();
        logger.info("server stopped");

    }

}
