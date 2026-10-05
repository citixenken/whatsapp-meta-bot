package com.ncba.whatsappbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookChange(
        @JsonProperty("value") WebhookValue value,
        @JsonProperty("field") String field) {
}
