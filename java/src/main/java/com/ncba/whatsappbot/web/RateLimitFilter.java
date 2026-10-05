package com.ncba.whatsappbot.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Global fixed-window rate limiter for the public webhook endpoint, matching the
 * .NET {@code AddFixedWindowLimiter("webhook")} policy: a shared counter of
 * {@code permitLimit} requests per {@code window}, with no queue (excess
 * requests get HTTP 429). Only POSTs are limited; the GET verification handshake
 * is not, mirroring the .NET {@code [EnableRateLimiting]} placement.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final int permitLimit;
    private final long windowMillis;
    private final Object lock = new Object();

    private long windowStart;
    private int count;

    public RateLimitFilter(int permitLimit, int windowSeconds) {
        this.permitLimit = permitLimit;
        this.windowMillis = windowSeconds * 1000L;
        this.windowStart = System.currentTimeMillis();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && !tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryAcquire() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count = 0;
            }
            if (count >= permitLimit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
