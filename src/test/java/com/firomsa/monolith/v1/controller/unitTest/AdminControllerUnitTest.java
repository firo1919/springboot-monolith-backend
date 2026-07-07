package com.firomsa.monolith.v1.controller.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import org.springframework.data.domain.Pageable;

import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.support.TestCacheConfig;
import com.firomsa.monolith.v1.dto.PageResponse;
import com.firomsa.monolith.v1.controller.AdminController;
import com.firomsa.monolith.v1.dto.RegisterRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterResponseDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.dto.UserUpdateRequestDTO;
import com.firomsa.monolith.v1.service.AuthService;
import com.firomsa.monolith.v1.service.EmployeeService;

@WebMvcTest(AdminController.class)
@Import(TestCacheConfig.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "SCOPE_ADMIN")
public class AdminControllerUnitTest {

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private EmployeeService employeeService;

    @Autowired
    private MockMvcTester mockMvc;

    private static final String BASE_URL = "/api/v1/admin";

    private UserResponseDTO sampleUser(UUID id, String username) {
        return new UserResponseDTO(id, "John", "Doe", username, username + "@example.com",
                "+251911111111", Roles.EMPLOYEE.name(), null, "2026-03-19T10:15:30", true, true);
    }

    private String registerRequestJson() {
        return """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "username": "jane.smith",
                    "password": "password123",
                    "email": "jane.smith@example.com",
                    "role": "EMPLOYEE",
                    "phone": "+251922222222"
                }
                """;
    }

    @Test
    void shouldReturnAllEmployees() {
        UserResponseDTO user = sampleUser(UUID.randomUUID(), "employee.one");
        PageResponse<UserResponseDTO> page = PageResponse.<UserResponseDTO>builder()
                .content(List.of(user)).pageNumber(0).pageSize(10)
                .totalElements(1).totalPages(1).first(true).last(true).empty(false).build();
        when(employeeService.getEmployees(any(Pageable.class))).thenReturn(page);

        MvcTestResult result = mockMvc.get().uri(BASE_URL + "/employees").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0].id").asString()
                .isEqualTo(user.getId().toString());
        assertThat(result).bodyJson().extractingPath("$.content[0].username").asString()
                .isEqualTo("employee.one");

        verify(employeeService).getEmployees(any(Pageable.class));
    }

    @Test
    void shouldDeactivateEmployee() {
        UUID employeeId = UUID.randomUUID();
        doNothing().when(employeeService).deactivateEmployee(employeeId);

        MvcTestResult result = mockMvc.post()
                .uri(BASE_URL + "/employees/{id}/deactivate", employeeId).with(csrf()).exchange();
        assertThat(result).hasStatusOk();

        verify(employeeService).deactivateEmployee(employeeId);
    }

    @Test
    void shouldDeleteEmployee() {
        UUID employeeId = UUID.randomUUID();
        doNothing().when(employeeService).deleteEmployee(employeeId);

        MvcTestResult result = mockMvc.delete().uri(BASE_URL + "/employees/{id}", employeeId)
                .with(csrf()).exchange();
        assertThat(result).hasStatus(org.springframework.http.HttpStatus.NO_CONTENT);

        verify(employeeService).deleteEmployee(employeeId);
    }

    @Test
    void shouldGetAllEmployees() {
        UserResponseDTO first = sampleUser(UUID.randomUUID(), "employee.one");
        UserResponseDTO second = sampleUser(UUID.randomUUID(), "employee.two");
        PageResponse<UserResponseDTO> page = PageResponse.<UserResponseDTO>builder()
                .content(List.of(first, second)).pageNumber(0).pageSize(10)
                .totalElements(2).totalPages(1).first(true).last(true).empty(false).build();
        when(employeeService.getEmployees(any(Pageable.class))).thenReturn(page);

        MvcTestResult result = mockMvc.get().uri(BASE_URL + "/employees").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[1].id").asString()
                .isEqualTo(second.getId().toString());
        assertThat(result).bodyJson().extractingPath("$.content[1].username").asString()
                .isEqualTo("employee.two");

        verify(employeeService).getEmployees(any(Pageable.class));
    }

    @Test
    void shouldGetEmployeeById() {
        UUID employeeId = UUID.randomUUID();
        UserResponseDTO employee = sampleUser(employeeId, "employee.by.id");
        when(employeeService.getEmployeeById(employeeId)).thenReturn(employee);

        MvcTestResult result = mockMvc.get().uri(BASE_URL + "/employees/{id}", employeeId).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").asString()
                .isEqualTo(employeeId.toString());
        assertThat(result).bodyJson().extractingPath("$.username").asString()
                .isEqualTo("employee.by.id");

        verify(employeeService).getEmployeeById(employeeId);
    }

    @Test
    void shouldRegisterUser() {
        UserResponseDTO user = sampleUser(UUID.randomUUID(), "jane.smith");
        RegisterResponseDTO response = new RegisterResponseDTO(user, "User created successfully");
        when(authService.create(any(RegisterRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/employees").with(csrf())
                .contentType(APPLICATION_JSON).content(registerRequestJson()).exchange();
        assertThat(result).hasStatus(org.springframework.http.HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.data.id").asString()
                .isEqualTo(user.getId().toString());
        assertThat(result).bodyJson().extractingPath("$.message").asString()
                .isEqualTo("User created successfully");

        verify(authService).create(any(RegisterRequestDTO.class));
    }

    @Test
    void shouldUpdateEmployee() {
        UUID employeeId = UUID.randomUUID();
        UserResponseDTO response = sampleUser(employeeId, "updated.user");

        when(employeeService.updateEmployee(eq(employeeId), any(UserUpdateRequestDTO.class)))
                .thenReturn(response);

        MvcTestResult result = mockMvc.put().uri(BASE_URL + "/employees/{id}", employeeId)
                .with(csrf()).contentType(APPLICATION_JSON).content("""
                        {
                            "firstName": "Updated",
                            "lastName": "User",
                            "username": "updated.user",
                            "password": "password123",
                            "email": "updated.user@example.com",
                            "role": "EMPLOYEE",
                            "phone": "+251933333333"
                        }
                            """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").asString()
                .isEqualTo(employeeId.toString());
        assertThat(result).bodyJson().extractingPath("$.username").asString()
                .isEqualTo("updated.user");

        verify(employeeService).updateEmployee(eq(employeeId), any(UserUpdateRequestDTO.class));
    }
}
