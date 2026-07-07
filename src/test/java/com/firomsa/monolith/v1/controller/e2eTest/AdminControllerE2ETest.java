package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class AdminControllerE2ETest extends AbstractE2ETest {

    private String registeredAdminEmail;
    private String registeredAdminAccessToken;

    private UUID randomId() {
        return UUID.randomUUID();
    }

    private String updateEmployeePayload(String suffix) {
        return """
                {
                    "firstName": "UpdatedFirst%s",
                    "lastName": "UpdatedLast%s",
                    "username": "updated_%s",
                    "password": "%s",
                    "email": "updated.employee_%s@example.com",
                    "role": "EMPLOYEE",
                    "phone": "+251922%s"
                }
                """.formatted(suffix, suffix, suffix, DEFAULT_PASSWORD, suffix,
                suffix.substring(0, 6));
    }

    private String updatedEmployeeEmailForSuffix(String suffix) {
        return "updated.employee_" + suffix + "@example.com";
    }

    private void registerAndConfirmAdmin(String suffix) {
        String adminEmail = adminEmailForSuffix(suffix);
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();

        registerResponse.expectStatus().isOk();
        String registerBody = registerResponse.returnResult(String.class).getResponseBody();
        assertThat(registerBody)
                .contains("You have successfully registered, confirm the OTP sent to your email");

        String otp = latestOtpForEmail(adminEmail);
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + adminEmail + "\"}";

        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();

        confirmResponse.expectStatus().isOk();
        String confirmBody = confirmResponse.returnResult(String.class).getResponseBody();
        assertThat(confirmBody)
                .contains("Successfully confirmed OTP, please login using your email and password");
        registeredAdminEmail = adminEmail;
        registeredAdminAccessToken = loginByEmail(adminEmail);
    }

    private void confirmOtpForEmail(String email) {
        String otp = latestOtpForEmail(email);
        String confirmPayload = "{\"otp\":\"" + otp + "\",\"email\":\"" + email + "\"}";

        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(APPLICATION_JSON).body(confirmPayload).exchange();

        confirmResponse.expectStatus().isOk();
        String confirmBody = confirmResponse.returnResult(String.class).getResponseBody();
        assertThat(confirmBody)
                .contains("Successfully confirmed OTP, please login using your email and password");
    }

    private void registerEmployee(String accessToken, String suffix) {
        var employeeResponse = client.post().uri(ADMIN_BASE_URL + "/employees").contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken))
                .body(registerEmployeePayload(suffix)).exchange();

        employeeResponse.expectStatus().isCreated();
        String employeeBody = employeeResponse.returnResult(String.class).getResponseBody();
        assertThat(employeeBody).contains(employeeEmailForSuffix(suffix));
    }

    private UUID employeeIdByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private void withAuthenticatedAdmin(Consumer<String> testBody) {
        if (registeredAdminEmail == null) {
            registerAndConfirmAdmin(randomSuffix());
        }
        testBody.accept(registeredAdminAccessToken);
    }

    @Test
    void shouldReturnAllEmployees() {
        String employeeSuffixOne = randomSuffix();
        String employeeSuffixTwo = randomSuffix();

        withAuthenticatedAdmin(accessToken -> {
            registerEmployee(accessToken, employeeSuffixOne);
            registerEmployee(accessToken, employeeSuffixTwo);

            var response = client.get().uri(ADMIN_BASE_URL + "/employees")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();

            response.expectStatus().isOk();
            String responseBody = response.returnResult(String.class).getResponseBody();
            assertThat(responseBody).contains(employeeEmailForSuffix(employeeSuffixOne));
            assertThat(responseBody).contains(employeeEmailForSuffix(employeeSuffixTwo));
        });
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingTokenForAdminEndpoint() {
        var response = client.get().uri(ADMIN_BASE_URL + "/employees").exchange();

        response.expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesAdminEndpoint() {
        String employeeSuffix = randomSuffix();

        withAuthenticatedAdmin(adminAccessToken -> {
            registerEmployee(adminAccessToken, employeeSuffix);
            String employeeEmail = employeeEmailForSuffix(employeeSuffix);
            confirmOtpForEmail(employeeEmail);
            String employeeAccessToken = loginByEmail(employeeEmail);

            var response = client.get().uri(ADMIN_BASE_URL + "/employees")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(employeeAccessToken))
                    .exchange();

            response.expectStatus().isForbidden();
        });
    }

    @Test
    void shouldGetEmployeeById() {
        String employeeSuffix = randomSuffix();

        withAuthenticatedAdmin(accessToken -> {
            registerEmployee(accessToken, employeeSuffix);
            String email = employeeEmailForSuffix(employeeSuffix);
            UUID employeeId = employeeIdByEmail(email);

            var response = client.get().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();

            response.expectStatus().isOk();
            String body = response.returnResult(String.class).getResponseBody();
            assertThat(body).contains(email);
        });
    }

    @Test
    void shouldReturnNotFoundWhenEmployeeIdDoesNotExist() {
        withAuthenticatedAdmin(accessToken -> {
            var response = client.get().uri(ADMIN_BASE_URL + "/employees/" + randomId())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();

            response.expectStatus().isNotFound();
        });
    }

    @Test
    void shouldReturnBadRequestWhenRegisterEmployeePayloadIsInvalid() {
        withAuthenticatedAdmin(accessToken -> {
            String invalidPayload = """
                    {
                        "firstName": "",
                        "lastName": "",
                        "username": "",
                        "password": "short",
                        "email": "not-an-email",
                        "role": "EMPLOYEE",
                        "phone": ""
                    }
                    """;

            var response = client.post().uri(ADMIN_BASE_URL + "/employees")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken))
                    .contentType(APPLICATION_JSON).body(invalidPayload).exchange();

            response.expectStatus().isBadRequest();
        });
    }

    @Test
    void shouldUpdateEmployee() {
        String employeeSuffix = randomSuffix();
        String updateSuffix = randomSuffix();

        withAuthenticatedAdmin(accessToken -> {
            registerEmployee(accessToken, employeeSuffix);
            UUID employeeId = employeeIdByEmail(employeeEmailForSuffix(employeeSuffix));

            var response = client.put().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken))
                    .contentType(APPLICATION_JSON).body(updateEmployeePayload(updateSuffix))
                    .exchange();

            response.expectStatus().isOk();
            String body = response.returnResult(String.class).getResponseBody();
            assertThat(body).contains(updatedEmployeeEmailForSuffix(updateSuffix));
        });
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownEmployee() {
        withAuthenticatedAdmin(accessToken -> {
            var response = client.put().uri(ADMIN_BASE_URL + "/employees/" + randomId())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken))
                    .contentType(APPLICATION_JSON).body(updateEmployeePayload(randomSuffix()))
                    .exchange();

            response.expectStatus().isNotFound();
        });
    }

    @Test
    void shouldDeactivateAndActivateEmployee() {
        String employeeSuffix = randomSuffix();

        withAuthenticatedAdmin(accessToken -> {
            registerEmployee(accessToken, employeeSuffix);
            UUID employeeId = employeeIdByEmail(employeeEmailForSuffix(employeeSuffix));

            var deactivateResponse = client.post()
                    .uri(ADMIN_BASE_URL + "/employees/" + employeeId + "/deactivate")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            deactivateResponse.expectStatus().isOk();

            var getAfterDeactivate = client.get().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            getAfterDeactivate.expectStatus().isOk();
            String deactivatedBody = getAfterDeactivate.returnResult(String.class).getResponseBody();
            assertThat(deactivatedBody).contains("\"active\":false");

            var activateResponse = client.post()
                    .uri(ADMIN_BASE_URL + "/employees/" + employeeId + "/activate")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            activateResponse.expectStatus().isOk();

            var getAfterActivate = client.get().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            getAfterActivate.expectStatus().isOk();
            String activatedBody = getAfterActivate.returnResult(String.class).getResponseBody();
            assertThat(activatedBody).contains("\"active\":true");
        });
    }

    @Test
    void shouldDeleteEmployee() {
        String employeeSuffix = randomSuffix();

        withAuthenticatedAdmin(accessToken -> {
            registerEmployee(accessToken, employeeSuffix);
            UUID employeeId = employeeIdByEmail(employeeEmailForSuffix(employeeSuffix));

            var deleteResponse = client.delete().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            deleteResponse.expectStatus().isNoContent();

            var getResponse = client.get().uri(ADMIN_BASE_URL + "/employees/" + employeeId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken)).exchange();
            getResponse.expectStatus().isNotFound();
        });
    }
}
