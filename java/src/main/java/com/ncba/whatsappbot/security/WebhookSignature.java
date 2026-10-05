package com.ncba.whatsappbot.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates Meta's {@code X-Hub-Signature-256} header: HMAC-SHA256 over the raw
 * request body, keyed with the App Secret, compared in constant time. Port of
 * the .NET {@code WebhookSignature}.
 */
public final class WebhookSignature {

    private static final String PREFIX = "sha256=";
    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSignature() {
    }

    public static boolean isValid(byte[] body, String signatureHeader, String appSecret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }

        String providedHex = signatureHeader.substring(PREFIX.length());

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            String computedHex = HexFormat.of().formatHex(mac.doFinal(body));

            return MessageDigest.isEqual(
                    providedHex.getBytes(StandardCharsets.UTF_8),
                    computedHex.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}
