package dev.feliperodrigue.botagendamento.scheduling;

import com.google.api.services.calendar.model.Event;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.google.GoogleCalendarService;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private final GoogleCalendarService calendarService;
    private final UserConnectionRepository userConnectionRepository;

    public AgendamentoService(GoogleCalendarService calendarService,
                              UserConnectionRepository userConnectionRepository) {
        this.calendarService = calendarService;
        this.userConnectionRepository = userConnectionRepository;
    }

    public record Resultado(boolean sucesso, boolean conflito, String mensagem, Event evento) {

        static Resultado ok(Event evento, String mensagem) {
            return new Resultado(true, false, mensagem, evento);
        }

        static Resultado conflito(String mensagem) {
            return new Resultado(false, true, mensagem, null);
        }

        static Resultado erro(String mensagem) {
            return new Resultado(false, false, mensagem, null);
        }
    }

    public Resultado criarAgendamento(Long chatId, String dateStr, String timeStr, String descricao) {
        return criarAgendamento(chatId, dateStr, timeStr, descricao, false);
    }

    public Resultado criarAgendamento(Long chatId, String dateStr, String timeStr, String descricao,
                                      boolean ignorarConflito) {

        UserConnection conn = userConnectionRepository.findById(chatId).orElse(null);
        if (conn == null) {
            return Resultado.erro("Conta Google nao vinculada. Use /vincular.");
        }

        LocalDate date;
        LocalTime time;
        try {
            date = parseDate(dateStr);
            time = parseTime(timeStr);
        } catch (IllegalArgumentException e) {
            return Resultado.erro(e.getMessage());
        }

        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusHours(1);

        try {
            if (!ignorarConflito && calendarService.temConflito(conn, start, end)) {
                log.info("[Agendamento] Conflito detectado — chatId={} horario={}", chatId, start);
                return Resultado.conflito(
                        "Ja existe um compromisso entre " +
                                start.format(DateTimeFormatter.ofPattern("HH:mm")) + " e " +
                                end.format(DateTimeFormatter.ofPattern("HH:mm")) + ".");
            }

            Event evento = calendarService.criarEvento(conn, descricao, start);
            String confirmacao = calendarService.formatarConfirmacaoCriacao(evento);
            return Resultado.ok(evento, confirmacao);

        } catch (Exception e) {
            log.error("[Agendamento] Erro ao criar evento — chatId={}", chatId, e);
            return Resultado.erro("Erro ao criar o compromisso no Google Calendar. Tente novamente.");
        }
    }

    public LocalDate parseDate(String input) {
        String s = input.trim().toLowerCase();

        if (s.equals("hoje")) return LocalDate.now();
        if (s.equals("amanha") || s.equals("amanhã")) return LocalDate.now().plusDays(1);

        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(s + "/" + LocalDate.now().getYear(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Data invalida: '" + input + "'. Use: hoje, amanha, ou dd/MM/yyyy");
        }
    }

    public LocalTime parseTime(String input) {
        String s = input.trim().toLowerCase();

        // "14h30" → "14:30", "14h" → "14:00"
        if (s.contains("h")) {
            String[] parts = s.split("h", 2);
            String horas = parts[0].trim();
            String minutos = (parts.length > 1 && !parts[1].isBlank()) ? parts[1].trim() : "00";
            if (minutos.length() == 1) minutos = minutos + "0"; // "9h3" → "9:30"
            s = horas + ":" + minutos;
        }

        // "14" → "14:00"
        if (!s.contains(":")) s = s + ":00";

        // "14:5" → "14:05"
        String[] parts = s.split(":", 2);
        if (parts.length == 2 && parts[1].length() == 1) {
            s = parts[0] + ":0" + parts[1];
        }

        try {
            return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Horario invalido: '" + input + "'. Use formatos como: 14:30, 14h30, 14h");
        }
    }
}
