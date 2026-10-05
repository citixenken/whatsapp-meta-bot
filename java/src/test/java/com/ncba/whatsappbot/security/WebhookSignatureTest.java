package com.ncba.whatsappbot.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookSignatureTest {

    private static final String SECRET = "test-app-secret";

    private static String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void isValid_true_for_correct_signature() throws Exception {
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        assertTrue(WebhookSignature.isValid(body, sign(body), SECRET));
    }

    @Test
    void isValid_false_for_tampered_body() throws Exception {
        byte[] body = "{\"amount\":1}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body);
        byte[] tampered = "{\"amount\":2}".getBytes(StandardCharsets.UTF_8);

        assertFalse(WebhookSignature.isValid(tampered, signature, SECRET));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "deadbeef", "sha256=not-a-valid-hash"})
    void isValid_false_for_missing_or_malformed_header(String header) {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        assertFalse(WebhookSignature.isValid(body, header, SECRET));
    }
}
