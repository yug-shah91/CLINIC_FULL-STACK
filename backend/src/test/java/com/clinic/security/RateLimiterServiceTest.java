package com.clinic.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // 3 login requests, 2 register requests, 60s window, max 5 tracked entries
        rateLimiterService = new RateLimiterService(3, 2, 60, 5);
    }

    @Test
    void allowsRequestsUpToLimitAndBlocksSubsequent() {
        String ip = "192.168.1.100";

        assertTrue(rateLimiterService.allowLogin(ip));
        assertTrue(rateLimiterService.allowLogin(ip));
        assertTrue(rateLimiterService.allowLogin(ip));
        // 4th request should be blocked
        assertFalse(rateLimiterService.allowLogin(ip));

        // Different IP should still be allowed
        assertTrue(rateLimiterService.allowLogin("192.168.1.101"));
    }

    @Test
    void allowsRegistrationUpToLimit() {
        String ip = "10.0.0.1";

        assertTrue(rateLimiterService.allowRegister(ip));
        assertTrue(rateLimiterService.allowRegister(ip));
        // 3rd attempt should be blocked (limit is 2)
        assertFalse(rateLimiterService.allowRegister(ip));
    }

    @Test
    void concurrentRequestsCannotExceedPerIpLimit() throws Exception {
        int limit = 10;
        int workers = 40;
        RateLimiterService limiter = new RateLimiterService(limit, 2, 60, 100);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < workers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return limiter.allowLogin("192.0.2.44");
                }));
            }
            ready.await();
            start.countDown();
            long allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) allowed++;
            }
            assertEquals(limit, allowed, "Concurrent requests must not bypass the configured limit");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void boundsStorageToMaxEntries() {
        // Register requests from 5 distinct IPs (up to maxEntries)
        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiterService.allowLogin("10.0.0." + i));
        }

        // 6th distinct IP should be denied because capacity is strictly bounded
        assertFalse(rateLimiterService.allowLogin("10.0.0.6"));
    }
}
