using System.Net.Http.Json;
using WhatsAppMetaBot.Models;

namespace WhatsAppMetaBot.Services;

/// <summary>
/// C#/.NET port of services/whatsappService.js. Handles Meta webhook
/// verification, inbound message processing and outbound sends through the
/// Meta WhatsApp Cloud API.
/// </summary>
public sealed class WhatsAppService : IWhatsAppService
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<WhatsAppService> _logger;
    private readonly IConfiguration _config;

    public WhatsAppService(
        HttpClient httpClient,
        ILogger<WhatsAppService> logger,
        IConfiguration config)
    {
        _httpClient = httpClient;
        _logger = logger;
        _config = config;
    }

    /// <inheritdoc />
    public string? VerifyWebhook(string? mode, string? token, string? challenge)
    {
        var verifyToken = _config["VERIFY_TOKEN"];

        if (mode == "subscribe" && token == verifyToken)
        {
            _logger.LogInformation("✅ Webhook verified");
            return challenge;
        }

        return null;
    }

    /// <inheritdoc />
    public async Task HandleIncomingMessageAsync(WebhookPayload payload)
    {
        var entries = payload.Entry ?? new List<WebhookEntry>();

        foreach (var entry in entries)
        {
            var changes = entry.Changes ?? new List<WebhookChange>();

            foreach (var change in changes)
            {
                var messages = change.Value?.Messages;

                if (messages is null)
                {
                    continue;
                }

                foreach (var message in messages)
                {
                    var from = message.From;

                    if (string.IsNullOrEmpty(from))
                    {
                        continue;
                    }

                    // Only handle text messages
                    if (message.Type != "text")
                    {
                        _ = SafeSendAsync(
                            from,
                            "⚠️ Only text messages are supported in this MVP.");
                        continue;
                    }

                    var text = message.Text?.Body ?? string.Empty;

                    _logger.LogInformation("📩 Incoming from {From}: {Text}", from, text);

                    var reply = GenerateReply(text);

                    // IMPORTANT: non-blocking send (Meta best practice)
                    _ = SafeSendAsync(from, reply);
                }
            }
        }

        await Task.CompletedTask;
    }

    /// <summary>
    /// MVP business logic ported from generateReply in whatsappService.js.
    /// </summary>
    private static string GenerateReply(string text)
    {
        var msg = text.ToLowerInvariant();

        if (msg.Contains("hi") || msg.Contains("hello"))
        {
            return "Hello 👋 Welcome to Fintech MVP Bot";
        }

        if (msg.Contains("balance"))
        {
            return "Your balance feature is coming soon 🚧";
        }

        if (msg.Contains("loan"))
        {
            return "Loan services will be available in next phase 📊";
        }

        return "I received your message 👍 (MVP mode)";
    }

    /// <inheritdoc />
    public async Task SendWhatsAppMessageAsync(string to, string body)
    {
        var phoneNumberId = _config["PHONE_NUMBER_ID"];
        var token = _config["WHATSAPP_TOKEN"];

        var url = $"https://graph.facebook.com/v20.0/{phoneNumberId}/messages";

        var payload = new OutboundMessage
        {
            To = to,
            Text = new OutboundMessageText { Body = body }
        };

        using var request = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(payload)
        };
        request.Headers.TryAddWithoutValidation("Authorization", $"Bearer {token}");

        var response = await _httpClient.SendAsync(request);

        if (response.IsSuccessStatusCode)
        {
            _logger.LogInformation("📤 Sent to {To}", to);
        }
        else
        {
            var error = await response.Content.ReadAsStringAsync();
            _logger.LogError("Meta send error: {Error}", error);
        }
    }

    /// <summary>
    /// Fire-and-forget send wrapper that mirrors the .catch() error logging
    /// used in the Node.js implementation.
    /// </summary>
    private async Task SafeSendAsync(string to, string body)
    {
        try
        {
            await SendWhatsAppMessageAsync(to, body);
        }
        catch (Exception ex)
        {
            _logger.LogError("Send error: {Message}", ex.Message);
        }
    }
}
