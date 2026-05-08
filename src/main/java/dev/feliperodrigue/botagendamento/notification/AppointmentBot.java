package dev.feliperodrigue.botagendamento.notification;

import dev.feliperodrigue.botagendamento.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AppointmentBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBot.class);

    private final String username;

    public AppointmentBot(TelegramProperties properties) {
        // O token é passado ao super — a lib usa internamente para autenticar
        super(properties.bot().token());
        this.username = properties.bot().username();
        log.info("[AppointmentBot] Bot inicializado: @{}", username);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
    }
}
