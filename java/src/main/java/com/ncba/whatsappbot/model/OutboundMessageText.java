package com.ncba.whatsappbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OutboundMessageText(
        @JsonProperty("body") String body) {
}
