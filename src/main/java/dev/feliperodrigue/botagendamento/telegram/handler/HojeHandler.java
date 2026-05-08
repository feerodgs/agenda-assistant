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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class HojeHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(HojeHandler.class);

    private final UserConnectionRepository userConnectionRepository;
    private final GoogleCalendarService calendarService;

    public HojeHandler(UserConnectionRepository userConnectionRepository,
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
            LocalDate hoje = LocalDate.now();
            List<Event> eventos = calendarService.listarEventosDoDia(conn, hoje);
            String cabecalho = "Agenda de hoje (" + hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "):";
            return BotResponse.text(calendarService.formatarListaEventos(eventos, cabecalho)).asList();
        } catch (Exception e) {
            log.error("[HojeHandler] Erro ao buscar eventos — chatId={}", chatId, e);
            return BotResponse.text("Erro ao buscar agenda. Tente novamente.").asList();
        }
    }
}
