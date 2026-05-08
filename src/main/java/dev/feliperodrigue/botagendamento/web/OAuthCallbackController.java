package dev.feliperodrigue.botagendamento.web;

import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.google.GoogleAuthService;
import dev.feliperodrigue.botagendamento.telegram.AgendaBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RestController
@RequestMapping("/oauth/google")
public class OAuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OAuthCallbackController.class);

    private final GoogleAuthService googleAuthService;
    private final AgendaBot bot;

    public OAuthCallbackController(GoogleAuthService googleAuthService, AgendaBot bot) {
        this.googleAuthService = googleAuthService;
        this.bot = bot;
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state) {

        Long chatId;
        try {
            chatId = Long.parseLong(state);
        } catch (NumberFormatException e) {
            log.error("[OAuth] Parâmetro 'state' inválido: {}", state);
            return ResponseEntity.badRequest().body(errorPage("Requisição inválida."));
        }

        try {
            UserConnection conn = googleAuthService.processCallback(code, chatId);

            notifyUser(chatId,
                    "Conta Google conectada com sucesso!\n" +
                    "E-mail: " + conn.getGoogleEmail() + "\n\n" +
                    "Agora você pode usar:\n" +
                    "/novo — criar um compromisso\n" +
                    "/hoje — ver agenda de hoje\n" +
                    "/amanha — ver agenda de amanhã\n" +
                    "/proximos — próximos compromissos");

            log.info("[OAuth] Vinculação concluída — chatId={} email={}", chatId, conn.getGoogleEmail());
            return ResponseEntity.ok(successPage(conn.getGoogleEmail()));

        } catch (Exception e) {
            log.error("[OAuth] Falha ao processar callback — chatId={}", chatId, e);
            notifyUser(chatId,
                    "Não foi possível conectar sua conta Google. Tente novamente com /vincular.");
            return ResponseEntity.internalServerError().body(errorPage(
                    "Erro ao conectar conta Google. Feche esta aba e tente novamente no bot."));
        }
    }

    private void notifyUser(Long chatId, String text) {
        try {
            bot.execute(new SendMessage(String.valueOf(chatId), text));
        } catch (TelegramApiException e) {
            log.warn("[OAuth] Não foi possível notificar usuário {} no Telegram após OAuth", chatId, e);
        }
    }

    private String successPage(String email) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><title>Conta conectada</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:60px">
                  <h2>Conta Google conectada!</h2>
                  <p>E-mail: <strong>%s</strong></p>
                  <p>Pode fechar esta aba e voltar ao Telegram.</p>
                </body>
                </html>
                """.formatted(email);
    }

    private String errorPage(String message) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><title>Erro</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:60px">
                  <h2>Algo deu errado</h2>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(message);
    }
}
