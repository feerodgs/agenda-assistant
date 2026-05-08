package dev.feliperodrigue.botagendamento.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "sent_reminder",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sent_reminder_chat_event",
        columnNames = {"chat_id", "event_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SentReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public SentReminder(Long chatId, String eventId) {
        this.chatId = chatId;
        this.eventId = eventId;
        this.sentAt = Instant.now();
    }
}
