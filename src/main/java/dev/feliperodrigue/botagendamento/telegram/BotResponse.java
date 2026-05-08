package dev.feliperodrigue.botagendamento.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

public record BotResponse(String text, InlineKeyboardMarkup keyboard) {

    public static BotResponse text(String text) {
        return new BotResponse(text, null);
    }

    public static BotResponse withKeyboard(String text, InlineKeyboardMarkup keyboard) {
        return new BotResponse(text, keyboard);
    }

    public List<BotResponse> asList() {
        return List.of(this);
    }
}
