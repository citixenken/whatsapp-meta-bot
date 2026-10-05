package com.ncba.whatsappbot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Startup checks and the ready banner. Port of the fail-fast logic in the .NET
 * {@code Program.cs}: outside the Development profile an App Secret is required
 * so inbound webhook signatures are always verified. The check runs in
 * {@link PostConstruct} (before the web server starts accepting traffic) so the
 * app never boots into an insecure, forgeable-webhook state.
 */
@Component
public class StartupConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfig.class);

    private final WhatsAppProperties properties;
    private final Environment environment;
    private final String port;

    public StartupConfig(WhatsAppProperties properties,
                         Environment environment,
                         @Value("${server.port:3000}") String port) {
        this.properties = properties;
        this.environment = environment;
        this.port = port;
    }

    @PostConstruct
    void validateAppSecret() {
        if (StringUtils.hasText(properties.getAppSecret())) {
            return;
        }

        if (isDevelopment()) {
            log.warn("APP_SECRET is not configured; inbound webhook signature verification is "
                    + "DISABLED. This is only permitted in the 'dev' profile for local demos.");
            return;
        }

        throw new IllegalStateException(
                "APP_SECRET is required outside the 'dev' profile so inbound webhook signatures "
                        + "(X-Hub-Signature-256) are verified. Set APP_SECRET, or run with "
                        + "SPRING_PROFILES_ACTIVE=dev for local demos.");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("WhatsApp bot running on port {}", port);
    }

    private boolean isDevelopment() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("dev")) {
                return true;
            }
        }
        return false;
    }
}
