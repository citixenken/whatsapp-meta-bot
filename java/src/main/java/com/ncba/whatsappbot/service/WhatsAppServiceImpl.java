package com.ncba.whatsappbot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.ncba.whatsappbot.config.WhatsAppProperties;
import com.ncba.whatsappbot.model.OutboundJob;
import com.ncba.whatsappbot.model.OutboundMessage;
import com.ncba.whatsappbot.model.WebhookChange;
import com.ncba.whatsappbot.model.WebhookEntry;
import com.ncba.whatsappbot.model.WebhookPayload;
import com.ncba.whatsappbot.model.WebhookValue;
import com.ncba.whatsappbot.model.WhatsAppMessage;
import com.ncba.whatsappbot.model.WhatsAppStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Handles Meta webhook verification, inbound message processing (enqueuing
 * replies for the background sender) and outbound sends through the Meta
 * WhatsApp Cloud API. Port of the .NET {@code WhatsAppService}.
 */
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final RestClient restClient;
    private final WhatsAppProperties properties;
    private final OutboundQueue queue;
    private final Cache<String, Boolean> idempotencyCache;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public WhatsAppServiceImpl(RestClient whatsAppRestClient,
                               WhatsAppProperties properties,
                               OutboundQueue queue,
                               Cache<String, Boolean> idempotencyCache,
                               Retry whatsAppRetry,
                               CircuitBreaker whatsAppCircuitBreaker) {
        this.restClient = whatsAppRestClient;
        this.properties = properties;
        this.queue = queue;
        this.idempotencyCache = idempotencyCache;
        this.retry = whatsAppRetry;
        this.circuitBreaker = whatsAppCircuitBreaker;
    }

    @Override
    public String verifyWebhook(String mode, String token, String challenge) {
        if ("subscribe".equals(mode) && fixedTimeEquals(token, properties.getVerifyToken())) {
            log.info("Webhook verified");
            return challenge;
        }
        return null;
    }

    @Override
    public void handleIncomingMessage(WebhookPayload payload) {
        if (payload == null || payload.entry() == null) {
            return;
        }

        for (WebhookEntry entry : payload.entry()) {
            if (entry == null || entry.changes() == null) {
                continue;
            }
            for (WebhookChange change : entry.changes()) {
                WebhookValue value = change == null ? null : change.value();
                if (value == null) {
                    continue;
                }
                handleStatuses(value.statuses());
                handleMessages(value.messages());
            }
        }
    }

    /** Logs delivery/read/failed status callbacks. */
    private void handleStatuses(List<WhatsAppStatus> statuses) {
        if (statuses == null) {
            return;
        }
        for (WhatsAppStatus status : statuses) {
            log.info("Delivery status {} for message {}", status.status(), status.id());
        }
    }

    private void handleMessages(List<WhatsAppMessage> messages) {
        if (messages == null) {
            return;
        }

        for (WhatsAppMessage message : messages) {
            String from = message.from();
            if (from == null || from.isEmpty()) {
                continue;
            }

            // De-duplicate redelivered webhooks by message id.
            if (isDuplicate(message.id())) {
                log.info("Skipping duplicate message {}", message.id());
                continue;
            }

            // Only handle text messages.
            if (!"text".equals(message.type())) {
                enqueue(from, "⚠️ Only text messages are supported in this MVP.");
                continue;
            }

            String text = message.text() != null && message.text().body() != null
                    ? message.text().body()
                    : "";

            log.info("Incoming text from {} ({} chars)", PiiMasker.maskPhone(from), text.length());

            enqueue(from, ReplyGenerator.generate(text));
        }
    }

    private boolean isDuplicate(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        if (idempotencyCache.getIfPresent(messageId) != null) {
            return true;
        }
        idempotencyCache.put(messageId, Boolean.TRUE);
        return false;
    }

    private void enqueue(String to, String body) {
        if (!queue.tryEnqueue(new OutboundJob(to, body))) {
            log.warn("Outbound queue full; dropped message to {}", PiiMasker.maskPhone(to));
        }
    }

    @Override
    public void sendWhatsAppMessage(String to, String body) {
        OutboundMessage payload = new OutboundMessage(to, body);

        // Wrap the HTTP call with retry + circuit breaker (Polly equivalent).
        Supplier<SendResult> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> doSend(payload)));

        try {
            SendResult result = decorated.get();
            if (result.success()) {
                log.info("Sent message to {}", PiiMasker.maskPhone(to));
            } else {
                log.error("Meta send error ({}): {}", result.status(), result.body());
            }
        } catch (TransientSendException ex) {
            // Retries exhausted on a transient failure.
            log.error("Meta send error ({}): {}", ex.status(), ex.body());
        } catch (Exception ex) {
            // Circuit open, network error, etc.
            log.error("Meta send error: {}", ex.getMessage());
        }
    }

    private SendResult doSend(OutboundMessage payload) {
        ResponseEntity<String> response = restClient.post()
                .uri(properties.getMessagesEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                // Suppress RestClient's default 4xx/5xx exception so we can
                // inspect the status and decide whether it is retryable.
                .onStatus(status -> true, (request, clientResponse) -> {
                })
                .toEntity(String.class);

        int status = response.getStatusCode().value();
        if (response.getStatusCode().is2xxSuccessful()) {
            return new SendResult(true, status, null);
        }

        String errorBody = response.getBody();
        if (isTransient(status)) {
            throw new TransientSendException(status, errorBody);
        }
        return new SendResult(false, status, errorBody);
    }

    private static boolean isTransient(int status) {
        return status >= 500 || status == 408 || status == 429;
    }

    /** Constant-time comparison for the verify token. */
    private static boolean fixedTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private record SendResult(boolean success, int status, String body) {
    }
}
