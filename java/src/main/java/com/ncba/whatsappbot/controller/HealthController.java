package com.ncba.whatsappbot.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root banner plus liveness/readiness probes. Equivalent to the .NET
 * {@code MapGet("/")}, {@code MapHealthChecks("/healthz")} and
 * {@code MapHealthChecks("/readyz")} endpoints.
 */
@RestController
public class HealthController {

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String root() {
        return "WhatsApp Meta Bot is running...";
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("Healthy");
    }

    @GetMapping("/readyz")
    public ResponseEntity<String> readyz() {
        return ResponseEntity.ok("Healthy");
    }
}
