package dev.feliperodrigue.botagendamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "use_google_calendar", nullable = false)
    private boolean useGoogleCalendar = false;
}
