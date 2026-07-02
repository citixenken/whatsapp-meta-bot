using System.Text.Json;
using System.Threading.RateLimiting;
using DotNetEnv;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Extensions.Options;
using WhatsAppMetaBot.Configuration;
using WhatsAppMetaBot.Middleware;
using WhatsAppMetaBot.Services;

// Load variables from a local .env file (parity with dotenv in the Node MVP).
// Real environment variables always take precedence over .env values.
Env.TraversePath().Load();

var builder = WebApplication.CreateBuilder(args);

// Surface .env / process environment variables through IConfiguration so
// services can read VERIFY_TOKEN, WHATSAPP_TOKEN and PHONE_NUMBER_ID.
builder.Configuration.AddEnvironmentVariables();

// Structured JSON logs outside Development (OBS-02); Development keeps the
// human-readable console for a smooth local experience.
if (!builder.Environment.IsDevelopment())
{
    builder.Logging.ClearProviders();
    builder.Logging.AddJsonConsole();
}

// Bind to the same port the Node MVP used (default 3000) so the existing
// ngrok + Meta webhook setup keeps working unchanged.
var port = builder.Configuration["PORT"] ?? "3000";
builder.WebHost.UseUrls($"http://0.0.0.0:{port}");

// Strongly-typed, validated config (CFG-01). Maps the existing flat env keys so
// current .env files keep working, but fails fast at startup if required values
// are missing instead of surfacing as a bad request later.
builder.Services.AddOptions<WhatsAppOptions>()
    .Configure<IConfiguration>((options, config) =>
    {
        options.VerifyToken = config["VERIFY_TOKEN"] ?? string.Empty;
        options.AccessToken = config["WHATSAPP_TOKEN"] ?? string.Empty;
        options.PhoneNumberId = config["PHONE_NUMBER_ID"] ?? string.Empty;
        options.AppSecret = config["APP_SECRET"];
        options.GraphApiBaseUrl = config["GRAPH_API_BASE_URL"] ?? "https://graph.facebook.com";
        options.GraphApiVersion = config["GRAPH_API_VERSION"] ?? "v20.0";
    })
    .ValidateDataAnnotations()
    .ValidateOnStart();

builder.Services.AddControllers();
builder.Services.AddMemoryCache();
builder.Services.AddHealthChecks();
builder.Services.AddProblemDetails();

// Rate limiting for the public webhook endpoint to blunt request floods and
// billable-send abuse (defense in depth). Limits are generous and configurable
// via env so local demos are unaffected; only abnormal bursts get HTTP 429.
var rateLimitPermit = int.TryParse(builder.Configuration["RATE_LIMIT_PERMIT"], out var permit) ? permit : 300;
var rateLimitWindowSeconds = int.TryParse(builder.Configuration["RATE_LIMIT_WINDOW_SECONDS"], out var windowSeconds) ? windowSeconds : 60;
builder.Services.AddRateLimiter(rateLimiter =>
{
    rateLimiter.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    rateLimiter.AddFixedWindowLimiter("webhook", limiter =>
    {
        limiter.PermitLimit = rateLimitPermit;
        limiter.Window = TimeSpan.FromSeconds(rateLimitWindowSeconds);
        limiter.QueueLimit = 0;
    });
});

// In-process outbound pipeline: enqueue on the request thread, send on a worker.
builder.Services.AddSingleton<IOutboundQueue, OutboundQueue>();
builder.Services.AddHostedService<OutboundMessageWorker>();

// Outbound HTTP with resilience: timeout, retry with backoff, circuit breaker (REL-03).
builder.Services
    .AddHttpClient<IWhatsAppService, WhatsAppService>()
    .AddStandardResilienceHandler();

var app = builder.Build();

// Centralized exception handling (CFG-03); returns ProblemDetails responses.
app.UseExceptionHandler();

// Throttle inbound requests before the (more expensive) signature check so
// floods are shed early with HTTP 429.
app.UseRateLimiter();

// HMAC verification for inbound webhooks (SEC-01). Enforced in every deployed
// environment; skipped only in Development when APP_SECRET is unset (local demo).
app.UseMiddleware<WebhookSignatureMiddleware>();

// Basic health check + dedicated liveness/readiness probes (OBS-01).
app.MapGet("/", () => Results.Content("WhatsApp Meta Bot is running...", "text/plain"));
app.MapHealthChecks("/healthz");
app.MapHealthChecks("/readyz");

// DEBUG endpoint (Development only — never expose raw payloads in production, TD-07).
if (app.Environment.IsDevelopment())
{
    app.MapPost("/debug", async (HttpContext context) =>
    {
        var logger = context.RequestServices.GetRequiredService<ILoggerFactory>()
            .CreateLogger("Debug");

        using var reader = new StreamReader(context.Request.Body);
        var raw = await reader.ReadToEndAsync();

        if (!string.IsNullOrWhiteSpace(raw))
        {
            try
            {
                using var doc = JsonDocument.Parse(raw);
                logger.LogInformation("DEBUG payload: {Payload}",
                    JsonSerializer.Serialize(doc, new JsonSerializerOptions { WriteIndented = true }));
            }
            catch (JsonException)
            {
                logger.LogInformation("DEBUG payload: {Payload}", raw);
            }
        }

        return Results.Ok();
    });
}

// Webhook routes (GET verify + POST messages)
app.MapControllers();

var startupLogger = app.Services.GetRequiredService<ILogger<Program>>();

// Enforce webhook signature verification outside Development (SEC-01). Local
// demos may run without an App Secret (verification is skipped in Development so
// testing against Meta's test number keeps working), but any deployed
// environment fails fast rather than silently accepting forgeable webhooks.
var whatsAppOptions = app.Services.GetRequiredService<IOptions<WhatsAppOptions>>().Value;
if (string.IsNullOrWhiteSpace(whatsAppOptions.AppSecret))
{
    if (app.Environment.IsDevelopment())
    {
        startupLogger.LogWarning(
            "APP_SECRET is not configured; inbound webhook signature verification is " +
            "DISABLED. This is only permitted in the Development environment for local demos.");
    }
    else
    {
        throw new InvalidOperationException(
            "APP_SECRET is required outside the Development environment so inbound webhook " +
            "signatures (X-Hub-Signature-256) are verified. Set APP_SECRET, or run with " +
            "ASPNETCORE_ENVIRONMENT=Development for local demos.");
    }
}

startupLogger.LogInformation("WhatsApp bot running on port {Port}", port);

app.Run();

