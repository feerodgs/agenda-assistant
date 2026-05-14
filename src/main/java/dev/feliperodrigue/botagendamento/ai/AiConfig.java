package dev.feliperodrigue.botagendamento.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o modelo de linguagem usado pela camada de IA.
 *
 * Para trocar de provedor:
 *   - Claude  → use AnthropicChatModel (langchain4j-anthropic)
 *   - Ollama  → use OllamaChatModel    (langchain4j-ollama)
 * A interface ChatLanguageModel é a única dependência do restante do código.
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    /**
     * Este bean assume prioridade sobre a auto-configuração do starter
     * (que usa @ConditionalOnMissingBean), garantindo que responseFormat
     * e demais parâmetros sejam aplicados corretamente.
     */
    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model-name:gpt-4o-mini}") String modelName,
            @Value("${openai.max-tokens:400}") int maxTokens) {

        if (apiKey.isBlank()) {
            log.warn("[AI] OPENAI_API_KEY não configurada — chamadas ao modelo falharão.");
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey.isBlank() ? "no-key-configured" : apiKey)
                .modelName(modelName)
                .temperature(0.0)
                // Força resposta em JSON puro — elimina texto antes/depois do objeto.
                // Requisito OpenAI: o prompt DEVE mencionar "JSON" para que isso funcione.
                .responseFormat("json_object")
                .maxTokens(maxTokens)
                .build();
    }
}
