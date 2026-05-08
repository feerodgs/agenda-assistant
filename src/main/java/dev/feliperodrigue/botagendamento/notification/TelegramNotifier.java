package dev.feliperodrigue.botagendamento.notification;

import dev.feliperodrigue.botagendamento.config.TelegramProperties;
import dev.feliperodrigue.botagendamento.domain.AppointmentResult;
import dev.feliperodrigue.botagendamento.telegram.AgendaBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.StringJoiner;

@Service
public class TelegramNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final AgendaBot bot;
    private final String chatId;

    public TelegramNotifier(AgendaBot bot, TelegramProperties properties) {
        this.bot = bot;
        this.chatId = properties.chatId();
    }

    public void notifyAvailability(AppointmentResult result) {
        String message = buildAvailabilityMessage(result);
        send(message);
    }

    public void notifyError(String errorMessage) {
        String message = "Erro na verificacao de agendamento:\n" + errorMessage;
        send(message);
    }

    private String buildAvailabilityMessage(AppointmentResult result) {
        StringJoiner slots = new StringJoiner("\n  - ", "  - ", "");
        result.slots().forEach(slots::add);
        String msg = """
                Vaga(s) de agendamento disponivel(is)!
                Horarios encontrados:
                %s
                Acesse o site e agende agora!
                """;
        return msg.formatted(slots);
    }

    private void send(String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            bot.execute(message);
            log.info("[TelegramNotifier] Mensagem enviada para chat {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[TelegramNotifier] Falha ao enviar mensagem no Telegram", e);
        }
    }
}
