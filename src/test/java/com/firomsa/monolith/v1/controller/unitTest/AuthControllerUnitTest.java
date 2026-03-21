package com.firomsa.monolith.v1.controller.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.v1.controller.AuthController;
import com.firomsa.monolith.v1.dto.ConfirmOtpRequestDTO;
import com.firomsa.monolith.v1.dto.ConfirmOtpResponseDTO;
import com.firomsa.monolith.v1.dto.LoginRequestDTO;
import com.firomsa.monolith.v1.dto.LoginResponseDTO;
import com.firomsa.monolith.v1.dto.LogoutRequestDTO;
import com.firomsa.monolith.v1.dto.LogoutResponseDTO;
import com.firomsa.monolith.v1.dto.RefreshTokenRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterAdminRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterResponseDTO;
import com.firomsa.monolith.v1.dto.ResendOtpRequestDTO;
import com.firomsa.monolith.v1.dto.ResendOtpResponseDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.service.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@WithMockUser
public class AuthControllerUnitTest {

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvcTester mockMvc;

    private static final String BASE_URL = "/api/v1/auth";

    private UserResponseDTO sampleUser(UUID id, String username) {
        return new UserResponseDTO(id, "Admin", "User", username, "admin@example.com",
                "+251900000001", Roles.ADMIN.name(), null, "2026-03-19T10:15:30", true, true);
    }

    private String registerAdminRequestJson() {
        return """
                {
                    "firstName": "Admin",
                    "lastName": "User",
                    "username": "admin.user",
                    "password": "password123",
                    "email": "admin@example.com",
                    "phone": "+251900000001",
                    "bootstrapToken": "test-bootstrap-token-12345678901234"
                }
                """;
    }

    @Test
    void shouldRegisterAdmin() {
        UserResponseDTO user = sampleUser(UUID.randomUUID(), "admin.user");
        RegisterResponseDTO response =
                new RegisterResponseDTO(user, "You have successfully registered");
        when(authService.createAdmin(any(RegisterAdminRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/admins").with(csrf())
                .contentType(APPLICATION_JSON).content(registerAdminRequestJson()).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.data.id").asString()
                .isEqualTo(user.getId().toString());
        assertThat(result).bodyJson().extractingPath("$.message").asString()
                .isEqualTo("You have successfully registered");

        verify(authService).createAdmin(any(RegisterAdminRequestDTO.class));
    }

    @Test
    void shouldConfirmOtp() {
        ConfirmOtpResponseDTO response = new ConfirmOtpResponseDTO("Successfully confirmed OTP");
        when(authService.confirmOtp(any(ConfirmOtpRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/confirm-otp").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "otp": "12345",
                                "email": "admin@example.com"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.message").asString()
                .isEqualTo("Successfully confirmed OTP");

        verify(authService).confirmOtp(any(ConfirmOtpRequestDTO.class));
    }

    @Test
    void shouldResendOtp() {
        ResendOtpResponseDTO response = new ResendOtpResponseDTO("Successfully resent OTP");
        when(authService.resendOtp(any(ResendOtpRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/resend-otp").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "email": "admin@example.com"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.message").asString()
                .isEqualTo("Successfully resent OTP");

        verify(authService).resendOtp(any(ResendOtpRequestDTO.class));
    }

    @Test
    void shouldLoginUser() {
        LoginResponseDTO response = new LoginResponseDTO(Roles.ADMIN, "access-token",
                "refresh-token", "admin.user", "admin@example.com");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/login").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "password": "password123",
                                "email": "admin@example.com"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.role").asString().isEqualTo("ADMIN");
        assertThat(result).bodyJson().extractingPath("$.accessToken").asString()
                .isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.refreshToken").asString()
                .isEqualTo("refresh-token");

        verify(authService).login(any(LoginRequestDTO.class));
    }

    @Test
    void shouldRefreshToken() {
        LoginResponseDTO response = new LoginResponseDTO(Roles.ADMIN, "new-access-token",
                "refresh-token", "admin.user", "admin@example.com");
        when(authService.refreshAccessToken(any(RefreshTokenRequestDTO.class)))
                .thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/refresh").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "refreshToken": "refresh-token",
                                "email": "admin@example.com"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.accessToken").asString()
                .isEqualTo("new-access-token");
        assertThat(result).bodyJson().extractingPath("$.refreshToken").asString()
                .isEqualTo("refresh-token");

        verify(authService).refreshAccessToken(any(RefreshTokenRequestDTO.class));
    }

    @Test
    void shouldLogoutUser() {
        LogoutResponseDTO response = new LogoutResponseDTO("Successfully logged out");
        when(authService.logoutUser(any(LogoutRequestDTO.class))).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri(BASE_URL + "/logout").with(csrf())
                .contentType(APPLICATION_JSON).content("""
                            {
                                "refreshToken": "refresh-token",
                                "email": "admin@example.com"
                            }
                        """).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.message").asString()
                .isEqualTo("Successfully logged out");

        verify(authService).logoutUser(any(LogoutRequestDTO.class));
    }
}
