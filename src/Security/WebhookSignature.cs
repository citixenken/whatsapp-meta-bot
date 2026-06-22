using System.Security.Cryptography;
using System.Text;

namespace WhatsAppMetaBot.Security;

/// <summary>
/// Validates Meta's <c>X-Hub-Signature-256</c> header: HMAC-SHA256 over the raw
/// request body, keyed with the App Secret, compared in constant time (SEC-01).
/// </summary>
internal static class WebhookSignature
{
    private const string Prefix = "sha256=";

    public static bool IsValid(byte[] body, string? signatureHeader, string appSecret)
    {
        if (string.IsNullOrEmpty(signatureHeader) ||
            !signatureHeader.StartsWith(Prefix, StringComparison.Ordinal))
        {
            return false;
        }

        var providedHex = signatureHeader[Prefix.Length..];

        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(appSecret));
        var computedHex = Convert.ToHexStringLower(hmac.ComputeHash(body));

        var providedBytes = Encoding.UTF8.GetBytes(providedHex);
        var computedBytes = Encoding.UTF8.GetBytes(computedHex);

        return CryptographicOperations.FixedTimeEquals(providedBytes, computedBytes);
    }
}
