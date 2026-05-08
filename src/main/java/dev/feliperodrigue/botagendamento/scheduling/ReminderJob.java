package dev.feliperodrigue.botagendamento.scheduling;

import com.google.api.services.calendar.model.Event;
import dev.feliperodrigue.botagendamento.domain.SentReminder;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.google.GoogleCalendarService;
import dev.feliperodrigue.botagendamento.repository.SentReminderRepository;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import dev.feliperodrigue.botagendamento.telegram.AgendaBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private static final int JANELA_MINUTOS = 15;

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final UserConnectionRepository userConnectionRepository;
    private final GoogleCalendarService calendarService;
    private final SentReminderRepository sentReminderRepository;
    private final AgendaBot bot;

    public ReminderJob(UserConnectionRepository userConnectionRepository,
                       GoogleCalendarService calendarService,
                       SentReminderRepository sentReminderRepository,
                       AgendaBot bot) {
        this.userConnectionRepository = userConnectionRepository;
        this.calendarService = calendarService;
        this.sentReminderRepository = sentReminderRepository;
        this.bot = bot;
    }

    @Scheduled(fixedDelayString = "${reminder.interval-ms:300000}",
            initialDelayString = "${reminder.initial-delay-ms:60000}")
    public void executar() {
        List<UserConnection> usuarios = userConnectionRepository.findAll();
        if (usuarios.isEmpty()) return;

        log.debug("[ReminderJob] Verificando lembretes para {} usuario(s)", usuarios.size());

        for (UserConnection conn : usuarios) {
            try {
                processarUsuario(conn);
            } catch (Exception e) {
                log.error("[ReminderJob] Erro ao processar chatId={}", conn.getChatId(), e);
                // Continua para o próximo usuário — falha individual não para o job
            }
        }
    }

    private void processarUsuario(UserConnection conn) throws Exception {
        LocalDateTime agora = LocalDateTime.now(ZONE);
        LocalDateTime limite = agora.plusMinutes(JANELA_MINUTOS);

        List<Event> eventos = calendarService.listarProximosEventos(conn, 10);

        for (Event evento : eventos) {
            if (evento.getId() == null || evento.getStart() == null
                    || evento.getStart().getDateTime() == null) continue;

            LocalDateTime inicio = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(evento.getStart().getDateTime().getValue()),
                    ZONE);

            if (inicio.isBefore(agora) || inicio.isAfter(limite)) continue;

            if (sentReminderRepository.existsByChatIdAndEventId(conn.getChatId(), evento.getId())) continue;

            enviarLembrete(conn, evento, inicio);
            sentReminderRepository.save(new SentReminder(conn.getChatId(), evento.getId()));
        }
    }

    private void enviarLembrete(UserConnection conn, Event evento, LocalDateTime inicio) {
        String texto = "Lembrete: hoje as " + inicio.format(FMT) + " — " + evento.getSummary();
        bot.sendText(conn.getChatId(), texto);
        log.info("[ReminderJob] Lembrete enviado — chatId={} evento='{}'",
                conn.getChatId(), evento.getSummary());
    }
}
