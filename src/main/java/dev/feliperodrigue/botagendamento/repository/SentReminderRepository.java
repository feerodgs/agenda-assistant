package dev.feliperodrigue.botagendamento.repository;

import dev.feliperodrigue.botagendamento.domain.SentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentReminderRepository extends JpaRepository<SentReminder, Long> {
    boolean existsByChatIdAndEventId(Long chatId, String eventId);
}
