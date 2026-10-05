package com.ncba.whatsappbot.service;

import com.ncba.whatsappbot.model.WebhookPayload;

/**
 * Mirrors the responsibilities of the .NET {@code IWhatsAppService}: webhook
 * verification, inbound message handling and outbound sends.
 */
public interface WhatsAppService {

    /**
     * Verifies the webhook subscription challenge (Meta requirement). Returns
     * the challenge string when verification succeeds, otherwise {@code null}.
     */
    String verifyWebhook(String mode, String token, String challenge);

    /** Processes an inbound webhook payload and enqueues any outbound replies. */
    void handleIncomingMessage(WebhookPayload payload);

    /** Sends a text message to a recipient via the Meta Cloud API. */
    void sendWhatsAppMessage(String to, String body);
}
