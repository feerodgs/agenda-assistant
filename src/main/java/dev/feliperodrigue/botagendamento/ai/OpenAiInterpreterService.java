package dev.feliperodrigue.botagendamento.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class OpenAiInterpreterService implements AiInterpreterService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiInterpreterService.class);

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public OpenAiInterpreterService(ChatLanguageModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiInterpretation interpret(String userMessage, UserContext context) {
        String systemPrompt = buildSystemPrompt(context);
        try {
            String json = chatModel
                    .generate(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from(userMessage))
                    .content()
                    .text();

            log.debug("[AI] input='{}' output='{}'", userMessage, json);
            return objectMapper.readValue(json, AiInterpretation.class);

        } catch (Exception e) {
            log.error("[AI] Falha ao interpretar mensagem: '{}'", userMessage, e);
            return AiInterpretation.unknown(null);
        }
    }

    // -------------------------------------------------------------------------
    // Prompt interno — edite este método para ajustar o comportamento da IA.
    // Decisão de idioma: prompt em português para que o modelo lide melhor com
    // expressões como "amanhã de manhã", "terça à tarde", "semana que vem".
    // -------------------------------------------------------------------------
    private String buildSystemPrompt(UserContext context) {
        LocalDate hoje = context.today();
        LocalDate amanha = hoje.plusDays(1);

        return """
                Você é o assistente de agenda do bot "Agenda Assistant".
                Hoje é %s (fuso horário: %s).

                Sua única tarefa é analisar a mensagem do usuário e devolver um JSON \
                com a intenção de agenda detectada.

                REGRAS OBRIGATÓRIAS:
                - Responda SOMENTE com o JSON. Sem texto antes ou depois.
                - O único campo obrigatório é "intent".
                - Datas no formato ISO: "yyyy-MM-dd". Horários: "HH:mm".
                - Resolva referências relativas usando a data de hoje acima \
                  ("amanhã", "semana que vem", "segunda-feira" etc.).
                - Expressões de período do dia: \
                  "manhã" = 09:00, "tarde" = 14:00, "noite" = 19:00.
                - Se não conseguir identificar a intenção, use "UNKNOWN".

                INTENTS DISPONÍVEIS:
                - CREATE_APPOINTMENT       → criar/agendar/marcar um compromisso novo
                - LIST_APPOINTMENTS        → ver/consultar/listar a agenda
                - RESCHEDULE_APPOINTMENT   → remarcar/mudar um compromisso existente
                - CANCEL_APPOINTMENT       → cancelar/apagar um compromisso existente
                - HELP                     → perguntas sobre o que o bot faz
                - SMALL_TALK               → conversa genérica sem relação com agenda
                - UNKNOWN                  → intenção não identificada

                SCHEMA DO JSON (todos os campos são opcionais exceto "intent"):
                {
                  "intent": "string",
                  "date": "yyyy-MM-dd  — data do novo compromisso ou filtro de listagem",
                  "time": "HH:mm       — horário do novo compromisso",
                  "durationMinutes": número — duração estimada (omita se não mencionado),
                  "description": "string   — título ou descrição do compromisso",
                  "constraints": {
                    "afterTime":  "HH:mm  — listagem: somente após este horário",
                    "beforeTime": "HH:mm  — listagem: somente antes deste horário"
                  },
                  "originalDate": "yyyy-MM-dd — RESCHEDULE/CANCEL: data do evento EXISTENTE",
                  "originalTime": "HH:mm      — RESCHEDULE/CANCEL: horário do evento EXISTENTE",
                  "friendlyMessage": "string  — HELP/SMALL_TALK/UNKNOWN: resposta amigável em português"
                }

                EXEMPLOS:

                Mensagem: "Marca um treino de perna amanhã às 19h"
                JSON: {"intent":"CREATE_APPOINTMENT","date":"%s","time":"19:00","description":"Treino de perna"}

                Mensagem: "Agendar corte de cabelo sábado de manhã"
                JSON: {"intent":"CREATE_APPOINTMENT","date":"<próximo sábado>","time":"09:00","description":"Corte de cabelo"}

                Mensagem: "Quais meus compromissos de hoje depois das 15h?"
                JSON: {"intent":"LIST_APPOINTMENTS","date":"%s","constraints":{"afterTime":"15:00"}}

                Mensagem: "Remarca minha consulta de terça pra quinta de tarde"
                JSON: {"intent":"RESCHEDULE_APPOINTMENT","description":"consulta","originalDate":"<próxima terça>","date":"<próxima quinta>","time":"14:00"}

                Mensagem: "Cancela meu dentista de sexta"
                JSON: {"intent":"CANCEL_APPOINTMENT","description":"dentista","originalDate":"<próxima sexta>"}

                Mensagem: "O que você faz?"
                JSON: {"intent":"HELP","friendlyMessage":"Posso criar, listar e gerenciar seus compromissos no Google Calendar! Tente: \\"Marca reunião amanhã às 10h\\""}

                Mensagem: "Tudo bem?"
                JSON: {"intent":"SMALL_TALK","friendlyMessage":"Tudo ótimo! Posso ajudar com sua agenda. Que tal agendar algo?"}
                """.formatted(hoje, context.timeZone().getId(), amanha, hoje);
    }
}
