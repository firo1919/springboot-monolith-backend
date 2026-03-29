package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;

public class AuthControllerE2ETest extends AbstractE2ETest {

    private static String registeredAdminEmail;

    private void registerAdmin(String suffix) {
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins").contentType(APPLICATION_JSON)
                .body(registerAdminPayload(suffix)).exchange();

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

        String loginPayload = "{\"password\":\"" + DEFAULT_PASSWORD + "\",\"email\":\"" + email + "\"}";

        var loginResponse = client.post().uri(AUTH_BASE_URL + "/login")
                .contentType(APPLICATION_JSON).body(loginPayload).exchange();
        loginResponse.expectStatus().isOk();
        String loginBody = loginResponse.returnResult(String.class).getResponseBody();

        String accessToken = extractJsonString(loginBody, "accessToken");
        String refreshToken = extractJsonString(loginBody, "refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        String refreshPayload = "{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}";
        var refreshResponse = client.post().uri(AUTH_BASE_URL + "/refresh")
                .contentType(APPLICATION_JSON).body(refreshPayload).exchange();
        refreshResponse.expectStatus().isOk();
        String refreshBody = refreshResponse.returnResult(String.class).getResponseBody();
        assertThat(extractJsonString(refreshBody, "accessToken")).isNotBlank();

        String logoutPayload = "{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}";
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
