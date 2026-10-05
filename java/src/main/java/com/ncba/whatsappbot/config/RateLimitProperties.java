package com.ncba.whatsappbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fixed-window rate-limit settings for the public webhook endpoint. Port of the
 * .NET RATE_LIMIT_PERMIT / RATE_LIMIT_WINDOW_SECONDS configuration. Generous
 * defaults so local demos are unaffected; only abnormal bursts get HTTP 429.
 */
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private int permit = 300;

    private int windowSeconds = 60;

    public int getPermit() {
        return permit;
    }

    public void setPermit(int permit) {
        this.permit = permit;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
