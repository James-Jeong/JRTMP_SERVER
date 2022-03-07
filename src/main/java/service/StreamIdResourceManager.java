/*
 * Copyright (C) 2021. Uangel Corp. All rights reserved.
 */

package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StreamIdResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(StreamIdResourceManager.class);

    private static StreamIdResourceManager resourceManager = null;
    private final ConcurrentLinkedQueue<Integer> queue;

    private final int streamIdMin = 1;
    private final int streamIdMax = 1000;
    private final int streamIdGap = 1;

    ////////////////////////////////////////////////////////////////////////////////

    public StreamIdResourceManager( ) {
        queue = new ConcurrentLinkedQueue<>();
    }

    public static StreamIdResourceManager getInstance ( ) {
        if (resourceManager == null) {
            resourceManager = new StreamIdResourceManager();
        }

        return resourceManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void initResource() {
        for (int idx = streamIdMin; idx <= streamIdMax; idx += streamIdGap) {
            try {
                queue.add(idx);
            } catch (Exception e) {
                logger.error("[StreamIdResourceManager] Exception to Stream ID resource in Queue", e);
                return;
            }
        }

        logger.info("[StreamIdResourceManager] Ready to Stream ID resource in Queue. (streamId range: {} - {}, gap={})",
                streamIdMin, streamIdMax, streamIdGap
        );
    }

    public void releaseResource () {
        queue.clear();
        logger.info("[StreamIdResourceManager] Release Stream ID resource in Queue. (streamId range: {} - {}, gap={})",
                streamIdMin, streamIdMax, streamIdGap
        );
    }

    public int takeStreamId() {
        if (queue.isEmpty()) {
            logger.warn("[StreamIdResourceManager] Stream ID resource in Queue is empty.");
            return -1;
        }

        int streamId = -1;
        try {
            Integer value = queue.poll();
            if (value != null) {
                streamId = value;
            }
        } catch (Exception e) {
            logger.warn("[StreamIdResourceManager] Exception to get Stream ID resource in Queue.", e);
        }

        logger.debug("[StreamIdResourceManager] Success to get Stream ID(={}) resource in Queue.", streamId);
        return streamId;
    }

    public void restoreStreamId(int streamId) {
        if (!queue.contains(streamId)) {
            try {
                queue.offer(streamId);
            } catch (Exception e) {
                logger.warn("[StreamIdResourceManager] Exception to restore Stream ID(={}) resource in Queue.", streamId, e);
            }
        }
    }

    public void removeStreamId(int streamId) {
        try {
            queue.remove(streamId);
        } catch (Exception e) {
            logger.warn("[StreamIdResourceManager] Exception to remove to Stream ID(={}) resource in Queue.", streamId, e);
        }
    }

}
