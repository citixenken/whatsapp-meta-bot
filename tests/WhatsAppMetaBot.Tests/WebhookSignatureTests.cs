using System.Security.Cryptography;
using System.Text;
using WhatsAppMetaBot.Security;
using Xunit;

namespace WhatsAppMetaBot.Tests;

public class WebhookSignatureTests
{
    private const string Secret = "test-app-secret";

    private static string Sign(byte[] body)
    {
        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(Secret));
        return "sha256=" + Convert.ToHexStringLower(hmac.ComputeHash(body));
    }

    [Fact]
    public void IsValid_true_for_correct_signature()
    {
        var body = Encoding.UTF8.GetBytes("{\"object\":\"whatsapp_business_account\"}");
        Assert.True(WebhookSignature.IsValid(body, Sign(body), Secret));
    }

    [Fact]
    public void IsValid_false_for_tampered_body()
    {
        var body = Encoding.UTF8.GetBytes("{\"amount\":1}");
        var signature = Sign(body);
        var tampered = Encoding.UTF8.GetBytes("{\"amount\":2}");

        Assert.False(WebhookSignature.IsValid(tampered, signature, Secret));
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("deadbeef")]
    [InlineData("sha256=not-a-valid-hash")]
    public void IsValid_false_for_missing_or_malformed_header(string? header)
    {
        var body = Encoding.UTF8.GetBytes("{}");
        Assert.False(WebhookSignature.IsValid(body, header, Secret));
    }
}
