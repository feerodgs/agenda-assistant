package dev.feliperodrigue.botagendamento.ai;

/**
 * Porta de entrada da camada de IA.
 * A implementação concreta pode ser trocada (OpenAI → Claude → Ollama)
 * sem alterar nenhuma outra classe do projeto.
 */
public interface AiInterpreterService {

    /**
     * Interpreta uma mensagem de texto livre e devolve a intenção estruturada.
     * Nunca lança exceção — em caso de falha retorna {@code Intent.UNKNOWN}.
     *
     * @param userMessage texto bruto enviado pelo usuário
     * @param context     contexto do usuário (data atual, fuso, preferências)
     * @return            interpretação estruturada, nunca {@code null}
     */
    AiInterpretation interpret(String userMessage, UserContext context);
}
