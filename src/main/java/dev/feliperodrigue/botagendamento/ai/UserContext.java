package dev.feliperodrigue.botagendamento.ai;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Contexto do usuário passado ao modelo para resolver referências relativas
 * de data/hora ("amanhã", "semana que vem", "sábado de manhã" etc.).
 */
public record UserContext(
        String displayName,
        ZoneId timeZone,
        boolean usesGoogleCalendar,
        LocalDate today
) {

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    public static UserContext of(String displayName, boolean usesGoogleCalendar) {
        return new UserContext(
                displayName,
                ZONE_SP,
                usesGoogleCalendar,
                LocalDate.now(ZONE_SP)
        );
    }
}
