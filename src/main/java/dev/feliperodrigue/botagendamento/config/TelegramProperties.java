package dev.feliperodrigue.botagendamento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(Bot bot, String chatId) {

    public record Bot(String token, String username) {}
}
