package dev.feliperodrigue.botagendamento.conversation;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStateService {

    private final ConcurrentHashMap<Long, ConversationState> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, OnboardingStep> onboardingStates = new ConcurrentHashMap<>();

    // ── /novo flow ────────────────────────────────────────────────────────────

    public void start(Long chatId) {
        states.put(chatId, new ConversationState());
    }

    public Optional<ConversationState> get(Long chatId) {
        return Optional.ofNullable(states.get(chatId));
    }

    public boolean hasActiveConversation(Long chatId) {
        return states.containsKey(chatId);
    }

    // ── onboarding flow ───────────────────────────────────────────────────────

    public void startOnboarding(Long chatId) {
        onboardingStates.put(chatId, OnboardingStep.WAITING_FOR_NAME);
    }

    public void advanceToGoogleChoice(Long chatId) {
        onboardingStates.put(chatId, OnboardingStep.WAITING_FOR_GOOGLE_CHOICE);
    }

    public boolean isWaitingForName(Long chatId) {
        return onboardingStates.get(chatId) == OnboardingStep.WAITING_FOR_NAME;
    }

    public boolean isWaitingForGoogleChoice(Long chatId) {
        return onboardingStates.get(chatId) == OnboardingStep.WAITING_FOR_GOOGLE_CHOICE;
    }

    // ── shared ────────────────────────────────────────────────────────────────

    public void clear(Long chatId) {
        states.remove(chatId);
        onboardingStates.remove(chatId);
    }
}
