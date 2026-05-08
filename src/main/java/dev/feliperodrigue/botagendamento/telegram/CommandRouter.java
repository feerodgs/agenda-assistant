package dev.feliperodrigue.botagendamento.telegram;

import dev.feliperodrigue.botagendamento.conversation.ConversationStateService;
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
    private final NovoHandler novoHandler;
    private final ConversationStateService conversationStateService;

    public CommandRouter(
            StartHandler startHandler,
            VincularHandler vincularHandler,
            NovoHandler novoHandler,
            HojeHandler hojeHandler,
            AmanhaHandler amanhaHandler,
            ProximosHandler proximosHandler,
            ConversationStateService conversationStateService) {

        this.novoHandler = novoHandler;
        this.conversationStateService = conversationStateService;
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
                    "Comando nao reconhecido. Comandos disponiveis:\n" +
                    "/vincular — conectar Google Calendar\n" +
                    "/novo     — criar compromisso\n" +
                    "/hoje     — agenda de hoje\n" +
                    "/amanha   — agenda de amanha\n" +
                    "/proximos — proximos compromissos"
            ).asList();
        }

        if (conversationStateService.hasActiveConversation(chatId)) {
            return novoHandler.handleConversationStep(chatId, text);
        }

        return BotResponse.text("Nao entendi. Use /start para ver os comandos disponiveis.").asList();
    }

    private String extractCommand(String text) {
        return text.split("[@\\s]")[0].toLowerCase();
    }
}
