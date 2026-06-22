using System.ComponentModel.DataAnnotations;

namespace WhatsAppMetaBot.Configuration;

/// <summary>
/// Strongly-typed WhatsApp configuration, validated at startup so the service
/// fails fast on misconfiguration instead of at the first request (CFG-01).
/// Values are mapped from the existing flat env keys (VERIFY_TOKEN, etc.) so
/// current .env files keep working unchanged.
/// </summary>
public sealed class WhatsAppOptions
{
    [Required(ErrorMessage = "VERIFY_TOKEN is required.")]
    public string VerifyToken { get; set; } = string.Empty;

    [Required(ErrorMessage = "WHATSAPP_TOKEN is required.")]
    public string AccessToken { get; set; } = string.Empty;

    [Required(ErrorMessage = "PHONE_NUMBER_ID is required.")]
    public string PhoneNumberId { get; set; } = string.Empty;

    /// <summary>
    /// Meta App Secret. Optional: when set, inbound webhooks are verified with
    /// HMAC-SHA256 (X-Hub-Signature-256). Left unset in the MVP, verification
    /// is skipped so existing POC setups keep working (SEC-01).
    /// </summary>
    public string? AppSecret { get; set; }

    /// <summary>Graph API base URL (configurable for testing/sovereign clouds).</summary>
    public string GraphApiBaseUrl { get; set; } = "https://graph.facebook.com";

    /// <summary>Graph API version, configurable so it can be bumped without code changes (REL-08).</summary>
    public string GraphApiVersion { get; set; } = "v20.0";

    /// <summary>Resolved Graph API "send message" endpoint for the configured number.</summary>
    public string MessagesEndpoint =>
        $"{GraphApiBaseUrl.TrimEnd('/')}/{GraphApiVersion}/{PhoneNumberId}/messages";
}
