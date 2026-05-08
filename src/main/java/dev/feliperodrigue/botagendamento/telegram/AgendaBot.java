package dev.feliperodrigue.botagendamento.telegram;

import dev.feliperodrigue.botagendamento.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class AgendaBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AgendaBot.class);

    private final String username;
    private final CommandRouter commandRouter;

    public AgendaBot(TelegramProperties properties, CommandRouter commandRouter) {
        super(properties.bot().token());
        this.username = properties.bot().username();
        this.commandRouter = commandRouter;
        log.info("[AgendaBot] Bot inicializado: @{}", username);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        log.info("[AgendaBot] Mensagem recebida — chatId={} text={}", chatId, text);

        try {
            List<BotResponse> responses = commandRouter.route(chatId, text, update);
            for (BotResponse response : responses) {
                send(chatId, response);
            }
        } catch (Exception e) {
            log.error("[AgendaBot] Erro ao processar mensagem — chatId={}", chatId, e);
            sendText(chatId, "Ocorreu um erro interno. Tente novamente.");
        }
    }

    public void sendText(long chatId, String text) {
        try {
            execute(new SendMessage(String.valueOf(chatId), text));
        } catch (TelegramApiException e) {
            log.error("[AgendaBot] Falha ao enviar mensagem — chatId={}", chatId, e);
        }
    }

    private void send(long chatId, BotResponse response) {
        try {
            SendMessage message = new SendMessage(String.valueOf(chatId), response.text());
            if (response.keyboard() != null) {
                message.setReplyMarkup(response.keyboard());
            }
            execute(message);
        } catch (TelegramApiException e) {
            log.error("[AgendaBot] Falha ao enviar resposta — chatId={}", chatId, e);
        }
    }
}
