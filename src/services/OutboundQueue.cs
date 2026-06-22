using System.Threading.Channels;
using WhatsAppMetaBot.Models;

namespace WhatsAppMetaBot.Services;

/// <inheritdoc />
public sealed class OutboundQueue : IOutboundQueue
{
    private readonly Channel<OutboundJob> _channel =
        Channel.CreateBounded<OutboundJob>(new BoundedChannelOptions(1000)
        {
            FullMode = BoundedChannelFullMode.Wait,
            SingleReader = true,
            SingleWriter = false
        });

    public bool TryEnqueue(OutboundJob job) => _channel.Writer.TryWrite(job);

    public IAsyncEnumerable<OutboundJob> DequeueAllAsync(CancellationToken cancellationToken)
        => _channel.Reader.ReadAllAsync(cancellationToken);
}
