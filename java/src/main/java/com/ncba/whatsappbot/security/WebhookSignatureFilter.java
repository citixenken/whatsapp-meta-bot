package com.ncba.whatsappbot.security;

import java.io.IOException;

import com.ncba.whatsappbot.config.WhatsAppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the HMAC signature on inbound webhook POSTs. When APP_SECRET is set,
 * unsigned or forged requests are rejected with 403. Verification is skipped
 * only in the {@code dev} profile when no App Secret is configured (local demo);
 * otherwise a missing App Secret is rejected (and the app also fails fast at
 * startup) so the endpoint can never fail open. Port of the .NET
 * {@code WebhookSignatureMiddleware}.
 */
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureFilter.class);

    private final WhatsAppProperties properties;
    private final Environment environment;

    public WebhookSignatureFilter(WhatsAppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        String appSecret = properties.getAppSecret();
        if (!StringUtils.hasText(appSecret)) {
            if (isDevelopment()) {
                // Development-only fallback so local demos work without an App
                // Secret. Startup fails fast outside the dev profile.
                filterChain.doFilter(request, response);
                return;
            }

            // Defense in depth: never fail open outside Development.
            log.error("APP_SECRET is not configured outside the dev profile; rejecting webhook.");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Buffer the body so we can hash the raw bytes and still let Spring read it.
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        String header = request.getHeader(SIGNATURE_HEADER);

        if (!WebhookSignature.isValid(wrapped.getCachedBody(), header, appSecret)) {
            log.warn("Rejected webhook with invalid or missing signature");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    private boolean isDevelopment() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("dev")) {
                return true;
            }
        }
        return false;
    }
}
