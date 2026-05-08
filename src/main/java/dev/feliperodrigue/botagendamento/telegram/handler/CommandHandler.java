package dev.feliperodrigue.botagendamento.telegram.handler;

import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface CommandHandler {
    List<BotResponse> handle(long chatId, Update update);
}
