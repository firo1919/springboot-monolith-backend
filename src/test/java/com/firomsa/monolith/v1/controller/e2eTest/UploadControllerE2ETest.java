package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class UploadControllerE2ETest extends AbstractE2ETest {

    private static final String UPLOAD_BASE_URL = "/api/v1/uploads";

    private static String adminAccessToken;

    private void registerAndConfirmAdminIfNeeded() {
        if (adminAccessToken != null) {
            return;
        }

        String suffix = randomSuffix();
        String email = adminEmailForSuffix(suffix);

        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        String otp = latestOtpForEmail(email);
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + email + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        adminAccessToken = loginByEmail(email);
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingToken() {
        var response = client.post().uri(UPLOAD_BASE_URL + "/presign").contentType(APPLICATION_JSON)
                .body("{\"filename\":\"f.png\",\"contentType\":\"image/png\"}").exchange();

        response.expectStatus().isUnauthorized();
    }

    @Test
    void shouldCreatePresignTicketForAdmin() {
        registerAndConfirmAdminIfNeeded();

        var response = client.post().uri(UPLOAD_BASE_URL + "/presign")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .contentType(APPLICATION_JSON)
                .body("{\"filename\":\"photo.png\",\"contentType\":\"image/png\"}").exchange();

        response.expectStatus().isOk();
        String body = response.returnResult(String.class).getResponseBody();
        assertThat(body).contains("objectKey");
        assertThat(body).contains("uploadUrl");
        assertThat(body).contains("expiresIn");
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() {
        registerAndConfirmAdminIfNeeded();

        var response = client.post().uri(UPLOAD_BASE_URL + "/presign")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(adminAccessToken))
                .contentType(APPLICATION_JSON).body("{\"filename\":\"\",\"contentType\":\"\"}")
                .exchange();

        response.expectStatus().isBadRequest();
    }
}
