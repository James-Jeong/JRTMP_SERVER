package register;

import config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import register.channel.RtmpRegisterNettyChannel;
import register.fsm.RtmpRegState;
import rtmp.base.RtmpRegUnit;
import service.AppInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class RtmpRegisterManager {

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpRegisterManager.class);

    private final ConfigManager configManager;

    private RtmpRegisterNettyChannel rtmpRegisterNettyChannel = null;

    private final HashMap<String, RtmpRegUnit> rtmpRegUnitMap = new HashMap<>();
    private final ReentrantLock rtmpRegUnitMapLock = new ReentrantLock();

    private final List<String> whitelist = new ArrayList<>();
    private final List<String> blacklist = new ArrayList<>();
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpRegisterManager() {
        configManager = AppInstance.getInstance().getConfigManager();

        String whitelistPath = configManager.getRegisterWhitelistPath();
        String blacklistPath = configManager.getRegisterBlacklistPath();

        try {
            File whitelistFile = new File(whitelistPath);
            if (!whitelistFile.exists() || whitelistFile.isDirectory()) {
                logger.error("[RtmpRegisterManager] Fail to get the white list file. (whitelistPath={})", whitelistPath);
                System.exit(1);
            } else {
                // GET LIST ONE TIME
                whitelist.clear();
                whitelist.addAll(readAllLines(configManager.getRegisterWhitelistPath()));
                logger.debug("[RtmpRegisterManager] WHITE USER LIST: [{}]", whitelist);
            }

            File blacklistFile = new File(blacklistPath);
            if (!blacklistFile.exists() || blacklistFile.isDirectory()) {
                logger.error("[RtmpRegisterManager] Fail to get the black list file. (blacklistPath={})", blacklistPath);
                System.exit(1);
            } else {
                // GET LIST ONE TIME
                blacklist.clear();
                blacklist.addAll(readAllLines(configManager.getRegisterBlacklistPath()));
                logger.debug("[RtmpRegisterManager] BLACK USER LIST: [{}]", blacklist);
            }
        } catch (Exception e) {
            logger.error("[RtmpRegisterManager] Fail to get the user list file(s). (whitelistPath={}, blacklistPath={})", whitelistPath, blacklistPath);
            System.exit(1);
        }
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    private static List<String> readAllLines(String fileName) {
        if (fileName == null) { return null; }

        BufferedReader bufferedReader = null;
        List<String> lines = new ArrayList<>();
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while( (line = bufferedReader.readLine()) != null ) {
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            logger.warn("[FileManager] Fail to read the file. (fileName={})", fileName);
            return lines;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                logger.warn("[FileManager] Fail to close the buffer reader. (fileName={})", fileName, e);
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public boolean addRegisterChannel() {
        if (rtmpRegisterNettyChannel != null) {
            return false;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtmpRegisterNettyChannel = new RtmpRegisterNettyChannel(
                configManager.getRegisterListenIp(),
                configManager.getRegisterListenPort()
        );
        rtmpRegisterNettyChannel.run();
        rtmpRegisterNettyChannel.start();

        return true;
    }

    // 프로그램 종료 시 호출
    public void removeRegisterChannel() {
        if (rtmpRegisterNettyChannel == null) {
            return;
        }

        rtmpRegisterNettyChannel.stop();
        rtmpRegisterNettyChannel = null;
    }

    public RtmpRegisterNettyChannel getRegisterChannel() {
        return rtmpRegisterNettyChannel;
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpRegUnit addRtmpRegUnit(String regId) {
        if (getRtmpRegUnit(regId) != null) { return null; }

        try {
            rtmpRegUnitMapLock.lock();

            RtmpRegUnit rtmpRegUnit = new RtmpRegUnit(regId);

            rtmpRegUnit.getRtmpRegStateManager().addStateUnit(
                    rtmpRegUnit.getRtmpRegStateUnitId(),
                    rtmpRegUnit.getRtmpRegStateManager().getStateHandler(RtmpRegState.NAME).getName(),
                    RtmpRegState.IDLE,
                    null
            );

            rtmpRegUnitMap.putIfAbsent(regId, rtmpRegUnit);
            logger.debug("[RtmpManager] [(+)CREATED] \n{}", rtmpRegUnit);
            return rtmpRegUnit;
        } catch (Exception e) {
            logger.warn("Fail to open the rtmp register unit. (id={})", regId, e);
            return null;
        } finally {
            rtmpRegUnitMapLock.unlock();
        }
    }

    public void deleteRtmpRegUnit(String regId) {
        RtmpRegUnit rtmpRegUnit = getRtmpRegUnit(regId);
        if (rtmpRegUnit == null) { return; }

        try {
            rtmpRegUnitMapLock.lock();

            logger.debug("[RtmpManager] [(-)DELETED] \n{}", rtmpRegUnit);

            rtmpRegUnitMap.remove(regId);
        } catch (Exception e) {
            logger.warn("Fail to close the rtmp register unit. (id={})", regId, e);
        } finally {
            rtmpRegUnitMapLock.unlock();
        }
    }

    public HashMap<String, RtmpRegUnit> getCloneRtmpRegUnitMap( ) {
        HashMap<String, RtmpRegUnit> cloneMap;

        try {
            rtmpRegUnitMapLock.lock();

            cloneMap = (HashMap<String, RtmpRegUnit>) rtmpRegUnitMap.clone();
        } catch (Exception e) {
            logger.warn("Fail to clone the rtmp register unit map.", e);
            cloneMap = rtmpRegUnitMap;
        } finally {
            rtmpRegUnitMapLock.unlock();
        }

        return cloneMap;
    }

    public void deleteAllRtmpRegUnits() {
        try {
            rtmpRegUnitMapLock.lock();
            rtmpRegUnitMap.entrySet().removeIf(Objects::nonNull);
        } catch (Exception e) {
            logger.warn("Fail to close all rtmp register units.", e);
        } finally {
            rtmpRegUnitMapLock.unlock();
        }
    }

    public RtmpRegUnit getRtmpRegUnit(String regId) {
        return rtmpRegUnitMap.get(regId);
    }

    public int getRtmpRegUnitMapSize() {
        return rtmpRegUnitMap.size();
    }
    ////////////////////////////////////////////////////////////

}
