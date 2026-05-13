package dev.feliperodrigue.botagendamento.telegram.handler;

import dev.feliperodrigue.botagendamento.conversation.ConversationStateService;
import dev.feliperodrigue.botagendamento.domain.UserProfile;
import dev.feliperodrigue.botagendamento.repository.UserProfileRepository;
import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class StartHandler implements CommandHandler {

    private static final String COMMANDS_BLOCK =
            "Conta configurada! Veja o que você pode fazer:\n\n" +
            "/novo      — criar um novo compromisso\n" +
            "/hoje      — ver os compromissos de hoje\n" +
            "/amanha    — ver os compromissos de amanhã\n" +
            "/proximos  — ver os próximos compromissos";

    private final ConversationStateService conversationStateService;
    private final UserProfileRepository userProfileRepository;

    public StartHandler(ConversationStateService conversationStateService,
                        UserProfileRepository userProfileRepository) {
        this.conversationStateService = conversationStateService;
        this.userProfileRepository = userProfileRepository;
    }

    // Etapa 1: /start → pergunta o nome
    @Override
    public List<BotResponse> handle(long chatId, Update update) {
        conversationStateService.startOnboarding(chatId);
        return BotResponse.text(
                "Olá! Eu sou seu assistente de agenda pessoal. 🙂\n" +
                "Como você gostaria de ser chamado?"
        ).asList();
    }

    // Etapa 2: recebe o nome → salva e pergunta sobre Google Calendar
    public List<BotResponse> handleNameInput(long chatId, String name) {
        String displayName = name.trim();

        UserProfile profile = userProfileRepository.findById(chatId)
                .orElseGet(UserProfile::new);
        profile.setChatId(chatId);
        profile.setDisplayName(displayName);
        userProfileRepository.save(profile);

        conversationStateService.advanceToGoogleChoice(chatId);

        return BotResponse.text(
                "Prazer, " + displayName + "! 🙂\n\n" +
                "Você deseja vincular sua conta Google para salvar seus agendamentos no Google Agenda?\n\n" +
                "Responda sim ou não."
        ).asList();
    }

    // Etapa 3: recebe sim/não → salva preferência e envia bloco de comandos
    public List<BotResponse> handleGoogleChoiceInput(long chatId, String text) {
        String input =  normalize(text);

        if (input.equals("sim") || input.equals("s")) {
            saveGoogleCalendarChoice(chatId, true);
            conversationStateService.clear(chatId);
            return List.of(
                    BotResponse.text(
                            "Perfeito! Para vincular sua conta, use o comando:\n/vincular"
                    ),
                    BotResponse.text(COMMANDS_BLOCK)
            );
        }

        if (input.equals("nao") || input.equals("n") || input.equals("não")) {
            saveGoogleCalendarChoice(chatId, false);
            conversationStateService.clear(chatId);
            return List.of(
                    BotResponse.text("Sem problemas! Vou salvar seus agendamentos localmente."),
                    BotResponse.text(COMMANDS_BLOCK)
            );
        }

        return BotResponse.text(
                "Não entendi. Por favor, responda sim ou não."
        ).asList();
    }

    private void saveGoogleCalendarChoice(long chatId, boolean useGoogle) {
        UserProfile profile = userProfileRepository.findById(chatId)
                .orElseGet(UserProfile::new);
        profile.setChatId(chatId);
        profile.setUseGoogleCalendar(useGoogle);
        userProfileRepository.save(profile);
    }

    private String normalize(String text) {
        return text.trim().toLowerCase()
                .replace("ã", "a")
                .replace("á", "a")
                .replace("â", "a")
                .replace("à", "a");
    }
}
