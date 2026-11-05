package com.clinic.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingFilterTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(10, 5, 60, 1000);
    }

    @Test
    void ignoresXForwardedForByDefaultToPreventSpoofing() {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiterService, false, "127.0.0.1");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.195");
        request.addHeader("X-Forwarded-For", "198.51.100.1"); // Spoofed IP header

        String extractedIp = filter.extractClientIp(request);
        assertEquals("203.0.113.195", extractedIp, "Must use remote address by default to prevent spoofing");
    }

    @Test
    void honorsXForwardedForOnlyWhenBehindTrustedProxy() {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiterService, true, "10.0.0.1, 127.0.0.1");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Incoming from trusted reverse proxy
        request.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.1");

        String extractedIp = filter.extractClientIp(request);
        assertEquals("198.51.100.1", extractedIp, "Should extract first client IP when request is from trusted proxy");
    }

    @Test
    void choosesRightmostUntrustedAddressToAvoidClientSuppliedPrefixSpoofing() {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiterService, true, "10.0.0.1, 10.0.0.2");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        // A client-supplied prefix may remain in the chain; the trusted proxy appends the real client.
        request.addHeader("X-Forwarded-For", "203.0.113.200, 198.51.100.22");

        assertEquals("198.51.100.22", filter.extractClientIp(request));
    }

    @Test
    void fallsBackToTrustedProxyAddressWhenForwardedHeaderIsBlank() {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiterService, true, "10.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "  ");
        assertEquals("10.0.0.1", filter.extractClientIp(request));
    }

    @Test
    void rejectsWildcardTrustedProxyConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimitingFilter(rateLimiterService, true, "*"));
    }

    @Test
    void rejectsXForwardedForWhenDirectRemoteAddrIsNotTrustedProxy() {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiterService, true, "10.0.0.1");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50"); // Not in trustedProxies
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String extractedIp = filter.extractClientIp(request);
        assertEquals("203.0.113.50", extractedIp, "Must ignore forwarded header when proxy itself is not trusted");
    }
}
