package com.ncba.whatsappbot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Root payload Meta delivers to the webhook (POST /webhook). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        @JsonProperty("object") String object,
        @JsonProperty("entry") List<WebhookEntry> entry) {
}
