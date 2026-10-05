package com.ncba.whatsappbot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed WhatsApp configuration, validated at startup so the service
 * fails fast on misconfiguration instead of at the first request. Port of the
 * .NET {@code WhatsAppOptions}. Flat env keys (VERIFY_TOKEN, WHATSAPP_TOKEN,
 * PHONE_NUMBER_ID, ...) are mapped in {@code application.yml} so existing .env
 * files keep working unchanged.
 */
@Validated
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    @NotBlank(message = "VERIFY_TOKEN is required.")
    private String verifyToken = "";

    @NotBlank(message = "WHATSAPP_TOKEN is required.")
    private String accessToken = "";

    @NotBlank(message = "PHONE_NUMBER_ID is required.")
    private String phoneNumberId = "";

    /**
     * Meta App Secret. Optional: when set, inbound webhooks are verified with
     * HMAC-SHA256 (X-Hub-Signature-256). Left unset only for local dev demos.
     */
    private String appSecret;

    private String graphApiBaseUrl = "https://graph.facebook.com";

    private String graphApiVersion = "v20.0";

    /** Resolved Graph API "send message" endpoint for the configured number. */
    public String getMessagesEndpoint() {
        String base = graphApiBaseUrl.endsWith("/")
                ? graphApiBaseUrl.substring(0, graphApiBaseUrl.length() - 1)
                : graphApiBaseUrl;
        return base + "/" + graphApiVersion + "/" + phoneNumberId + "/messages";
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getGraphApiBaseUrl() {
        return graphApiBaseUrl;
    }

    public void setGraphApiBaseUrl(String graphApiBaseUrl) {
        this.graphApiBaseUrl = graphApiBaseUrl;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public void setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }
}
