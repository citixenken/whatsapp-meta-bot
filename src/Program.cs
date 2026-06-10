using System.Text.Json;
using DotNetEnv;
using WhatsAppMetaBot.Services;

// Load variables from a local .env file (parity with dotenv in the Node MVP).
// Real environment variables always take precedence over .env values.
Env.TraversePath().Load();

var builder = WebApplication.CreateBuilder(args);

// Surface .env / process environment variables through IConfiguration so
// services can read VERIFY_TOKEN, WHATSAPP_TOKEN and PHONE_NUMBER_ID.
builder.Configuration.AddEnvironmentVariables();

// Bind to the same port the Node MVP used (default 3000) so the existing
// ngrok + Meta webhook setup keeps working unchanged.
var port = builder.Configuration["PORT"] ?? "3000";
builder.WebHost.UseUrls($"http://0.0.0.0:{port}");

builder.Services.AddControllers();
builder.Services.AddHttpClient<IWhatsAppService, WhatsAppService>();

var app = builder.Build();

// Basic health check
app.MapGet("/", () => Results.Content("WhatsApp Meta Bot is running...", "text/plain"));

// DEBUG endpoint (VERY useful for Meta testing)
app.MapPost("/debug", async (HttpContext context) =>
{
    var logger = context.RequestServices.GetRequiredService<ILoggerFactory>()
        .CreateLogger("Debug");

    using var reader = new StreamReader(context.Request.Body);
    var raw = await reader.ReadToEndAsync();

    logger.LogInformation("📦 DEBUG PAYLOAD:");

    if (!string.IsNullOrWhiteSpace(raw))
    {
        try
        {
            using var doc = JsonDocument.Parse(raw);
            logger.LogInformation("{Payload}",
                JsonSerializer.Serialize(doc, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch (JsonException)
        {
            logger.LogInformation("{Payload}", raw);
        }
    }

    return Results.Ok();
});

// Webhook routes (GET verify + POST messages)
app.MapControllers();

var startupLogger = app.Services.GetRequiredService<ILogger<Program>>();
startupLogger.LogInformation("🚀 WhatsApp bot running on port {Port}", port);

app.Run();
