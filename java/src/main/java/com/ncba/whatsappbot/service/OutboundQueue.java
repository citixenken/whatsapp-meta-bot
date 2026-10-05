package com.ncba.whatsappbot.service;

import com.ncba.whatsappbot.model.OutboundJob;

/**
 * In-process outbound message queue. Lets the webhook acknowledge Meta
 * immediately while sends happen on a background worker. MVP-scoped substitute
 * for Kafka/Redis: state is in-memory and therefore single-instance only
 * (lost on restart). Port of the .NET {@code IOutboundQueue}.
 */
public interface OutboundQueue {

    /** Tries to enqueue a job. Returns false if the queue is full. */
    boolean tryEnqueue(OutboundJob job);

    /** Blocks until a job is available and returns it. */
    OutboundJob take() throws InterruptedException;
}
