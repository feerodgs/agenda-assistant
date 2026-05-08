package dev.feliperodrigue.botagendamento.telegram.handler;

import dev.feliperodrigue.botagendamento.google.GoogleAuthService;
import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class VincularHandler implements CommandHandler {

    private final GoogleAuthService googleAuthService;

    public VincularHandler(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @Override
    public List<BotResponse> handle(long chatId, Update update) {
        String url = googleAuthService.generateAuthUrl(chatId);

        InlineKeyboardButton button = new InlineKeyboardButton("Conectar conta Google");
        button.setUrl(url);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(button)));

        return BotResponse.withKeyboard(
                "Clique no botao abaixo para autorizar o acesso ao seu Google Calendar.\n\n" +
                "Apos autorizar, voce recebera uma confirmacao aqui no chat.",
                keyboard
        ).asList();
    }
}
