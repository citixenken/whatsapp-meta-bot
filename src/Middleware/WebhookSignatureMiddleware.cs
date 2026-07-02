using Microsoft.Extensions.Options;
using WhatsAppMetaBot.Configuration;
using WhatsAppMetaBot.Security;

namespace WhatsAppMetaBot.Middleware;

/// <summary>
/// Verifies the HMAC signature on inbound webhook POSTs (SEC-01). When APP_SECRET
/// is set, unsigned or forged requests are rejected with 403. Verification is
/// skipped only in the Development environment when no App Secret is configured
/// (local demo); outside Development a missing App Secret is rejected (and the
/// app also fails fast at startup) so the endpoint can never fail open.
/// </summary>
public sealed class WebhookSignatureMiddleware
{
    private const string SignatureHeader = "X-Hub-Signature-256";

    private readonly RequestDelegate _next;
    private readonly IOptions<WhatsAppOptions> _options;
    private readonly IHostEnvironment _environment;
    private readonly ILogger<WebhookSignatureMiddleware> _logger;

    public WebhookSignatureMiddleware(
        RequestDelegate next,
        IOptions<WhatsAppOptions> options,
        IHostEnvironment environment,
        ILogger<WebhookSignatureMiddleware> logger)
    {
        _next = next;
        _options = options;
        _environment = environment;
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
            if (_environment.IsDevelopment())
            {
                // Development-only fallback so local demos work without an App
                // Secret. Startup fails fast outside Development (see Program.cs),
                // so this branch is only reachable during local development.
                await _next(context);
                return;
            }

            // Defense in depth: never fail open outside Development.
            _logger.LogError(
                "APP_SECRET is not configured outside Development; rejecting webhook.");
            context.Response.StatusCode = StatusCodes.Status403Forbidden;
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
