using WhatsAppMetaBot.Models;

namespace WhatsAppMetaBot.Services;

/// <summary>
/// In-process outbound message queue. Lets the webhook acknowledge Meta
/// immediately while sends happen on a background worker (REL-01). This is an
/// MVP-scoped substitute for Kafka/Redis: state is in-memory and therefore
/// single-instance only (lost on restart).
/// </summary>
public interface IOutboundQueue
{
    /// <summary>Tries to enqueue a job. Returns false if the queue is full.</summary>
    bool TryEnqueue(OutboundJob job);

    /// <summary>Streams queued jobs until the token is cancelled.</summary>
    IAsyncEnumerable<OutboundJob> DequeueAllAsync(CancellationToken cancellationToken);
}
