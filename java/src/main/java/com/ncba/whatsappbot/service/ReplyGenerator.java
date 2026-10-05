package com.ncba.whatsappbot.service;

import java.util.Locale;

/**
 * MVP business logic: maps inbound text to a canned reply. Port of the .NET
 * {@code ReplyGenerator}. Kept as a pure function so it can be unit-tested.
 * Replace with a real intent/NLP engine and core-banking integration for
 * production.
 */
public final class ReplyGenerator {

    private ReplyGenerator() {
    }

    public static String generate(String text) {
        String msg = text.toLowerCase(Locale.ROOT);

        if (msg.contains("hi") || msg.contains("hello")) {
            return "Hello 👋 Welcome to Fintech MVP Bot";
        }

        if (msg.contains("balance")) {
            return "Your balance feature is coming soon 🚧";
        }

        if (msg.contains("loan")) {
            return "Loan services will be available in next phase 📊";
        }

        return "I received your message 👍 (MVP mode)";
    }
}
