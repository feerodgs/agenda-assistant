package dev.feliperodrigue.botagendamento.telegram.handler;

import dev.feliperodrigue.botagendamento.conversation.ConversationState;
import dev.feliperodrigue.botagendamento.conversation.ConversationState.Step;
import dev.feliperodrigue.botagendamento.conversation.ConversationStateService;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import dev.feliperodrigue.botagendamento.scheduling.AgendamentoService;
import dev.feliperodrigue.botagendamento.telegram.BotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

@Component
public class NovoHandler implements CommandHandler {

    private final ConversationStateService conversationStateService;
    private final UserConnectionRepository userConnectionRepository;
    private final AgendamentoService agendamentoService;

    public NovoHandler(ConversationStateService conversationStateService,
                       UserConnectionRepository userConnectionRepository,
                       AgendamentoService agendamentoService) {
        this.conversationStateService = conversationStateService;
        this.userConnectionRepository = userConnectionRepository;
        this.agendamentoService = agendamentoService;
    }

    @Override
    public List<BotResponse> handle(long chatId, Update update) {
        if (!userConnectionRepository.existsById(chatId)) {
            return BotResponse.text(
                    "Voce ainda nao vinculou sua conta Google.\nUse /vincular primeiro."
            ).asList();
        }
        conversationStateService.start(chatId);
        return BotResponse.text("Qual a data do compromisso?\n(ex.: hoje, amanha, 10/05/2026)").asList();
    }

    public List<BotResponse> handleConversationStep(long chatId, String text) {
        Optional<ConversationState> stateOpt = conversationStateService.get(chatId);
        if (stateOpt.isEmpty()) return List.of();

        ConversationState state = stateOpt.get();

        return switch (state.getStep()) {

            case WAITING_DATE -> {
                state.setDate(text);
                state.setStep(Step.WAITING_TIME);
                yield BotResponse.text("Qual o horario? (ex.: 14:30, 14h30, 14h)").asList();
            }

            case WAITING_TIME -> {
                state.setTime(text);
                state.setStep(Step.WAITING_DESCRIPTION);
                yield BotResponse.text("Qual a descricao do compromisso?").asList();
            }

            case WAITING_DESCRIPTION -> {
                state.setDescription(text);
                AgendamentoService.Resultado resultado = agendamentoService.criarAgendamento(
                        chatId, state.getDate(), state.getTime(), state.getDescription());

                if (resultado.conflito()) {
                    // Mantém o estado para confirmação
                    state.setStep(Step.WAITING_CONFLICT_CONFIRMATION);
                    yield BotResponse.text(
                            "Atencao: " + resultado.mensagem() + "\n\n" +
                            "Deseja criar o compromisso mesmo assim?\n" +
                            "Responda sim ou nao."
                    ).asList();
                }

                conversationStateService.clear(chatId);
                yield BotResponse.text(resultado.mensagem()).asList();
            }

            case WAITING_CONFLICT_CONFIRMATION -> {
                String resposta = text.trim().toLowerCase();
                if (resposta.equals("sim") || resposta.equals("s")) {
                    AgendamentoService.Resultado resultado = agendamentoService.criarAgendamento(
                            chatId, state.getDate(), state.getTime(), state.getDescription(), true);
                    conversationStateService.clear(chatId);
                    yield BotResponse.text(resultado.mensagem()).asList();
                } else {
                    conversationStateService.clear(chatId);
                    yield BotResponse.text("Compromisso cancelado.").asList();
                }
            }
        };
    }
}
