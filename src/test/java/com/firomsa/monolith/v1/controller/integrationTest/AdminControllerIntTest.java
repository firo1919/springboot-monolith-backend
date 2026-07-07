package com.firomsa.monolith.v1.controller.integrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.UserRepository;

public class AdminControllerIntTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin";

    @AfterEach
    void tearDown() {
        confirmationOtpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String registerEmployeeJson(String suffix) {
        return """
                {
                    "firstName": "EmpFirst%s",
                    "lastName": "EmpLast%s",
                    "username": "employee_%s",
                    "password": "password123",
                    "email": "employee_%s@example.com",
                    "role": "EMPLOYEE",
                    "phone": "+251911%s"
                }
                """.formatted(suffix, suffix, suffix, suffix, suffix.substring(0, 6));
    }

    private String updateEmployeeJson(String suffix) {
        return """
                {
                    "firstName": "UpdatedFirst%s",
                    "lastName": "UpdatedLast%s",
                    "username": "updated_employee_%s",
                    "password": "password123",
                    "email": "updated_%s@example.com",
                    "role": "EMPLOYEE",
                    "phone": "+251922%s"
                }
                """.formatted(suffix, suffix, suffix, suffix, suffix.substring(0, 6));
    }

    private UUID findEmployeeIdBySuffix(String suffix) {
        return userRepository.findByEmail("employee_" + suffix + "@example.com").orElseThrow()
                .getId();
    }

    private void createEmployee(String suffix) {
        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/employees")
                .contentType(APPLICATION_JSON).content(registerEmployeeJson(suffix)).exchange();

        assertThat(result).hasStatus(org.springframework.http.HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.data.email").asString()
                .isEqualTo("employee_" + suffix + "@example.com");
    }

    @Test
    void shouldRejectUnauthorizedRegisterEmployee() {
        assertThat(mockMvc.post().uri(BASE_URL + "/employees").contentType(APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedGetAllEmployees() {
        assertThat(mockMvc.get().uri(BASE_URL + "/employees").exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedGetEmployeeById() {
        assertThat(mockMvc.get().uri(BASE_URL + "/employees/{id}", UUID.randomUUID()).exchange())
                .hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedUpdateEmployee() {
        assertThat(mockMvc.put().uri(BASE_URL + "/employees/{id}", UUID.randomUUID())
                .contentType(APPLICATION_JSON).content("{}").exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedDeactivateEmployee() {
        assertThat(mockMvc.post().uri(BASE_URL + "/employees/{id}/deactivate", UUID.randomUUID())
                .exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedActivateEmployee() {
        assertThat(mockMvc.post().uri(BASE_URL + "/employees/{id}/activate", UUID.randomUUID())
                .exchange()).hasStatus(401);
    }

    @Test
    void shouldRejectUnauthorizedDeleteEmployee() {
        assertThat(mockMvc.delete().uri(BASE_URL + "/employees/{id}", UUID.randomUUID()).exchange())
                .hasStatus(401);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldReturnBadRequestWhenCreateEmployeePayloadIsInvalid() {
        assertThat(mockMvc.post().uri(BASE_URL + "/employees").contentType(APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldReturnBadRequestWhenEmployeeIdIsMalformed() {
        assertThat(mockMvc.get().uri(BASE_URL + "/employees/not-a-uuid").exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedRegisterEmployee() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        assertThat(userRepository.findByEmail("employee_" + suffix + "@example.com")).isPresent();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedGetAllEmployees() {
        String suffix = randomSuffix();
        createEmployee(suffix);

        MvcTestResult result = mockMvc.get().uri(BASE_URL + "/employees").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyText().contains("employee_" + suffix + "@example.com");
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedGetEmployeeById() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        UUID employeeId = findEmployeeIdBySuffix(suffix);

        MvcTestResult result = mockMvc.get().uri(BASE_URL + "/employees/{id}", employeeId).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").asString()
                .isEqualTo(employeeId.toString());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedUpdateEmployee() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        UUID employeeId = findEmployeeIdBySuffix(suffix);

        MvcTestResult result = mockMvc.put().uri(BASE_URL + "/employees/{id}", employeeId)
                .contentType(APPLICATION_JSON).content(updateEmployeeJson(suffix)).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").asString()
                .isEqualTo("updated_" + suffix + "@example.com");
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedDeactivateEmployee() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        UUID employeeId = findEmployeeIdBySuffix(suffix);

        assertThat(
                mockMvc.post().uri(BASE_URL + "/employees/{id}/deactivate", employeeId).exchange())
                .hasStatusOk();

        assertThat(userRepository.findById(employeeId)).isPresent();
        assertThat(userRepository.findById(employeeId).orElseThrow().isActive()).isFalse();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedActivateEmployee() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        UUID employeeId = findEmployeeIdBySuffix(suffix);

        assertThat(
                mockMvc.post().uri(BASE_URL + "/employees/{id}/deactivate", employeeId).exchange())
                .hasStatusOk();

        assertThat(mockMvc.post().uri(BASE_URL + "/employees/{id}/activate", employeeId).exchange())
                .hasStatusOk();

        assertThat(userRepository.findById(employeeId)).isPresent();
        assertThat(userRepository.findById(employeeId).orElseThrow().isActive()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void shouldAllowAuthorizedDeleteEmployee() {
        String suffix = randomSuffix();
        createEmployee(suffix);
        UUID employeeId = findEmployeeIdBySuffix(suffix);

        assertThat(mockMvc.delete().uri(BASE_URL + "/employees/{id}", employeeId).exchange())
                .hasStatus(org.springframework.http.HttpStatus.NO_CONTENT);

        assertThat(userRepository.existsById(employeeId)).isFalse();
    }
}
