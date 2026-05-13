package dev.feliperodrigue.botagendamento.repository;

import dev.feliperodrigue.botagendamento.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
