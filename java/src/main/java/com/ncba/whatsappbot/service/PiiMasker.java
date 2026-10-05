package com.ncba.whatsappbot.service;

/** Helpers to keep PII (phone numbers) out of logs. Port of the .NET {@code PiiMasker}. */
public final class PiiMasker {

    private PiiMasker() {
    }

    /** Masks all but the last 4 digits of a phone number. */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "(unknown)";
        }

        if (phone.length() <= 4) {
            return "*".repeat(phone.length());
        }

        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
