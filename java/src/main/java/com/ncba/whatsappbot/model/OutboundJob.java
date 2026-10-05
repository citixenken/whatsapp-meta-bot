package com.ncba.whatsappbot.model;

/** A queued outbound text reply: recipient number + message body. */
public record OutboundJob(String to, String body) {
}
