package com.firomsa.monolith.v1.controller.integrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.RefreshTokenRepository;
import com.firomsa.monolith.repository.UserRepository;

public class AuthControllerIntTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/auth";

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        confirmationOtpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String registerAdminJson(String suffix) {
        return """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "username": "john_%s",
                    "password": "password123",
                    "email": "john.%s@example.com",
                    "phone": "+251900%s",
                    "bootstrapToken": "test-bootstrap-token-12345678901234"
                }
                """.formatted(suffix, suffix, suffix.substring(0, 6));
    }

    private void registerAdmin(String suffix) {
        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).content(registerAdminJson(suffix)).exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyText()
                .contains("You have successfully registered, confirm the OTP sent to your email");
    }

    private String emailForSuffix(String suffix) {
        return "john." + suffix + "@example.com";
    }

    private String latestOtpForEmail(String email) {
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        return confirmationOtpRepository.findAll().stream()
                .filter(otp -> otp.getUser() != null && userId.equals(otp.getUser().getId()))
                .map(otp -> otp.getOtp()).reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No OTP found for user " + email));
    }

    private void confirmOtp(String email, String otp) {
        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\",\"email\":\"" + email + "\"}").exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyText()
                .contains("Successfully confirmed OTP, please login using your email and password");
    }

    @Test
    void shouldRegisterAdminWhenRequestIsValid() {
        String suffix = randomSuffix();

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).content(registerAdminJson(suffix)).exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyText()
                .contains("You have successfully registered, confirm the OTP sent to your email");
        assertThat(response).bodyText().contains(emailForSuffix(suffix));
    }

    @Test
    void shouldConfirmOtpWhenOtpIsValid() {
        String suffix = randomSuffix();
        String email = emailForSuffix(suffix);

        registerAdmin(suffix);
        String otp = latestOtpForEmail(email);

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\",\"email\":\"" + email + "\"}").exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyText()
                .contains("Successfully confirmed OTP, please login using your email and password");
    }

    @Test
    void shouldLoginRefreshAndLogoutWhenFlowIsValid() {
        String suffix = randomSuffix();
        String email = emailForSuffix(suffix);

        registerAdmin(suffix);
        confirmOtp(email, latestOtpForEmail(email));

        MvcTestResult loginResponse = mockMvc.post().uri(BASE_URL + "/login")
                .contentType(APPLICATION_JSON)
                .content("{\"password\":\"password123\",\"email\":\"" + email + "\"}").exchange();

        assertThat(loginResponse).hasStatusOk();
        String refreshToken =
                refreshTokenRepository.findAll().stream().findFirst().orElseThrow().getToken();
        assertThat(refreshToken).isNotBlank();
        assertThat(loginResponse).bodyJson().extractingPath("$.accessToken").asString()
                .isNotBlank();

        MvcTestResult refreshResponse = mockMvc.post().uri(BASE_URL + "/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}")
                .exchange();

        assertThat(refreshResponse).hasStatusOk();
        assertThat(refreshResponse).bodyJson().extractingPath("$.accessToken").asString()
                .isNotBlank();
        assertThat(refreshResponse).bodyJson().extractingPath("$.refreshToken").asString()
                .isEqualTo(refreshToken);

        MvcTestResult logoutResponse = mockMvc.post().uri(BASE_URL + "/logout")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\",\"email\":\"" + email + "\"}")
                .exchange();

        assertThat(logoutResponse).hasStatusOk();
        assertThat(logoutResponse).bodyText().contains("Successfully logged out");
    }

    @Test
    void shouldReturnBadRequestWhenBootstrapTokenIsInvalid() {
        String suffix = randomSuffix();

        MvcTestResult response =
                mockMvc.post().uri(BASE_URL + "/admins").contentType(APPLICATION_JSON).content("""
                        {
                            "firstName": "John",
                            "lastName": "Doe",
                            "username": "john_%s",
                            "password": "password123",
                            "email": "john.%s@example.com",
                            "phone": "+251900%s",
                            "bootstrapToken": "wrong-bootstrap-token-123456789012"
                        }
                        """.formatted(suffix, suffix, suffix.substring(0, 6))).exchange();

        assertThat(response).hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenConfirmOtpPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/confirm-otp").contentType(APPLICATION_JSON)
                .content("{\"otp\":\"\",\"email\":\"\"}").exchange()).hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenConfirmOtpCodeIsInvalid() {
        String suffix = randomSuffix();
        String email = emailForSuffix(suffix);
        registerAdmin(suffix);

        assertThat(mockMvc.post().uri(BASE_URL + "/confirm-otp").contentType(APPLICATION_JSON)
                .content("{\"otp\":\"00000\",\"email\":\"" + email + "\"}").exchange())
                        .hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenResendOtpPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/resend-otp").contentType(APPLICATION_JSON)
                .content("{\"email\":\"invalid-email\"}").exchange()).hasStatus(400);
    }

    @Test
    void shouldResendOtpWhenEmailExists() {
        String suffix = randomSuffix();
        String email = emailForSuffix(suffix);
        registerAdmin(suffix);

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/resend-otp")
                .contentType(APPLICATION_JSON).content("{\"email\":\"" + email + "\"}").exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyText().contains("Successfully resent OTP, check your inbox");
    }

    @Test
    void shouldReturnNotFoundWhenResendOtpEmailDoesNotExist() {
        assertThat(mockMvc.post().uri(BASE_URL + "/resend-otp").contentType(APPLICATION_JSON)
                .content("{\"email\":\"unknown.user@example.com\"}").exchange()).hasStatus(404);
    }

    @Test
    void shouldReturnBadRequestWhenSecondAdminRegistrationAttempted() {
        String suffixA = randomSuffix();
        String suffixB = randomSuffix();
        registerAdmin(suffixA);

        MvcTestResult response = mockMvc.post().uri(BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).content(registerAdminJson(suffixB)).exchange();

        assertThat(response).hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenLoginPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/login").contentType(APPLICATION_JSON)
                .content("{\"password\":\"short\",\"email\":\"invalid-email\"}").exchange())
                        .hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenRefreshPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/refresh").contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":null,\"email\":\"invalid-email\"}").exchange())
                        .hasStatus(400);
    }

    @Test
    void shouldReturnBadRequestWhenLogoutPayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/logout").contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":null,\"email\":\"invalid-email\"}").exchange())
                        .hasStatus(400);
    }
}
