package dev.feliperodrigue.botagendamento.telegram.handler;

import com.google.api.services.calendar.model.Event;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.google.GoogleCalendarService;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class ProximosHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ProximosHandler.class);
    private static final int MAX_EVENTOS = 5;

    private final UserConnectionRepository userConnectionRepository;
    private final GoogleCalendarService calendarService;

    public ProximosHandler(UserConnectionRepository userConnectionRepository,
                           GoogleCalendarService calendarService) {
        this.userConnectionRepository = userConnectionRepository;
        this.calendarService = calendarService;
    }

    @Override
    public List<BotResponse> handle(long chatId, Update update) {
        UserConnection conn = userConnectionRepository.findById(chatId).orElse(null);
        if (conn == null) {
            return BotResponse.text("Voce ainda nao vinculou sua conta Google.\nUse /vincular para conectar.").asList();
        }

        try {
            List<Event> eventos = calendarService.listarProximosEventos(conn, MAX_EVENTOS);
            String cabecalho = "Proximos " + MAX_EVENTOS + " compromissos:";
            return BotResponse.text(calendarService.formatarListaEventos(eventos, cabecalho)).asList();
        } catch (Exception e) {
            log.error("[ProximosHandler] Erro ao buscar eventos — chatId={}", chatId, e);
            return BotResponse.text("Erro ao buscar agenda. Tente novamente.").asList();
        }
    }
}
