package com.ncba.whatsappbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Application entry point. Java/Spring Boot port of the .NET {@code Program.cs}
 * bootstrap. {@link ConfigurationPropertiesScan} registers the strongly-typed,
 * validated configuration beans (equivalent to the .NET {@code IOptions} setup).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class WhatsAppMetaBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsAppMetaBotApplication.class, args);
    }
}
