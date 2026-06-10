using System.Text.Json.Serialization;

namespace WhatsAppMetaBot.Models;

/// <summary>
/// Outbound message payload sent to the Meta Graph API.
/// </summary>
public sealed class OutboundMessage
{
    [JsonPropertyName("messaging_product")]
    public string MessagingProduct { get; set; } = "whatsapp";

    [JsonPropertyName("to")]
    public string To { get; set; } = string.Empty;

    [JsonPropertyName("type")]
    public string Type { get; set; } = "text";

    [JsonPropertyName("text")]
    public OutboundMessageText Text { get; set; } = new();
}

public sealed class OutboundMessageText
{
    [JsonPropertyName("body")]
    public string Body { get; set; } = string.Empty;
}
