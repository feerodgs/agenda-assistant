package dev.feliperodrigue.botagendamento.scheduler;

import dev.feliperodrigue.botagendamento.domain.AppointmentResult;
import dev.feliperodrigue.botagendamento.notification.TelegramNotifier;
import dev.feliperodrigue.botagendamento.checker.AppointmentChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

public class AppointmentCheckJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCheckJob.class);

    private final AppointmentChecker checker;
    private final TelegramNotifier notifier;

    public AppointmentCheckJob(AppointmentChecker checker, TelegramNotifier notifier) {
        this.checker = checker;
        this.notifier = notifier;
    }

    @Scheduled(
            fixedDelayString = "${bot.scheduler.interval-ms:300000}",
            initialDelayString = "${bot.scheduler.initial-delay-ms:10000}"
    )
    public void run() {
        log.info("========================================");
        log.info("[Job] Iniciando verificação de agendamento — {}", LocalDateTime.now());

        AppointmentResult result;
        try {
            result = checker.check();
        } catch (Exception e) {
            log.error("[Job] Erro inesperado ao executar o checker", e);
            notifier.notifyError("Erro inesperado no checker: " + e.getMessage());
            return;
        }

        if (result.hasError()) {
            log.warn("[Job] Checker retornou erro: {}", result.errorMessage());
            notifier.notifyError(result.errorMessage());
        } else if (result.available()) {
            log.info("[Job] Vagas encontradas! Enviando notificação. Slots: {}", result.slots());
            notifier.notifyAvailability(result);
        } else {
            log.info("[Job] Nenhuma vaga disponível no momento.");
        }

        log.info("[Job] Verificação concluída — {}", LocalDateTime.now());
        log.info("========================================");
    }
}
