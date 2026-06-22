using Microsoft.Extensions.Options;
using WhatsAppMetaBot.Configuration;
using WhatsAppMetaBot.Security;

namespace WhatsAppMetaBot.Middleware;

/// <summary>
/// Verifies the HMAC signature on inbound webhook POSTs (SEC-01). When no App
/// Secret is configured (the MVP default) verification is skipped so existing
/// setups keep working; once APP_SECRET is set, unsigned or forged requests are
/// rejected with 403.
/// </summary>
public sealed class WebhookSignatureMiddleware
{
    private const string SignatureHeader = "X-Hub-Signature-256";

    private readonly RequestDelegate _next;
    private readonly IOptions<WhatsAppOptions> _options;
    private readonly ILogger<WebhookSignatureMiddleware> _logger;

    public WebhookSignatureMiddleware(
        RequestDelegate next,
        IOptions<WhatsAppOptions> options,
        ILogger<WebhookSignatureMiddleware> logger)
    {
        _next = next;
        _options = options;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        if (!HttpMethods.IsPost(context.Request.Method) ||
            !context.Request.Path.StartsWithSegments("/webhook"))
        {
            await _next(context);
            return;
        }

        var appSecret = _options.Value.AppSecret;
        if (string.IsNullOrWhiteSpace(appSecret))
        {
            // MVP fallback: signature verification disabled until APP_SECRET is set.
            await _next(context);
            return;
        }

        // Buffer the body so we can hash the raw bytes and still let model binding read it.
        context.Request.EnableBuffering();

        byte[] body;
        await using (var ms = new MemoryStream())
        {
            await context.Request.Body.CopyToAsync(ms);
            body = ms.ToArray();
        }
        context.Request.Body.Position = 0;

        var header = context.Request.Headers[SignatureHeader].ToString();
        if (!WebhookSignature.IsValid(body, header, appSecret))
        {
            _logger.LogWarning("Rejected webhook with invalid or missing signature");
            context.Response.StatusCode = StatusCodes.Status403Forbidden;
            return;
        }

        await _next(context);
    }
}
