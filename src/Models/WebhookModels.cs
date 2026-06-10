using System.Text.Json.Serialization;

namespace WhatsAppMetaBot.Models;

/// <summary>
/// Root payload Meta delivers to the webhook (POST /webhook).
/// </summary>
public sealed class WebhookPayload
{
    [JsonPropertyName("object")]
    public string? Object { get; set; }

    [JsonPropertyName("entry")]
    public List<WebhookEntry>? Entry { get; set; }
}

public sealed class WebhookEntry
{
    [JsonPropertyName("id")]
    public string? Id { get; set; }

    [JsonPropertyName("changes")]
    public List<WebhookChange>? Changes { get; set; }
}

public sealed class WebhookChange
{
    [JsonPropertyName("value")]
    public WebhookValue? Value { get; set; }

    [JsonPropertyName("field")]
    public string? Field { get; set; }
}

public sealed class WebhookValue
{
    [JsonPropertyName("messaging_product")]
    public string? MessagingProduct { get; set; }

    [JsonPropertyName("messages")]
    public List<WhatsAppMessage>? Messages { get; set; }
}

public sealed class WhatsAppMessage
{
    [JsonPropertyName("from")]
    public string? From { get; set; }

    [JsonPropertyName("id")]
    public string? Id { get; set; }

    [JsonPropertyName("timestamp")]
    public string? Timestamp { get; set; }

    [JsonPropertyName("type")]
    public string? Type { get; set; }

    [JsonPropertyName("text")]
    public WhatsAppMessageText? Text { get; set; }
}

public sealed class WhatsAppMessageText
{
    [JsonPropertyName("body")]
    public string? Body { get; set; }
}
