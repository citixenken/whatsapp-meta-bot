package com.ncba.whatsappbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Outbound message payload sent to the Meta Graph API. */
public record OutboundMessage(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("to") String to,
        @JsonProperty("type") String type,
        @JsonProperty("text") OutboundMessageText text) {

    /** Convenience constructor mirroring the .NET defaults (whatsapp / text). */
    public OutboundMessage(String to, String body) {
        this("whatsapp", to, "text", new OutboundMessageText(body));
    }
}
