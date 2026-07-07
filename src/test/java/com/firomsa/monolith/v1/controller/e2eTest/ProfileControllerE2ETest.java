package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class ProfileControllerE2ETest extends AbstractE2ETest {

    private static final String PROFILE_BASE_URL = "/api/v1/profile";

    private String adminEmail;
    private String adminAccessToken;

    private void registerAndConfirmAdminIfNeeded() {
        if (adminAccessToken != null) {
            return;
        }

        String suffix = randomSuffix();
        adminEmail = adminEmailForSuffix(suffix);

        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        String otp = latestOtpForEmail(adminEmail);
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + adminEmail + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        adminAccessToken = loginByEmail(adminEmail);
    }

    private String registerAndLoginEmployee(String adminToken, String suffix) {
        var registerEmployeeResponse = client.post().uri(ADMIN_BASE_URL + "/employees")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminToken))
                .contentType(APPLICATION_JSON).body(registerEmployeePayload(suffix)).exchange();
        registerEmployeeResponse.expectStatus().isCreated();

        String employeeEmail = employeeEmailForSuffix(suffix);
        String otp = latestOtpForEmail(employeeEmail);
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + employeeEmail + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        return loginByEmail(employeeEmail);
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingToken() {
        var response = client.get().uri(PROFILE_BASE_URL).exchange();
        response.expectStatus().isUnauthorized();
    }

    @Test
    void shouldGetAdminProfile() {
        registerAndConfirmAdminIfNeeded();

        var response = client.get().uri(PROFILE_BASE_URL)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .exchange();

        response.expectStatus().isOk();
        String body = response.returnResult(String.class).getResponseBody();
        assertThat(body).contains(adminEmail);
    }

    @Test
    void shouldUpdateProfile() {
        registerAndConfirmAdminIfNeeded();
        String suffix = randomSuffix();
        String updatePayload = """
                {
                    "firstName": "UpdatedFirst%s",
                    "lastName": "UpdatedLast%s",
                    "username": "updated_%s",
                    "password": "password123",
                    "email": "updated.%s@example.com",
                    "phone": "+251933%s"
                }
                """.formatted(suffix, suffix, suffix, suffix, suffix.substring(0, 6));

        var response = client.put().uri(PROFILE_BASE_URL)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .contentType(APPLICATION_JSON).body(updatePayload).exchange();

        response.expectStatus().isOk();
        String body = response.returnResult(String.class).getResponseBody();
        assertThat(body).contains("updated." + suffix + "@example.com");
    }

    @Test
    void shouldReturnBadRequestWhenProfilePayloadInvalid() {
        registerAndConfirmAdminIfNeeded();
        String invalidPayload = """
                {
                    "firstName": "",
                    "lastName": "",
                    "username": "",
                    "password": "short",
                    "email": "bad-email",
                    "phone": ""
                }
                """;

        var response = client.put().uri(PROFILE_BASE_URL)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .contentType(APPLICATION_JSON).body(invalidPayload).exchange();

        response.expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnNotFoundWhenAddingMissingProfileImage() {
        registerAndConfirmAdminIfNeeded();
        String payload = "{\"objectKey\":\"missing-image-key\"}";

        var response = client.post().uri(PROFILE_BASE_URL + "/profile-picture")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .contentType(APPLICATION_JSON).body(payload).exchange();

        response.expectStatus().isNotFound();
    }

    @Test
    void shouldAllowEmployeeToReadOwnProfile() {
        registerAndConfirmAdminIfNeeded();
        String employeeToken = registerAndLoginEmployee(adminAccessToken, randomSuffix());

        var response = client.get().uri(PROFILE_BASE_URL)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(employeeToken)).exchange();

        response.expectStatus().isOk();
    }
}
