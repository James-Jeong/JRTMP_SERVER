package config;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @class public class UserConfig
 * @brief UserConfig Class
 */
public class ConfigManager {

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private Ini ini = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_RTMP = "RTMP"; // RTMP Section 이름
    public static final String SECTION_REGISTER = "REGISTER"; // REGISTER Section 이름
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // Field String
    // COMMON
    public static final String FIELD_SERVICE_NAME = "SERVICE_NAME";
    public static final String FIELD_LONG_SESSION_LIMIT_TIME = "LONG_SESSION_LIMIT_TIME";

    // RTMP
    public static final String FIELD_FLAZR_CONF_PATH = "FLAZR_CONF_PATH";
    public static final String FIELD_RTMP_LISTEN_IP = "RTMP_LISTEN_IP";
    public static final String FIELD_RTMP_LISTEN_PORT = "RTMP_LISTEN_PORT";
    public static final String FIELD_RTMP_MEDIA_BASE_NAME = "RTMP_MEDIA_BASE_NAME";

    // REGISTER
    public static final String FIELD_REGISTER_WHITELIST_PATH = "REGISTER_WHITELIST_PATH";
    public static final String FIELD_REGISTER_BLACKLIST_PATH = "REGISTER_BLACKLIST_PATH";
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // VARIABLES
    // COMMON
    private String serviceName = null;
    private long localSessionLimitTime = 0; // ms

    // RTMP
    private String flazrConfPath = null;
    private String rtmpListenIp = null;
    private int rtmpListenPort = 0;
    private String rtmpMediaBaseName = null;

    // REGISTER
    private String registerWhitelistPath = null;
    private String registerBlacklistPath = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public AuditConfig(String configPath)
     * @brief AuditConfig 생성자 함수
     * @param configPath Config 파일 경로 이름
     */
    public ConfigManager(String configPath) {
        File iniFile = new File(configPath);
        if (!iniFile.isFile() || !iniFile.exists()) {
            logger.warn("Not found the config path. (path={})", configPath);
            System.exit(1);
        }

        try {
            this.ini = new Ini(iniFile);

            loadCommonConfig();
            loadRtmpConfig();
            loadRegisterConfig();

            logger.info("Load config [{}]", configPath);
        } catch (IOException e) {
            logger.error("ConfigManager.IOException", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private void loadCommonConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadCommonConfig() {
        this.serviceName = getIniValue(SECTION_COMMON, FIELD_SERVICE_NAME);
        if (serviceName == null) {
            logger.error("Fail to load [{}-{}].", SECTION_COMMON, FIELD_SERVICE_NAME);
            System.exit(1);
        }

        this.localSessionLimitTime = Long.parseLong(getIniValue(SECTION_COMMON, FIELD_LONG_SESSION_LIMIT_TIME));
        if (this.localSessionLimitTime < 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_COMMON, FIELD_LONG_SESSION_LIMIT_TIME, localSessionLimitTime);
            System.exit(1);
        }

        logger.debug("Load [{}] config...(OK)", SECTION_COMMON);
    }

    /**
     * @fn private void loadRtmpConfig()
     * @brief RTMP Section 을 로드하는 함수
     */
    private void loadRtmpConfig() {
        this.flazrConfPath = getIniValue(SECTION_RTMP, FIELD_FLAZR_CONF_PATH);
        if (this.flazrConfPath == null) {
            logger.error("Fail to load [{}-{}].", SECTION_RTMP, FIELD_FLAZR_CONF_PATH);
            System.exit(1);
        }

        this.rtmpListenIp = getIniValue(SECTION_RTMP, FIELD_RTMP_LISTEN_IP);
        if (this.rtmpListenIp == null) {
            logger.error("Fail to load [{}-{}].", SECTION_RTMP, FIELD_RTMP_LISTEN_IP);
            System.exit(1);
        }

        String rtmpPublishPortString = getIniValue(SECTION_RTMP, FIELD_RTMP_LISTEN_PORT);
        if (rtmpPublishPortString == null) {
            logger.error("Fail to load [{}-{}].", SECTION_RTMP, FIELD_RTMP_LISTEN_PORT);
            System.exit(1);
        } else {
            this.rtmpListenPort = Integer.parseInt(rtmpPublishPortString);
            if (this.rtmpListenPort <= 0 || this.rtmpListenPort > 65535) {
                logger.error("Fail to load [{}-{}].", SECTION_RTMP, FIELD_RTMP_LISTEN_PORT);
                System.exit(1);
            }
        }

        this.rtmpMediaBaseName = getIniValue(SECTION_RTMP, FIELD_RTMP_MEDIA_BASE_NAME);
        if (this.rtmpMediaBaseName == null) {
            logger.error("Fail to load [{}-{}].", SECTION_RTMP, FIELD_RTMP_MEDIA_BASE_NAME);
            System.exit(1);
        }

        logger.debug("Load [{}] config...(OK)", SECTION_RTMP);
    }

    /**
     * @fn private void loadRegisterConfig()
     * @brief REGISTER Section 을 로드하는 함수
     */
    private void loadRegisterConfig() {
        this.registerWhitelistPath = getIniValue(SECTION_REGISTER, FIELD_REGISTER_WHITELIST_PATH);
        if (this.registerWhitelistPath == null) {
            logger.error("Fail to load [{}-{}].", SECTION_REGISTER, FIELD_REGISTER_WHITELIST_PATH);
            System.exit(1);
        }

        this.registerBlacklistPath = getIniValue(SECTION_REGISTER, FIELD_REGISTER_BLACKLIST_PATH);
        if (this.registerBlacklistPath == null) {
            logger.error("Fail to load [{}-{}].", SECTION_REGISTER, FIELD_REGISTER_BLACKLIST_PATH);
            System.exit(1);
        }

        logger.debug("Load [{}] config...(OK)", SECTION_REGISTER);
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private String getIniValue(String section, String key)
     * @brief INI 파일에서 지정한 section 과 key 에 해당하는 value 를 가져오는 함수
     * @param section Section
     * @param key Key
     * @return 성공 시 value, 실패 시 null 반환
     */
    private String getIniValue(String section, String key) {
        String value = ini.get(section,key);
        if (value == null) {
            logger.warn("[ {} ] \" {} \" is null.", section, key);
            System.exit(1);
            return null;
        }

        value = value.trim();
        logger.debug("\tGet Config [{}] > [{}] : [{}]", section, key, value);
        return value;
    }

    /**
     * @fn public void setIniValue(String section, String key, String value)
     * @brief INI 파일에 새로운 value 를 저장하는 함수
     * @param section Section
     * @param key Key
     * @param value Value
     */
    public void setIniValue(String section, String key, String value) {
        try {
            ini.put(section, key, value);
            ini.store();

            logger.debug("\tSet Config [{}] > [{}] : [{}]", section, key, value);
        } catch (IOException e) {
            logger.warn("Fail to set the config. (section={}, field={}, value={})", section, key, value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getServiceName() {
        return serviceName;
    }

    public long getLocalSessionLimitTime() {
        return localSessionLimitTime;
    }

    public String getFlazrConfPath() {
        return flazrConfPath;
    }

    public String getRtmpListenIp() {
        return rtmpListenIp;
    }

    public int getRtmpListenPort() {
        return rtmpListenPort;
    }

    public String getRtmpMediaBaseName() {
        return rtmpMediaBaseName;
    }

    public String getRegisterWhitelistPath() {
        return registerWhitelistPath;
    }

    public String getRegisterBlacklistPath() {
        return registerBlacklistPath;
    }
}
