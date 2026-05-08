package dev.feliperodrigue.botagendamento.conversation;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStateService {

    private final ConcurrentHashMap<Long, ConversationState> states = new ConcurrentHashMap<>();

    public void start(Long chatId) {
        states.put(chatId, new ConversationState());
    }

    public Optional<ConversationState> get(Long chatId) {
        return Optional.ofNullable(states.get(chatId));
    }

    public boolean hasActiveConversation(Long chatId) {
        return states.containsKey(chatId);
    }

    public void clear(Long chatId) {
        states.remove(chatId);
    }
}
