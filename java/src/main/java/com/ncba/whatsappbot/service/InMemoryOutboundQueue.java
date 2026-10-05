package com.ncba.whatsappbot.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ncba.whatsappbot.model.OutboundJob;
import org.springframework.stereotype.Component;

/**
 * Bounded in-memory implementation of {@link OutboundQueue}. Equivalent to the
 * .NET bounded {@code System.Threading.Channels} channel (capacity 1000): a
 * full queue causes {@link #tryEnqueue} to return false so the caller can drop
 * and log instead of blocking the request thread.
 */
@Component
public class InMemoryOutboundQueue implements OutboundQueue {

    private final BlockingQueue<OutboundJob> queue = new LinkedBlockingQueue<>(1000);

    @Override
    public boolean tryEnqueue(OutboundJob job) {
        return queue.offer(job);
    }

    @Override
    public OutboundJob take() throws InterruptedException {
        return queue.take();
    }
}
