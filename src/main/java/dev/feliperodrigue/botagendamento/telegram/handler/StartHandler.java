package dev.feliperodrigue.botagendamento.telegram.handler;

import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class StartHandler implements CommandHandler {

    @Override
    public List<BotResponse> handle(long chatId, Update update) {
        String name = update.getMessage().getFrom().getFirstName();
        return BotResponse.text(
                "Ola, " + name + "! Sou seu assistente de agenda pessoal.\n\n" +
                "Para comecar, vincule sua conta Google:\n" +
                "/vincular — conectar Google Calendar\n\n" +
                "Depois disso voce podera usar:\n" +
                "/novo      — criar compromisso\n" +
                "/hoje      — agenda de hoje\n" +
                "/amanha    — agenda de amanha\n" +
                "/proximos  — proximos compromissos"
        ).asList();
    }
}
