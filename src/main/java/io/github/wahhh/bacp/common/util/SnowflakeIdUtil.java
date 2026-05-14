package io.github.wahhh.bacp.common.util;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight snowflake-style ID generator (thread-safe).
 */
public final class SnowflakeIdUtil {

    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_BITS = 5L;

    private static final long DATACENTER_BITS = 5L;

    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);

    private static final long MAX_DATACENTER = ~(-1L << DATACENTER_BITS);

    private static final long WORKER_SHIFT = SEQUENCE_BITS;

    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;

    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID;

    private static final long DATACENTER_ID;

    private static final AtomicLong SEQUENCE = new AtomicLong(0L);

    private static volatile long lastTs = -1L;

    static {
        long hash = hostHash();
        WORKER_ID = hash & MAX_WORKER;
        DATACENTER_ID = (hash >> 5) & MAX_DATACENTER;
    }

    private SnowflakeIdUtil() {
    }

    /**
     * Generates the next 64-bit unique id.
     *
     * @return snowflake id
     */
    public static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            throw new IllegalStateException("clock moved backwards");
        }
        long seq = SEQUENCE.get();
        if (ts == lastTs) {
            seq = (seq + 1) & SEQUENCE_MASK;
            if (seq == 0) {
                ts = waitNextMillis(lastTs);
            }
        } else {
            seq = 0L;
        }
        lastTs = ts;
        SEQUENCE.set(seq);
        return ((ts - EPOCH) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << DATACENTER_SHIFT)
                | (WORKER_ID << WORKER_SHIFT)
                | seq;
    }

    private static long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    private static long hostHash() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(host.getBytes(StandardCharsets.UTF_8));
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (digest[i] & 0xff);
            }
            return Math.abs(v);
        } catch (Exception ex) {
            return 1L;
        }
    }
}
