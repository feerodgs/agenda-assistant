package dev.feliperodrigue.botagendamento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.oauth")
public record GoogleProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {}
