package dev.feliperodrigue.botagendamento.telegram.handler;

import com.google.api.services.calendar.model.Event;
import dev.feliperodrigue.botagendamento.ai.AiInterpretation;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.google.GoogleCalendarService;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import dev.feliperodrigue.botagendamento.scheduling.AgendamentoService;
import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiFallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(AiFallbackHandler.class);

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String NAO_VINCULADO =
            "Você ainda não vinculou sua conta Google.\nUse /vincular para conectar.";

    private static final String FALLBACK_UNKNOWN =
            """
            Não entendi sua mensagem. Tente algo como:
            • "Marca reunião amanhã às 10h"
            • "Quais meus compromissos de hoje?"
            • "Cancela minha consulta de sexta"
            Ou use /novo para o fluxo guiado.""";

    private final AgendamentoService agendamentoService;
    private final GoogleCalendarService calendarService;
    private final UserConnectionRepository userConnectionRepository;

    public AiFallbackHandler(AgendamentoService agendamentoService,
                              GoogleCalendarService calendarService,
                              UserConnectionRepository userConnectionRepository) {
        this.agendamentoService = agendamentoService;
        this.calendarService = calendarService;
        this.userConnectionRepository = userConnectionRepository;
    }

    public List<BotResponse> handle(long chatId, AiInterpretation ai) {
        log.debug("[AiFallback] chatId={} intent={}", chatId, ai.intent());

        return switch (ai.intent()) {
            case CREATE_APPOINTMENT      -> handleCreate(chatId, ai);
            case LIST_APPOINTMENTS       -> handleList(chatId, ai);
            case RESCHEDULE_APPOINTMENT  -> handleReschedule(chatId, ai);
            case CANCEL_APPOINTMENT      -> handleCancel(chatId, ai);
            case HELP, SMALL_TALK        -> handleConversational(ai);
            case UNKNOWN                 -> handleUnknown(ai);
        };
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    private List<BotResponse> handleCreate(long chatId, AiInterpretation ai) {
        if (ai.date() == null || ai.time() == null || ai.description() == null) {
            return BotResponse.text(
                    "Não consegui identificar todas as informações necessárias.\n" +
                    "Tente ser mais específico, por exemplo:\n" +
                    "\"Marca dentista amanhã às 14h\"\n\n" +
                    "Ou use /novo para o fluxo guiado."
            ).asList();
        }

        AgendamentoService.Resultado resultado = agendamentoService.criarAgendamento(
                chatId,
                ai.date().format(DATE_FMT),
                ai.time().format(TIME_FMT),
                ai.description()
        );

        return BotResponse.text(resultado.mensagem()).asList();
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    private List<BotResponse> handleList(long chatId, AiInterpretation ai) {
        UserConnection conn = userConnectionRepository.findById(chatId).orElse(null);
        if (conn == null) return BotResponse.text(NAO_VINCULADO).asList();

        try {
            LocalDate data = ai.date() != null ? ai.date() : LocalDate.now(ZONE);
            List<Event> eventos = calendarService.listarEventosDoDia(conn, data);

            if (ai.constraints() != null) {
                eventos = filtrarPorConstraints(eventos, ai.constraints());
            }

            String cabecalho = "Agenda de " + data.format(DATE_FMT) + ":";
            return BotResponse.text(calendarService.formatarListaEventos(eventos, cabecalho)).asList();

        } catch (Exception e) {
            log.error("[AiFallback] Erro ao listar — chatId={}", chatId, e);
            return BotResponse.text("Erro ao buscar agenda. Tente novamente.").asList();
        }
    }

    // ── RESCHEDULE / CANCEL (Opção B) ─────────────────────────────────────────
    // A IA extrai o contexto do evento existente; o bot lista os candidatos
    // e pede que o usuário confirme qual deseja alterar.

    private List<BotResponse> handleReschedule(long chatId, AiInterpretation ai) {
        return listarCandidatos(chatId, ai,
                "Qual desses compromissos você quer remarcar?\n" +
                "Me diga o horário exato ou use /novo para criar um novo horário.");
    }

    private List<BotResponse> handleCancel(long chatId, AiInterpretation ai) {
        return listarCandidatos(chatId, ai,
                "Qual desses compromissos você quer cancelar?\n" +
                "(Cancelamento via bot em breve — por enquanto acesse o Google Calendar.)");
    }

    private List<BotResponse> listarCandidatos(long chatId, AiInterpretation ai, String pergunta) {
        UserConnection conn = userConnectionRepository.findById(chatId).orElse(null);
        if (conn == null) return BotResponse.text(NAO_VINCULADO).asList();

        if (ai.originalDate() == null) {
            return BotResponse.text(
                    "Não consegui identificar a data do compromisso que você quer modificar.\n" +
                    "Informe a data, por exemplo:\n" +
                    "\"Cancela minha consulta do dia 20/05\""
            ).asList();
        }

        try {
            List<Event> candidatos = calendarService.listarEventosDoDia(conn, ai.originalDate());
            if (candidatos.isEmpty()) {
                return BotResponse.text(
                        "Não encontrei compromissos em " + ai.originalDate().format(DATE_FMT) + "."
                ).asList();
            }

            String lista = calendarService.formatarListaEventos(
                    candidatos,
                    "Compromissos em " + ai.originalDate().format(DATE_FMT) + ":"
            );
            return BotResponse.text(lista + "\n\n" + pergunta).asList();

        } catch (Exception e) {
            log.error("[AiFallback] Erro ao buscar candidatos — chatId={}", chatId, e);
            return BotResponse.text("Erro ao buscar agenda. Tente novamente.").asList();
        }
    }

    // ── HELP / SMALL_TALK / UNKNOWN ───────────────────────────────────────────

    private List<BotResponse> handleConversational(AiInterpretation ai) {
        String msg = ai.friendlyMessage() != null
                ? ai.friendlyMessage()
                : "Posso criar e listar seus compromissos no Google Calendar.\n" +
                  "Tente: \"Marca reunião amanhã às 10h\"";
        return BotResponse.text(msg).asList();
    }

    private List<BotResponse> handleUnknown(AiInterpretation ai) {
        String msg = ai.friendlyMessage() != null ? ai.friendlyMessage() : FALLBACK_UNKNOWN;
        return BotResponse.text(msg).asList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Event> filtrarPorConstraints(List<Event> eventos, AiInterpretation.Constraints c) {
        return eventos.stream()
                .filter(event -> {
                    if (event.getStart().getDateTime() == null) return true; // eventos de dia todo passam
                    LocalTime horario = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZONE
                    ).toLocalTime();
                    if (c.afterTime() != null && horario.isBefore(c.afterTime())) return false;
                    if (c.beforeTime() != null && horario.isAfter(c.beforeTime())) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }
}
