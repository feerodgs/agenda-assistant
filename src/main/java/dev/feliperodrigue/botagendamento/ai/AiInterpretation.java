package dev.feliperodrigue.botagendamento.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Resultado estruturado produzido pela IA a partir de uma mensagem livre.
 * Campos são null quando não se aplicam à intent detectada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiInterpretation(

        Intent intent,

        // CREATE_APPOINTMENT / RESCHEDULE_APPOINTMENT — novo slot
        LocalDate date,
        LocalTime time,
        Integer durationMinutes,
        String description,

        // LIST_APPOINTMENTS — filtros opcionais de horário
        Constraints constraints,

        // RESCHEDULE_APPOINTMENT / CANCEL_APPOINTMENT — compromisso existente
        LocalDate originalDate,
        LocalTime originalTime,

        // HELP / SMALL_TALK / UNKNOWN — resposta amigável gerada pelo modelo
        String friendlyMessage

) {

    public enum Intent {
        CREATE_APPOINTMENT,
        LIST_APPOINTMENTS,
        RESCHEDULE_APPOINTMENT,
        CANCEL_APPOINTMENT,
        HELP,
        SMALL_TALK,
        UNKNOWN
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Constraints(
            LocalTime afterTime,
            LocalTime beforeTime
    ) {}

    public static AiInterpretation unknown(String message) {
        return new AiInterpretation(
                Intent.UNKNOWN, null, null, null, null, null, null, null, message
        );
    }
}
