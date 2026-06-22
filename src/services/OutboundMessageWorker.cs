namespace WhatsAppMetaBot.Services;

/// <summary>
/// Background worker that drains the outbound queue and sends messages via the
/// Meta Graph API. Replaces the MVP's fire-and-forget sends with observed,
/// gracefully-stoppable processing (REL-01, REL-07, CFG-05).
/// </summary>
public sealed class OutboundMessageWorker : BackgroundService
{
    private readonly IOutboundQueue _queue;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<OutboundMessageWorker> _logger;

    public OutboundMessageWorker(
        IOutboundQueue queue,
        IServiceScopeFactory scopeFactory,
        ILogger<OutboundMessageWorker> logger)
    {
        _queue = queue;
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await foreach (var job in _queue.DequeueAllAsync(stoppingToken))
        {
            try
            {
                // WhatsAppService is a typed HttpClient (transient); resolve it
                // per message from a fresh scope.
                using var scope = _scopeFactory.CreateScope();
                var sender = scope.ServiceProvider.GetRequiredService<IWhatsAppService>();
                await sender.SendWhatsAppMessageAsync(job.To, job.Body, stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to send outbound message");
            }
        }
    }
}
