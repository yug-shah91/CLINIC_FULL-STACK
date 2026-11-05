package com.clinic.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * In-memory sliding-window limiter for login and registration endpoints.
 * All map/queue changes are guarded by this instance's monitor so capacity
 * enforcement, request recording and cleanup cannot race each other.
 * For multi-instance deployments, use a shared store (for example Redis).
 */
@Slf4j
@Service
public class RateLimiterService {

    private final int loginMaxRequests;
    private final int registerMaxRequests;
    private final long windowSeconds;
    private final int maxEntries;

    private final Map<String, ArrayDeque<Instant>> loginAttempts = new HashMap<>();
    private final Map<String, ArrayDeque<Instant>> registerAttempts = new HashMap<>();

    @Autowired
    public RateLimiterService(
            @Value("${clinic.ratelimit.login.max-requests:10}") int loginMaxRequests,
            @Value("${clinic.ratelimit.register.max-requests:5}") int registerMaxRequests,
            @Value("${clinic.ratelimit.window-seconds:60}") long windowSeconds,
            @Value("${clinic.ratelimit.max-entries:10000}") int maxEntries) {
        if (loginMaxRequests < 1 || registerMaxRequests < 1 || windowSeconds < 1 || maxEntries < 1) {
            throw new IllegalArgumentException("Rate-limit counts, window and max-entries must all be positive");
        }
        this.loginMaxRequests = loginMaxRequests;
        this.registerMaxRequests = registerMaxRequests;
        this.windowSeconds = windowSeconds;
        this.maxEntries = maxEntries;
    }

    public RateLimiterService(int loginMaxRequests, int registerMaxRequests, long windowSeconds) {
        this(loginMaxRequests, registerMaxRequests, windowSeconds, 10000);
    }

    public synchronized boolean allowLogin(String clientIp) {
        return allowRequest(loginAttempts, clientIp, loginMaxRequests, Instant.now());
    }

    public synchronized boolean allowRegister(String clientIp) {
        return allowRequest(registerAttempts, clientIp, registerMaxRequests, Instant.now());
    }

    private boolean allowRequest(Map<String, ArrayDeque<Instant>> store,
                                 String clientIp, int maxAllowed, Instant now) {
        String key = clientIp == null || clientIp.isBlank() ? "UNKNOWN" : clientIp.trim();
        Instant windowStart = now.minusSeconds(windowSeconds);
        ArrayDeque<Instant> timestamps = store.get(key);

        if (timestamps == null) {
            // Expired entries are evicted under the same lock before checking capacity.
            cleanupStore(store, windowStart);
            if (store.size() >= maxEntries) {
                log.warn("Rate limiter capacity reached ({} entries); denying a request", maxEntries);
                return false;
            }
            timestamps = new ArrayDeque<>();
            store.put(key, timestamps);
        }

        evictExpired(timestamps, windowStart);
        if (timestamps.size() >= maxAllowed) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }

    @Scheduled(fixedRate = 60000)
    public synchronized void cleanup() {
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);
        cleanupStore(loginAttempts, windowStart);
        cleanupStore(registerAttempts, windowStart);
    }

    private void cleanupStore(Map<String, ArrayDeque<Instant>> store, Instant windowStart) {
        Iterator<Map.Entry<String, ArrayDeque<Instant>>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Instant> timestamps = iterator.next().getValue();
            evictExpired(timestamps, windowStart);
            if (timestamps.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void evictExpired(ArrayDeque<Instant> timestamps, Instant windowStart) {
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(windowStart)) {
            timestamps.removeFirst();
        }
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public synchronized int getTrackedLoginIpCount() {
        return loginAttempts.size();
    }

    public synchronized int getTrackedRegisterIpCount() {
        return registerAttempts.size();
    }
}
