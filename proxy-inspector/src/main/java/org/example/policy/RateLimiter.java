package org.example.policy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RateLimiter {
    private static final class Window {
        volatile long windowStartMs;
        final AtomicInteger count = new AtomicInteger(0); // number of requests for time window
        volatile long lastSeenMs;

        Window(long nowMs) {
            this.windowStartMs = nowMs;
            this.lastSeenMs = nowMs;
        }
    }

    private final ConcurrentHashMap<String, Window> byClient = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;
    private final long idleEvictMs; // cleanup so the map does not grow forever

    public RateLimiter(int maxRequests, long windowMs, long idleEvictMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.idleEvictMs = idleEvictMs;
    }

    public boolean allow(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return true; // if cant identify client, default to allow for now
        }

        long now = System.currentTimeMillis(); // current time

        // create new window for client if it does not have on already
        Window w = byClient.computeIfAbsent(clientIp, ip -> new Window(now));
        w.lastSeenMs = now; // update last seen to current time

        // rotate window and reset count if expired
        if (now - w.windowStartMs >= windowMs) {
           synchronized (w) {
               if (now - w.windowStartMs >= windowMs) {
                   w.windowStartMs = now;
                   w.count.set(0);
               }
           }
        }

        int n = w.count.incrementAndGet(); // increase request count
        return n <= maxRequests; // return true if count is less than max requests for given window
    }

    // cleanup so the client map does not grow forever
    public void evictIdle() {
        long now = System.currentTimeMillis();
        byClient.entrySet().removeIf(e -> now - e.getValue().lastSeenMs > idleEvictMs);
    }
}
