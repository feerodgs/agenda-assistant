package dev.feliperodrigue.botagendamento.repository;

import dev.feliperodrigue.botagendamento.domain.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {
    // chatId é a PK → use findById(chatId) para buscar um usuário
    // findAll() retorna todos os usuários vinculados (usado pelo ReminderJob)
}
