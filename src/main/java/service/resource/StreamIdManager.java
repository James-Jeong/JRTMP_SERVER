/*
 * Copyright (C) 2021. Uangel Corp. All rights reserved.
 */

package service.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StreamIdManager {

    private static final Logger logger = LoggerFactory.getLogger(StreamIdManager.class);

    private static StreamIdManager streamIdManager = null;
    private final ConcurrentLinkedQueue<Integer> queue;

    private static final int STREAM_ID_MIN = 1;
    private static final int STREAM_ID_MAX = 1000;
    private static final int STREAM_ID_GAP = 1;

    ////////////////////////////////////////////////////////////////////////////////

    public StreamIdManager( ) {
        queue = new ConcurrentLinkedQueue<>();
    }

    public static StreamIdManager getInstance ( ) {
        if (streamIdManager == null) {
            streamIdManager = new StreamIdManager();
        }

        return streamIdManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void initResource() {
        for (int idx = STREAM_ID_MIN; idx <= STREAM_ID_MAX; idx += STREAM_ID_GAP) {
            try {
                queue.add(idx);
            } catch (Exception e) {
                logger.error("[StreamIdManager.initResource] Error Occurs ", e);
                return;
            }
        }

        logger.info("[StreamIdManager] Ready StreamID Queue (size: {}, range: {} - {}, gap: {})",
                getStreamIdSize(), STREAM_ID_MIN, STREAM_ID_MAX, STREAM_ID_GAP
        );
    }

    public void releaseResource () {
        queue.clear();
        logger.info("[StreamIdManager] Release Stream ID (size: {}, range: {} - {}, gap: {})",
                getStreamIdSize(), STREAM_ID_MIN, STREAM_ID_MAX, STREAM_ID_GAP
        );
    }

    public int takeStreamId() {
        if (queue.isEmpty()) {
            logger.warn("[StreamIdManager] StreamID Queue is Empty.");
            return -1;
        }

        int streamId = -1;
        try {
            Integer value = queue.poll();
            if (value != null) {
                streamId = value;
            }
        } catch (Exception e) {
            logger.warn("[StreamIdManager.takeStreamId] Error Occurs ", e);
        }

        logger.debug("[StreamIdManager] Take Stream ID(={}) Success", streamId);
        return streamId;
    }

    public boolean restoreStreamId(int streamId) {
        if (!queue.contains(streamId)) {
            try {
                queue.offer(streamId);
                logger.debug("[StreamIdManager] Restore Stream ID(={}) Success", streamId);
                return true;
            } catch (Exception e) {
                logger.warn("[StreamIdManager.restoreStreamId] Error Occurs (Restore StreamId:{}) ", streamId, e);
            }
        } else {
            logger.warn("[StreamIdManager] StreamId [{}] ALREADY EXIST", streamId);
        }
        return false;
    }

    public void removeStreamId(int streamId) {
        try {
            queue.remove(streamId);
        } catch (Exception e) {
            logger.warn("[StreamIdManager.removeStreamId] Error Occurs (Remove StreamId:{}) ", streamId, e);
        }
    }

    public int getStreamIdSize() {
        return queue.size();
    }

}
