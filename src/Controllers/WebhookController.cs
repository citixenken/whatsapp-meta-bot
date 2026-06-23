using Microsoft.AspNetCore.Mvc;
using WhatsAppMetaBot.Models;
using WhatsAppMetaBot.Services;

namespace WhatsAppMetaBot.Controllers;

/// <summary>
/// C#/.NET port of routes/webhook.js. Exposes the Meta webhook verification
/// (GET) and inbound message (POST) endpoints under /webhook.
/// </summary>
[ApiController]
[Route("webhook")]
public sealed class WebhookController : ControllerBase
{
    private readonly IWhatsAppService _whatsAppService;
    private readonly ILogger<WebhookController> _logger;

    public WebhookController(
        IWhatsAppService whatsAppService,
        ILogger<WebhookController> logger)
    {
        _whatsAppService = whatsAppService;
        _logger = logger;
    }

    /// <summary>
    /// Meta webhook verification (GET /webhook).
    /// </summary>
    [HttpGet]
    public IActionResult Verify(
        [FromQuery(Name = "hub.mode")] string? mode,
        [FromQuery(Name = "hub.verify_token")] string? token,
        [FromQuery(Name = "hub.challenge")] string? challenge)
    {
        var result = _whatsAppService.VerifyWebhook(mode, token, challenge);

        if (result is not null)
        {
            return Content(result, "text/plain");
        }

        return StatusCode(StatusCodes.Status403Forbidden);
    }

    /// <summary>
    /// Incoming WhatsApp messages (POST /webhook).
    /// </summary>
    [HttpPost]
    public async Task<IActionResult> Receive([FromBody] WebhookPayload payload)
    {
        try
        {
            await _whatsAppService.HandleIncomingMessageAsync(payload);
        }
        catch (Exception ex)
        {
            // Log the full exception (with stack trace) for diagnosability, but
            // still ack Meta with 200 so it does not retry-storm the webhook.
            _logger.LogError(ex, "Error processing inbound webhook");
        }

        // Always respond fast to Meta
        return Ok();
    }
}
