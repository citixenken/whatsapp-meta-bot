package com.ncba.whatsappbot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEntry(
        @JsonProperty("id") String id,
        @JsonProperty("changes") List<WebhookChange> changes) {
}
