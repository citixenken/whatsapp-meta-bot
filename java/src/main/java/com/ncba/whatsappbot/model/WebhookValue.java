package com.ncba.whatsappbot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookValue(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("messages") List<WhatsAppMessage> messages,
        @JsonProperty("statuses") List<WhatsAppStatus> statuses) {
}
