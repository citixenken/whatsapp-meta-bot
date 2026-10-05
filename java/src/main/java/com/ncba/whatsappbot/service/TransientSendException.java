package com.ncba.whatsappbot.service;

/**
 * Signals a transient Meta Graph API send failure (HTTP 5xx / 408 / 429) so the
 * Resilience4j retry and circuit-breaker policies engage. Carries the status
 * and response body for diagnostic logging after retries are exhausted.
 */
public class TransientSendException extends RuntimeException {

    private final int status;
    private final String body;

    public TransientSendException(int status, String body) {
        super("Transient Meta send failure: " + status);
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
