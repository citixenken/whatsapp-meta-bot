package com.ncba.whatsappbot.service;

import com.ncba.whatsappbot.model.OutboundJob;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Background worker that drains the outbound queue and sends messages via the
 * Meta Graph API. Port of the .NET {@code OutboundMessageWorker}
 * ({@code BackgroundService}): replaces fire-and-forget sends with observed,
 * gracefully-stoppable processing on a dedicated daemon thread.
 */
@Component
public class OutboundMessageWorker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OutboundMessageWorker.class);

    private final OutboundQueue queue;
    private final WhatsAppService whatsAppService;

    private volatile boolean running = true;
    private Thread worker;

    public OutboundMessageWorker(OutboundQueue queue, WhatsAppService whatsAppService) {
        this.queue = queue;
        this.whatsAppService = whatsAppService;
    }

    @Override
    public void run(ApplicationArguments args) {
        worker = new Thread(this::loop, "outbound-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void loop() {
        while (running) {
            try {
                OutboundJob job = queue.take();
                whatsAppService.sendWhatsAppMessage(job.to(), job.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to send outbound message", e);
            }
        }
    }

    /** Graceful shutdown: stop the loop and interrupt the blocking take. */
    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }
}
