package com.ncba.whatsappbot.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ncba.whatsappbot.service.TransientSendException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Infrastructure beans: the outbound HTTP client with resilience, the
 * idempotency cache and the Resilience4j policies. Together these replace the
 * .NET {@code HttpClientFactory} + {@code AddStandardResilienceHandler} (Polly)
 * and {@code IMemoryCache} setup, preserving the same behaviour.
 */
@Configuration
public class AppConfig {

    /** Per-attempt HTTP timeout (connect + read), matching Polly's attempt timeout. */
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient whatsAppRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) ATTEMPT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) ATTEMPT_TIMEOUT.toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /** De-duplication store for redelivered webhooks (10-minute window). */
    @Bean
    public Cache<String, Boolean> idempotencyCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(100_000)
                .build();
    }

    /**
     * Retry policy matching Polly's standard handler: 3 retries (4 attempts
     * total) with exponential backoff + jitter on transient failures.
     */
    @Bean
    public Retry whatsAppRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        Duration.ofSeconds(2), 2.0, 0.5))
                .retryExceptions(
                        TransientSendException.class,
                        ResourceAccessException.class,
                        java.io.IOException.class)
                .build();
        return Retry.of("whatsApp", config);
    }

    /**
     * Circuit breaker matching Polly's defaults: ~10% failure ratio over a
     * minimum throughput of 100 calls, 5s open state. Effectively dormant at
     * MVP traffic (same as the .NET behaviour).
     */
    @Bean
    public CircuitBreaker whatsAppCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(10)
                .recordExceptions(
                        TransientSendException.class,
                        ResourceAccessException.class,
                        java.io.IOException.class)
                .build();
        return CircuitBreaker.of("whatsApp", config);
    }
}
