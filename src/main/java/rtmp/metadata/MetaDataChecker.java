package rtmp.metadata;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MetaDataChecker {

    public static VideoCodecId checkVideoCodecId(String videoCodecId) {
        if (videoCodecId == null || videoCodecId.isEmpty()) { return null; }

        try {
            int parsedVideoCodecId = (int) Float.parseFloat(videoCodecId);
            AtomicReference<VideoCodecId> requestedVideoCodecId = new AtomicReference<>(null);
            if (Arrays.stream(VideoCodecId.values()).anyMatch(codecId -> {
                if (codecId == null) { return false; }
                if (parsedVideoCodecId == codecId.getId()) {
                    requestedVideoCodecId.set(codecId);
                    return true;
                } else { return false; }
            })) {
                return requestedVideoCodecId.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("MetaDataChecker.checkVideoCodecId.Exception", e);
            return null;
        }
    }

    public static AudioCodecId checkAudioCodecId(String audioCodecId) {
        if (audioCodecId == null || audioCodecId.isEmpty()) { return null; }

        try {
            int parsedAudioCodecId = (int) Float.parseFloat(audioCodecId);
            AtomicReference<AudioCodecId> requestedAudioCodecId = new AtomicReference<>(null);
            if (Arrays.stream(AudioCodecId.values()).anyMatch(codecId -> {
                if (codecId == null) { return false; }
                if (parsedAudioCodecId == codecId.getId()) {
                    requestedAudioCodecId.set(codecId);
                    return true;
                } else { return false; }
            })) {
                return requestedAudioCodecId.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("MetaDataChecker.checkAudioCodecId.Exception", e);
            return null;
        }
    }

}
