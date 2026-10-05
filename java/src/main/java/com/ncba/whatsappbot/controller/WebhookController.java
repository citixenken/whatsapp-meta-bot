package com.ncba.whatsappbot.controller;

import com.ncba.whatsappbot.model.WebhookPayload;
import com.ncba.whatsappbot.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Meta webhook verification (GET) and inbound message (POST)
 * endpoints under {@code /webhook}. Port of the .NET {@code WebhookController}.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WhatsAppService whatsAppService;

    public WebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    /** Meta webhook verification (GET /webhook). */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        String result = whatsAppService.verifyWebhook(mode, token, challenge);

        if (result != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(result);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /** Incoming WhatsApp messages (POST /webhook). */
    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) WebhookPayload payload) {
        try {
            whatsAppService.handleIncomingMessage(payload);
        } catch (Exception ex) {
            // Log the full exception for diagnosability, but still ack Meta with
            // 200 so it does not retry-storm the webhook.
            log.error("Error processing inbound webhook", ex);
        }

        // Always respond fast to Meta.
        return ResponseEntity.ok().build();
    }
}
