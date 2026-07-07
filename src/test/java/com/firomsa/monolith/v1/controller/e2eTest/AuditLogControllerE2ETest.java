package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.firomsa.monolith.repository.AuditLogRepository;

class AuditLogControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void testAuditLogFlow() {
        String suffix = randomSuffix();
        String adminEmail = adminEmailForSuffix(suffix);

        // Register admin
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(MediaType.APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        // Confirm OTP
        String otp = latestOtpForEmail(adminEmail);
        String confirmPayload = "{\"email\":\"" + adminEmail + "\",\"otp\":\"" + otp + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        // Login
        String accessToken = loginByEmail(adminEmail);

        // Verify audit log was created
        long auditLogCount = auditLogRepository.count();
        assertThat(auditLogCount).isGreaterThan(0);

        // Get audit logs
        var auditLogsResponse = client.get().uri(ADMIN_BASE_URL + "/audit-logs")
                .header("Authorization", authorizationHeader(accessToken)).exchange();
        auditLogsResponse.expectStatus().isOk();

        String auditLogsBody = auditLogsResponse.returnResult(String.class).getResponseBody();
        assertThat(auditLogsBody).contains("content");
    }

    @Test
    void testAuditLogStatistics() {
        String suffix = randomSuffix();
        String adminEmail = adminEmailForSuffix(suffix);

        // Register and login
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(MediaType.APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        String otp = latestOtpForEmail(adminEmail);
        String confirmPayload = "{\"email\":\"" + adminEmail + "\",\"otp\":\"" + otp + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        String accessToken = loginByEmail(adminEmail);

        // Get statistics
        var statsResponse = client.get().uri(ADMIN_BASE_URL + "/audit-logs/statistics")
                .header("Authorization", authorizationHeader(accessToken)).exchange();
        statsResponse.expectStatus().isOk();

        String statsBody = statsResponse.returnResult(String.class).getResponseBody();
        assertThat(statsBody).contains("totalLogs");
        assertThat(statsBody).contains("successCount");
        assertThat(statsBody).contains("failureCount");
    }

    @Test
    void testAuditLogExport() {
        String suffix = randomSuffix();
        String adminEmail = adminEmailForSuffix(suffix);

        // Register and login
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(MediaType.APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        String otp = latestOtpForEmail(adminEmail);
        String confirmPayload = "{\"email\":\"" + adminEmail + "\",\"otp\":\"" + otp + "\"}";
        var confirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON).body(confirmPayload).exchange();
        confirmResponse.expectStatus().isOk();

        String accessToken = loginByEmail(adminEmail);

        // Export to CSV
        String exportFilter = """
                {
                    "page": 0,
                    "size": 20,
                    "sort": "timestamp,desc"
                }
                """;

        var exportResponse = client.post().uri(ADMIN_BASE_URL + "/audit-logs/export/csv")
                .header("Authorization", authorizationHeader(accessToken))
                .contentType(MediaType.APPLICATION_JSON).body(exportFilter).exchange();
        exportResponse.expectStatus().isOk();
    }

    @Test
    void testAuditLogAccessDeniedForNonAdmin() {
        String suffix = randomSuffix();
        String adminEmail = adminEmailForSuffix(suffix);
        String employeeEmail = employeeEmailForSuffix(suffix);

        // Register and login an admin (employees can only be created by an admin)
        var registerResponse = client.post().uri(AUTH_BASE_URL + "/admins")
                .contentType(MediaType.APPLICATION_JSON).body(registerAdminPayload(suffix)).exchange();
        registerResponse.expectStatus().isOk();

        String adminOtp = latestOtpForEmail(adminEmail);
        String adminConfirmPayload = "{\"email\":\"" + adminEmail + "\",\"otp\":\"" + adminOtp + "\"}";
        var adminConfirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON).body(adminConfirmPayload).exchange();
        adminConfirmResponse.expectStatus().isOk();

        String adminAccessToken = loginByEmail(adminEmail);

        // Create the employee through the admin endpoint
        var registerEmployeeResponse = client.post().uri(ADMIN_BASE_URL + "/employees")
                .header("Authorization", authorizationHeader(adminAccessToken))
                .contentType(MediaType.APPLICATION_JSON).body(registerEmployeePayload(suffix)).exchange();
        registerEmployeeResponse.expectStatus().isCreated();

        String employeeOtp = latestOtpForEmail(employeeEmail);
        String employeeConfirmPayload = "{\"email\":\"" + employeeEmail + "\",\"otp\":\"" + employeeOtp + "\"}";
        var employeeConfirmResponse = client.post().uri(AUTH_BASE_URL + "/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON).body(employeeConfirmPayload).exchange();
        employeeConfirmResponse.expectStatus().isOk();

        String employeeAccessToken = loginByEmail(employeeEmail);

        // Try to access audit logs as a non-admin (should be denied)
        var auditLogsResponse = client.get().uri(ADMIN_BASE_URL + "/audit-logs")
                .header("Authorization", authorizationHeader(employeeAccessToken)).exchange();
        auditLogsResponse.expectStatus().isForbidden();
    }
}
