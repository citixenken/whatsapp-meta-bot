using Microsoft.AspNetCore.Http;
using WhatsAppMetaBot.Models;

namespace WhatsAppMetaBot.Services;

/// <summary>
/// Mirrors the responsibilities of the original Node.js whatsappService.js:
/// webhook verification, inbound message handling and outbound sends.
/// </summary>
public interface IWhatsAppService
{
    /// <summary>
    /// Verifies the webhook subscription challenge (Meta requirement).
    /// Returns the challenge string when verification succeeds, otherwise null.
    /// </summary>
    string? VerifyWebhook(string? mode, string? token, string? challenge);

    /// <summary>
    /// Processes an inbound webhook payload and triggers any outbound replies.
    /// </summary>
    Task HandleIncomingMessageAsync(WebhookPayload payload);

    /// <summary>
    /// Sends a text message to a recipient via the Meta Cloud API.
    /// </summary>
    Task SendWhatsAppMessageAsync(string to, string body);
}
