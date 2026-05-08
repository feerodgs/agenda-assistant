package dev.feliperodrigue.botagendamento.checker;

import dev.feliperodrigue.botagendamento.domain.AppointmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@ConditionalOnProperty(name = "bot.checker.mode", havingValue = "fake", matchIfMissing = true)
public class FakeAppointmentChecker implements AppointmentChecker {

    private static final Logger log = LoggerFactory.getLogger(FakeAppointmentChecker.class);
    private final Random random = new Random();

    @Override
    public AppointmentResult check() {
        log.info("[FakeChecker] Simulando verificação de agendamento...");

        boolean foundSlot = true;

        if (foundSlot) {
            List<String> slots = List.of("15/06/2025 às 10h00", "16/06/2025 às 14h30");
            log.info("[FakeChecker] Vagas SIMULADAS encontradas: {}", slots);
            return AppointmentResult.withSlots(slots);
        }

        log.info("[FakeChecker] Nenhuma vaga simulada disponível.");
        return AppointmentResult.noSlots();
    }
}
