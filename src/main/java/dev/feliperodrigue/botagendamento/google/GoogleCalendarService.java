package dev.feliperodrigue.botagendamento.google;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String CALENDAR_ID = "primary";
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm");

    private final GoogleAuthService authService;

    public GoogleCalendarService(GoogleAuthService authService) {
        this.authService = authService;
    }

    public List<Event> listarEventosDoDia(UserConnection conn, LocalDate date)
            throws GeneralSecurityException, IOException {

        Calendar service = authService.buildCalendarService(conn);

        DateTime inicio = toDateTime(date.atStartOfDay());
        DateTime fim = toDateTime(date.atTime(LocalTime.MAX));

        Events events = service.events().list(CALENDAR_ID)
                .setTimeMin(inicio)
                .setTimeMax(fim)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Event> items = events.getItems();
        log.debug("[Calendar] {} evento(s) em {} para chatId={}", items.size(), date, conn.getChatId());
        return items;
    }

    public List<Event> listarProximosEventos(UserConnection conn, int maxResults)
            throws GeneralSecurityException, IOException {

        Calendar service = authService.buildCalendarService(conn);

        Events events = service.events().list(CALENDAR_ID)
                .setTimeMin(toDateTime(LocalDateTime.now(ZONE)))
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems();
    }

    public boolean temConflito(UserConnection conn, LocalDateTime start, LocalDateTime end)
            throws GeneralSecurityException, IOException {

        Calendar service = authService.buildCalendarService(conn);

        Events events = service.events().list(CALENDAR_ID)
                .setTimeMin(toDateTime(start))
                .setTimeMax(toDateTime(end))
                .setSingleEvents(true)
                .execute();

        List<Event> items = events.getItems();
        return items != null && !items.isEmpty();
    }

    public Event criarEvento(UserConnection conn, String titulo, LocalDateTime start)
            throws GeneralSecurityException, IOException {

        Calendar service = authService.buildCalendarService(conn);

        LocalDateTime end = start.plusHours(1);
        String tzId = ZONE.getId();

        Event event = new Event()
                .setSummary(titulo)
                .setStart(new EventDateTime().setDateTime(toDateTime(start)).setTimeZone(tzId))
                .setEnd(new EventDateTime().setDateTime(toDateTime(end)).setTimeZone(tzId));

        Event created = service.events().insert(CALENDAR_ID, event).execute();
        log.info("[Calendar] Evento criado: '{}' em {} — chatId={}", titulo, start.format(DATE_TIME_FMT), conn.getChatId());
        return created;
    }

    public String formatarListaEventos(List<Event> eventos, String cabecalho) {
        if (eventos == null || eventos.isEmpty()) {
            return cabecalho + "\n\nNenhum compromisso encontrado.";
        }

        StringBuilder sb = new StringBuilder(cabecalho).append("\n\n");
        for (Event event : eventos) {
            if (event.getStart().getDateTime() != null) {
                LocalDateTime dt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getStart().getDateTime().getValue()),
                        ZONE);
                sb.append(dt.format(TIME_FMT)).append(" — ");
            } else {
                sb.append("[dia todo] — ");
            }
            sb.append(event.getSummary()).append("\n");
        }
        return sb.toString().trim();
    }

    public String formatarConfirmacaoCriacao(Event event) {
        String titulo = event.getSummary();
        String horario = "";
        if (event.getStart().getDateTime() != null) {
            LocalDateTime dt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getStart().getDateTime().getValue()),
                    ZONE);
            horario = dt.format(DATE_TIME_FMT);
        }
        return "Compromisso criado no Google Calendar!\n\n" +
                titulo + "\n" + horario;
    }

    private DateTime toDateTime(LocalDateTime ldt) {
        return new DateTime(ldt.atZone(ZONE).toInstant().toEpochMilli());
    }
}
