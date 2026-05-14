package dev.feliperodrigue.botagendamento.telegram;

import dev.feliperodrigue.botagendamento.ai.AiInterpretation;
import dev.feliperodrigue.botagendamento.ai.AiInterpreterService;
import dev.feliperodrigue.botagendamento.ai.UserContext;
import dev.feliperodrigue.botagendamento.conversation.ConversationStateService;
import dev.feliperodrigue.botagendamento.domain.UserProfile;
import dev.feliperodrigue.botagendamento.repository.UserProfileRepository;
import dev.feliperodrigue.botagendamento.telegram.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@Component
public class CommandRouter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final Map<String, CommandHandler> handlers;
    private final StartHandler startHandler;
    private final NovoHandler novoHandler;
    private final ConversationStateService conversationStateService;
    private final AiInterpreterService aiInterpreterService;
    private final AiFallbackHandler aiFallbackHandler;
    private final UserProfileRepository userProfileRepository;

    public CommandRouter(
            StartHandler startHandler,
            VincularHandler vincularHandler,
            NovoHandler novoHandler,
            HojeHandler hojeHandler,
            AmanhaHandler amanhaHandler,
            ProximosHandler proximosHandler,
            ConversationStateService conversationStateService,
            AiInterpreterService aiInterpreterService,
            AiFallbackHandler aiFallbackHandler,
            UserProfileRepository userProfileRepository) {

        this.startHandler = startHandler;
        this.novoHandler = novoHandler;
        this.conversationStateService = conversationStateService;
        this.aiInterpreterService = aiInterpreterService;
        this.aiFallbackHandler = aiFallbackHandler;
        this.userProfileRepository = userProfileRepository;
        this.handlers = Map.of(
                "/start", startHandler,
                "/vincular", vincularHandler,
                "/novo", novoHandler,
                "/hoje", hojeHandler,
                "/amanha", amanhaHandler,
                "/proximos", proximosHandler
        );
    }

    public List<BotResponse> route(long chatId, String text, Update update) {
        if (text.startsWith("/")) {
            String command = extractCommand(text);
            log.debug("[Router] Comando: {} — chatId={}", command, chatId);

            if (!"/novo".equals(command)) {
                conversationStateService.clear(chatId);
            }

            CommandHandler handler = handlers.get(command);
            if (handler != null) {
                return handler.handle(chatId, update);
            }

            return BotResponse.text(
                    "Comando não reconhecido. Comandos disponíveis:\n" +
                    "/vincular — conectar Google Calendar\n" +
                    "/novo     — criar compromisso\n" +
                    "/hoje     — agenda de hoje\n" +
                    "/amanha   — agenda de amanhã\n" +
                    "/proximos — próximos compromissos"
            ).asList();
        }

        if (conversationStateService.isWaitingForName(chatId)) {
            return startHandler.handleNameInput(chatId, text);
        }

        if (conversationStateService.isWaitingForGoogleChoice(chatId)) {
            return startHandler.handleGoogleChoiceInput(chatId, text);
        }

        if (conversationStateService.hasActiveConversation(chatId)) {
            return novoHandler.handleConversationStep(chatId, text);
        }

        // Mensagem livre → interpreta com IA e encaminha para o handler de intent
        return routeWithAi(chatId, text);
    }

    private List<BotResponse> routeWithAi(long chatId, String text) {
        UserProfile profile = userProfileRepository.findById(chatId).orElse(null);
        String nome = profile != null ? profile.getDisplayName() : "usuário";
        boolean usaGoogle = profile != null && profile.isUseGoogleCalendar();

        UserContext context = UserContext.of(nome, usaGoogle);
        AiInterpretation interpretation = aiInterpreterService.interpret(text, context);

        log.debug("[Router] IA — chatId={} intent={}", chatId, interpretation.intent());
        return aiFallbackHandler.handle(chatId, interpretation);
    }

    private String extractCommand(String text) {
        return text.split("[@\\s]")[0].toLowerCase();
    }
}
