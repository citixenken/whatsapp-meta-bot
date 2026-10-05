package com.ncba.whatsappbot.config;

import com.ncba.whatsappbot.security.WebhookSignatureFilter;
import com.ncba.whatsappbot.web.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Registers the servlet filters in the same order as the .NET middleware
 * pipeline: rate limiting runs first (shed floods early), then HMAC signature
 * verification. Both are scoped to {@code /webhook}.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
                new RateLimitFilter(properties.getPermit(), properties.getWindowSeconds()));
        registration.addUrlPatterns("/webhook");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<WebhookSignatureFilter> webhookSignatureFilter(
            WhatsAppProperties properties, Environment environment) {
        FilterRegistrationBean<WebhookSignatureFilter> registration = new FilterRegistrationBean<>(
                new WebhookSignatureFilter(properties, environment));
        registration.addUrlPatterns("/webhook");
        registration.setOrder(2);
        return registration;
    }
}
