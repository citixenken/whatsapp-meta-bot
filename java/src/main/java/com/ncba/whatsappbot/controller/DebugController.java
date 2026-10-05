package com.ncba.whatsappbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only endpoint that logs raw webhook payloads, matching the .NET
 * {@code /debug} route. Registered only under the {@code dev} profile so raw
 * PII payloads are never logged in production.
 */
@RestController
@Profile("dev")
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger("Debug");

    @PostMapping("/debug")
    public ResponseEntity<Void> debug(@RequestBody(required = false) String raw) {
        if (StringUtils.hasText(raw)) {
            log.info("DEBUG payload: {}", raw);
        }
        return ResponseEntity.ok().build();
    }
}
