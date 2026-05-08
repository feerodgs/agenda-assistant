package dev.feliperodrigue.botagendamento.google;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import dev.feliperodrigue.botagendamento.config.GoogleProperties;
import dev.feliperodrigue.botagendamento.domain.UserConnection;
import dev.feliperodrigue.botagendamento.repository.UserConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            CalendarScopes.CALENDAR_EVENTS, "email", "openid"
    );

    private final GoogleProperties props;
    private final UserConnectionRepository repo;

    public GoogleAuthService(GoogleProperties props, UserConnectionRepository repo) {
        this.props = props;
        this.repo = repo;
    }

    /** Gera a URL OAuth para o usuário autorizar o acesso ao Google Calendar. */
    public String generateAuthUrl(Long chatId) {
        try {
            return buildFlow()
                    .newAuthorizationUrl()
                    .setRedirectUri(props.redirectUri())
                    .setState(String.valueOf(chatId))
                    .set("prompt", "consent")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Falha ao gerar URL de autorização Google", e);
        }
    }

    /** Processa o callback OAuth: troca o code por tokens e salva a conexão. */
    public UserConnection processCallback(String code, Long chatId)
            throws GeneralSecurityException, IOException {

        GoogleTokenResponse tokenResponse = buildFlow()
                .newTokenRequest(code)
                .setRedirectUri(props.redirectUri())
                .execute();

        String email = extractEmail(tokenResponse);

        UserConnection conn = repo.findById(chatId).orElse(new UserConnection());
        conn.setChatId(chatId);
        conn.setGoogleEmail(email);
        conn.setAccessToken(tokenResponse.getAccessToken());
        if (tokenResponse.getRefreshToken() != null) {
            conn.setRefreshToken(tokenResponse.getRefreshToken());
        }
        long expiresIn = tokenResponse.getExpiresInSeconds() != null
                ? tokenResponse.getExpiresInSeconds() : 3600L;
        conn.setTokenExpiry(Instant.now().plusSeconds(expiresIn));

        UserConnection saved = repo.save(conn);
        log.info("[OAuth] Conta vinculada — chatId={} email={}", chatId, email);
        return saved;
    }

    /**
     * Constrói um cliente da Google Calendar API autenticado com os tokens
     * do usuário. Renova o access token automaticamente se estiver expirado.
     */
    public Calendar buildCalendarService(UserConnection conn)
            throws GeneralSecurityException, IOException {

        refreshTokenIfNeeded(conn);

        AccessToken accessToken = new AccessToken(
                conn.getAccessToken(),
                Date.from(conn.getTokenExpiry())
        );

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(props.clientId())
                .setClientSecret(props.clientSecret())
                .setRefreshToken(conn.getRefreshToken())
                .setAccessToken(accessToken)
                .build();

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new com.google.auth.http.HttpCredentialsAdapter(credentials))
                .setApplicationName("bot-agendamento")
                .build();
    }

    // ─── Privados ─────────────────────────────────────────────────────────────

    /**
     * Renova o access token se expirar nos próximos 5 minutos.
     * O refresh token é permanente (até o usuário revogar) e pode ser usado
     * quantas vezes for necessário.
     */
    private void refreshTokenIfNeeded(UserConnection conn)
            throws GeneralSecurityException, IOException {

        boolean expirado = conn.getTokenExpiry() == null
                || conn.getTokenExpiry().isBefore(Instant.now().plusSeconds(300));

        if (!expirado) return;

        log.info("[OAuth] Renovando access token — chatId={}", conn.getChatId());

        GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                conn.getRefreshToken(),
                props.clientId(),
                props.clientSecret()
        ).execute();

        conn.setAccessToken(response.getAccessToken());
        long expiresIn = response.getExpiresInSeconds() != null
                ? response.getExpiresInSeconds() : 3600L;
        conn.setTokenExpiry(Instant.now().plusSeconds(expiresIn));
        repo.save(conn);
    }

    private String extractEmail(GoogleTokenResponse tokenResponse) throws IOException {
        GoogleIdToken idToken = tokenResponse.parseIdToken();
        if (idToken == null) {
            throw new IllegalStateException("Google não retornou ID Token.");
        }
        String email = idToken.getPayload().getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("E-mail não presente no ID Token.");
        }
        return email;
    }

    private GoogleAuthorizationCodeFlow buildFlow() throws GeneralSecurityException, IOException {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setWeb(new GoogleClientSecrets.Details()
                        .setClientId(props.clientId())
                        .setClientSecret(props.clientSecret()));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                SCOPES)
                .setAccessType("offline")
                .build();
    }
}
