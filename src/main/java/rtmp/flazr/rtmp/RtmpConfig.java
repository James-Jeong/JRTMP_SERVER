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

package rtmp.flazr.rtmp;

import rtmp.flazr.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class RtmpConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RtmpConfig.class);

    public enum Type { SERVER, SERVER_STOP, PROXY, PROXY_STOP }

    public static String SERVER_HOME_DIR = "home";
    public static int TIMER_TICK_SIZE = 100;
    public static int SERVER_PORT = 1935;
    public static int SERVER_STOP_PORT = 1934;

    public static void configureServer(String flazrConfPath) {
        configure(flazrConfPath, Type.SERVER);
        //addShutdownHook(SERVER_STOP_PORT);
    }

    public static int configureServerStop(String flazrConfPath) {
        configure(flazrConfPath, Type.SERVER_STOP);
        return SERVER_STOP_PORT;
    }

    private static void configure(String flazrConfPath, Type type) {
        File propsFile = new File(flazrConfPath);
        if(!propsFile.exists()) {
            logger.warn("{} not found, will use configuration defaults", propsFile.getAbsolutePath());
        } else {
            logger.info("loading config from: {}", propsFile.getAbsolutePath());
            Properties props = loadProps(propsFile);
            switch(type) {
                case SERVER:
                case SERVER_STOP:
                    Integer serverStop = parseInt(props.getProperty("server.stop.port"));
                    if(serverStop != null) SERVER_STOP_PORT = serverStop;
                    if(type == Type.SERVER_STOP) {
                        break;
                    }
                    Integer serverPort = parseInt(props.getProperty("server.port"));
                    if(serverPort != null) SERVER_PORT = serverPort;
                    SERVER_HOME_DIR = props.getProperty("server.home", "home");
                    File homeFile = new File(SERVER_HOME_DIR);
                    if(!homeFile.exists()) {
                        logger.error("home dir does not exist, aborting: {}", homeFile.getAbsolutePath());
                        throw new RuntimeException("home dir does not exist: " + homeFile.getAbsolutePath());
                    }
                    logger.info("home dir: '{}'", homeFile.getAbsolutePath());
                    logger.info("server port: {} (stop {})", SERVER_PORT, SERVER_STOP_PORT);
                    break;
            }
        }        
    }

    private static class ServerShutdownHook extends Thread {

        private final int port;

        public ServerShutdownHook(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            Utils.sendStopSignal(port);
        }

    }

    private static void addShutdownHook(final int port) {
        Runtime.getRuntime().addShutdownHook(new ServerShutdownHook(port));
    }

    private static Properties loadProps(final File file) {
        try {
            final InputStream is = new FileInputStream(file);
            final Properties props = loadProps(is);
            is.close();
            return props;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties loadProps(final InputStream is) {
        final Properties props = new Properties();
        try {
            props.load(is);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return props;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch(Exception e) {
            logger.warn("unable to parse into integer value: {}", e.getMessage());
            return null;
        }
    }

}
