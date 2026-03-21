package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.RefreshTokenRepository;
import com.firomsa.monolith.repository.UserRepository;

public class AuthControllerE2ETest extends AbstractE2ETest {

    private static final String AUTH_BASE_URL = "/api/v1/auth";
    private static final String BOOTSTRAP_TOKEN = "test-bootstrap-token-12345678901234";
    private static final String DEFAULT_PASSWORD = "password123";
    private static String registeredAdminEmail;

    private RestTestClient client;

    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUpClient() {
        this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAllInBatch();
        confirmationOtpRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String adminEmailForSuffix(String suffix) {
        return "admin." + suffix + "@example.com";
    }

    private String registerAdminPayload(String suffix, String bootstrapToken) {
        return """
                {
                    "firstName": "Admin",
                    "lastName": "User",
                    "username": "admin_%s",
                    "password": "%s",
                    "email": "admin.%s@example.com",
                    "phone": "+251900%s",
                    "bootstrapToken": "%s"
                }
                """.formatted(suffix, DEFAULT_PASSWORD, suffix, suffix.substring(0, 6),
                bootstrapToken);
    }

    private String latestOtpForEmail(String email) {
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        return confirmationOtpRepository.findAll().stream()
                .filter(otp -> otp.getUser() != null && userId.equals(otp.getUser().getId()))
                .map(otp -> otp.getOtp()).reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No OTP found for user " + email));
    }

    private String extractJsonString(String body, String field) {
        String marker = "\"" + field + "\":\"";
        int start = body.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int from = start + marker.length();
        int end = body.indexOf('"', from);
        assertThat(end).isGreaterThan(from);
        return body.substring(from, end);
    }

    private void registerAdmin(String suffix) {
        var registerResponse =
                client.post().uri(AUTH_BASE_URL + "/admins").contentType(APPLICATION_JSON)
                        .body(registerAdminPayload(suffix, BOOTSTRAP_TOKEN)).exchange();

        registerResponse.expectStatus().isOk();
        String registerBody = registerResponse.returnResult(String.class).getResponseBody();
        assertThat(registerBody)
                .contains("You have successfully registered, confirm the OTP sent to your email");
        registeredAdminEmail = adminEmailForSuffix(suffix);
    }

    private void confirmOtp(String email, String otp) {
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + email + "\"}";

        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();

        confirmResponse.expectStatus().isOk();
        String confirmBody = confirmResponse.returnResult(String.class).getResponseBody();
        assertThat(confirmBody)
                .contains("Successfully confirmed OTP, please login using your email and password");
    }

    private String ensureRegisteredAndConfirmedAdmin() {
        if (registeredAdminEmail != null) {
            return registeredAdminEmail;
        }

        String suffix = randomSuffix();
        String email = adminEmailForSuffix(suffix);
        registerAdmin(suffix);
        confirmOtp(email, latestOtpForEmail(email));
        return email;
    }

    @Test
    void shouldRegisterConfirmLoginRefreshAndLogoutAdmin() {
        String email = ensureRegisteredAndConfirmedAdmin();

        String loginPayload =
                "{\"password\":\"" + DEFAULT_PASSWORD + "\",\"email\":\"" + email + "\"}";

        var loginResponse = client.post().uri(AUTH_BASE_URL + "/login")
                .contentType(APPLICATION_JSON).body(loginPayload).exchange();
        loginResponse.expectStatus().isOk();
        String loginBody = loginResponse.returnResult(String.class).getResponseBody();

        String accessToken = extractJsonString(loginBody, "accessToken");
        String refreshToken = extractJsonString(loginBody, "refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        String refreshPayload =
                "{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}";
        var refreshResponse = client.post().uri(AUTH_BASE_URL + "/refresh")
                .contentType(APPLICATION_JSON).body(refreshPayload).exchange();
        refreshResponse.expectStatus().isOk();
        String refreshBody = refreshResponse.returnResult(String.class).getResponseBody();
        assertThat(extractJsonString(refreshBody, "accessToken")).isNotBlank();

        String logoutPayload =
                "{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}";
        var logoutResponse = client.post().uri(AUTH_BASE_URL + "/logout")
                .contentType(APPLICATION_JSON).body(logoutPayload).exchange();
        logoutResponse.expectStatus().isOk();
        String logoutBody = logoutResponse.returnResult(String.class).getResponseBody();
        assertThat(logoutBody).contains("Successfully logged out");
    }

    @Test
    void shouldReturnBadRequestWhenBootstrapTokenIsWrong() {
        String suffix = randomSuffix();

        var response = client.post().uri(AUTH_BASE_URL + "/admins").contentType(APPLICATION_JSON)
                .body(registerAdminPayload(suffix, "invalid-bootstrap-token-12345678901234"))
                .exchange();

        response.expectStatus().isBadRequest();
        String body = response.returnResult(String.class).getResponseBody();
        assertThat(body).contains("Authentication failed");
    }

    @Test
    void shouldReturnBadRequestWhenConfirmingWrongOtp() {
        String email = ensureRegisteredAndConfirmedAdmin();

        String payload = "{\"otp\":\"00000\",\"email\":\"" + email + "\"}";
        var response = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(payload).exchange();

        response.expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnBadRequestWhenLoginPayloadIsInvalid() {
        String payload = "{\"password\":\"short\",\"email\":\"bad-email\"}";

        var response = client.post().uri(AUTH_BASE_URL + "/login").contentType(APPLICATION_JSON)
                .body(payload).exchange();

        response.expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnForbiddenWhenPasswordIsWrong() {
        String email = ensureRegisteredAndConfirmedAdmin();

        String wrongPasswordPayload = "{\"password\":\"wrongpass123\",\"email\":\"" + email + "\"}";
        var response = client.post().uri(AUTH_BASE_URL + "/login").contentType(APPLICATION_JSON)
                .body(wrongPasswordPayload).exchange();

        response.expectStatus().isForbidden();
    }
}
