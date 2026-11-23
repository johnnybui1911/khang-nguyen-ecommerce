package com.ecommerce.ext.web.guard;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window counter keyed by an arbitrary string. {@link #tryAcquire} returns 0 when the call
 * is allowed, otherwise the number of seconds until the window resets.
 */
public class KnRateLimiter {

    private static final long CLEANUP_EVERY = 256;

    private final int maxAttempts;
    private final long windowMillis;
    private final Map<String, long[]> windows = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public KnRateLimiter(int maxAttempts, long windowMillis) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    public long tryAcquire(String key, long nowMillis) {
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            cleanup(nowMillis);
        }
        long[] retryAfter = new long[1];
        windows.compute(key, (k, w) -> {
            if (w == null || nowMillis - w[0] >= windowMillis) {
                return new long[] {nowMillis, 1};
            }
            if (w[1] >= maxAttempts) {
                retryAfter[0] = Math.max(1, (w[0] + windowMillis - nowMillis + 999) / 1000);
                return w;
            }
            w[1]++;
            return w;
        });
        return retryAfter[0];
    }

    private void cleanup(long nowMillis) {
        Iterator<Map.Entry<String, long[]>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            if (nowMillis - it.next().getValue()[0] >= windowMillis) {
                it.remove();
            }
        }
    }
}
