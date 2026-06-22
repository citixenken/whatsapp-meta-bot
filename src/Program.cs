using System.Text.Json;
using DotNetEnv;
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

// HMAC verification for inbound webhooks (SEC-01); no-op until APP_SECRET is set.
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
startupLogger.LogInformation("WhatsApp bot running on port {Port}", port);

app.Run();

