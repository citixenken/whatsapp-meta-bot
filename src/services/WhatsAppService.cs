using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Options;
using WhatsAppMetaBot.Configuration;
using WhatsAppMetaBot.Models;

namespace WhatsAppMetaBot.Services;

/// <summary>
/// Handles Meta webhook verification, inbound message processing (enqueuing
/// replies for the background sender) and outbound sends through the Meta
/// WhatsApp Cloud API.
/// </summary>
public sealed class WhatsAppService : IWhatsAppService
{
    private static readonly TimeSpan IdempotencyWindow = TimeSpan.FromMinutes(10);

    private readonly HttpClient _httpClient;
    private readonly ILogger<WhatsAppService> _logger;
    private readonly WhatsAppOptions _options;
    private readonly IOutboundQueue _queue;
    private readonly IMemoryCache _cache;

    public WhatsAppService(
        HttpClient httpClient,
        ILogger<WhatsAppService> logger,
        IOptions<WhatsAppOptions> options,
        IOutboundQueue queue,
        IMemoryCache cache)
    {
        _httpClient = httpClient;
        _logger = logger;
        _options = options.Value;
        _queue = queue;
        _cache = cache;
    }

    /// <inheritdoc />
    public string? VerifyWebhook(string? mode, string? token, string? challenge)
    {
        if (mode == "subscribe" && FixedTimeEquals(token, _options.VerifyToken))
        {
            _logger.LogInformation("Webhook verified");
            return challenge;
        }

        return null;
    }

    /// <inheritdoc />
    public Task HandleIncomingMessageAsync(WebhookPayload payload)
    {
        foreach (var entry in payload.Entry ?? new List<WebhookEntry>())
        {
            foreach (var change in entry.Changes ?? new List<WebhookChange>())
            {
                HandleStatuses(change.Value?.Statuses);
                HandleMessages(change.Value?.Messages);
            }
        }

        return Task.CompletedTask;
    }

    /// <summary>Logs delivery/read/failed status callbacks (CFG-04).</summary>
    private void HandleStatuses(List<WhatsAppStatus>? statuses)
    {
        if (statuses is null)
        {
            return;
        }

        foreach (var status in statuses)
        {
            _logger.LogInformation(
                "Delivery status {Status} for message {MessageId}",
                status.Status, status.Id);
        }
    }

    private void HandleMessages(List<WhatsAppMessage>? messages)
    {
        if (messages is null)
        {
            return;
        }

        foreach (var message in messages)
        {
            var from = message.From;

            if (string.IsNullOrEmpty(from))
            {
                continue;
            }

            // De-duplicate redelivered webhooks by message id (REL-02).
            if (IsDuplicate(message.Id))
            {
                _logger.LogInformation("Skipping duplicate message {MessageId}", message.Id);
                continue;
            }

            // Only handle text messages
            if (message.Type != "text")
            {
                Enqueue(from, "⚠️ Only text messages are supported in this MVP.");
                continue;
            }

            var text = message.Text?.Body ?? string.Empty;

            _logger.LogInformation(
                "Incoming text from {From} ({Length} chars)",
                PiiMasker.MaskPhone(from), text.Length);

            Enqueue(from, ReplyGenerator.Generate(text));
        }
    }

    private bool IsDuplicate(string? messageId)
    {
        if (string.IsNullOrEmpty(messageId))
        {
            return false;
        }

        if (_cache.TryGetValue(messageId, out _))
        {
            return true;
        }

        _cache.Set(messageId, true, IdempotencyWindow);
        return false;
    }

    private void Enqueue(string to, string body)
    {
        if (!_queue.TryEnqueue(new OutboundJob(to, body)))
        {
            _logger.LogWarning("Outbound queue full; dropped message to {To}", PiiMasker.MaskPhone(to));
        }
    }

    /// <inheritdoc />
    public async Task SendWhatsAppMessageAsync(string to, string body, CancellationToken cancellationToken = default)
    {
        var payload = new OutboundMessage
        {
            To = to,
            Text = new OutboundMessageText { Body = body }
        };

        using var request = new HttpRequestMessage(HttpMethod.Post, _options.MessagesEndpoint)
        {
            Content = JsonContent.Create(payload)
        };
        request.Headers.TryAddWithoutValidation("Authorization", $"Bearer {_options.AccessToken}");

        var response = await _httpClient.SendAsync(request, cancellationToken);

        if (response.IsSuccessStatusCode)
        {
            _logger.LogInformation("Sent message to {To}", PiiMasker.MaskPhone(to));
        }
        else
        {
            var error = await response.Content.ReadAsStringAsync(cancellationToken);
            _logger.LogError("Meta send error ({StatusCode}): {Error}", (int)response.StatusCode, error);
        }
    }

    /// <summary>Constant-time comparison for the verify token (SEC-05).</summary>
    private static bool FixedTimeEquals(string? left, string? right)
    {
        if (left is null || right is null)
        {
            return false;
        }

        var leftBytes = Encoding.UTF8.GetBytes(left);
        var rightBytes = Encoding.UTF8.GetBytes(right);
        return CryptographicOperations.FixedTimeEquals(leftBytes, rightBytes);
    }
}
