package com.ncba.whatsappbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppStatus(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("recipient_id") String recipientId,
        @JsonProperty("timestamp") String timestamp) {
}
