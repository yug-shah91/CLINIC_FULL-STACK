package com.clinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final boolean behindProxy;
    private final Set<String> trustedProxies;

    public RateLimitingFilter(
            RateLimiterService rateLimiterService,
            @Value("${clinic.security.behind-proxy:false}") boolean behindProxy,
            @Value("${clinic.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String trustedProxies) {
        this.rateLimiterService = rateLimiterService;
        this.behindProxy = behindProxy;
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (this.trustedProxies.contains("*")) {
            throw new IllegalArgumentException("Wildcard trusted proxies are unsafe; configure explicit proxy IP addresses");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = extractClientIp(request);
            if (path.endsWith("/api/auth/login") && !rateLimiterService.allowLogin(clientIp)) {
                rejectRateLimited(response, "Too many login attempts. Please try again later.");
                return;
            }
            if (path.endsWith("/api/auth/register") && !rateLimiterService.allowRegister(clientIp)) {
                rejectRateLimited(response, "Too many registration attempts. Please try again later.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Trust forwarded addresses only when the immediate peer is explicitly trusted.
     * Walk the forwarded chain right-to-left and choose the first non-trusted address.
     * This assumes the trusted proxy appends the actual client address to X-Forwarded-For.
     */
    public String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!behindProxy || remoteAddr == null || !trustedProxies.contains(remoteAddr)) {
            return remoteAddr != null ? remoteAddr : "UNKNOWN";
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }

        List<String> chain = Arrays.stream(forwarded.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        for (int i = chain.size() - 1; i >= 0; i--) {
            String candidate = chain.get(i);
            if (!trustedProxies.contains(candidate)) {
                return candidate;
            }
        }
        return remoteAddr;
    }

    private void rejectRateLimited(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(rateLimiterService.getWindowSeconds()));
        response.getWriter().write(String.format(
                "{\"status\":429,\"error\":\"%s\",\"timestamp\":\"%s\"}",
                message, Instant.now()));
    }
}
