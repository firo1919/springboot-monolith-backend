package com.firomsa.monolith.v1.controller;

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
import com.firomsa.monolith.v1.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API for performing authentication")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "For registering an admin")
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.OK)
    public RegisterResponseDTO registerAdmin(
            @Valid @RequestBody RegisterAdminRequestDTO registerAdminRequestDTO) {
        var response = authService.createAdmin(registerAdminRequestDTO);
        return response;
    }

    @Operation(summary = "For confirming otp")
    @PostMapping("/confirm-otp")
    @ResponseStatus(HttpStatus.OK)
    public ConfirmOtpResponseDTO confirmOtp(
            @Valid @RequestBody ConfirmOtpRequestDTO confirmOtpRequestDTO) {
        var response = authService.confirmOtp(confirmOtpRequestDTO);
        return response;
    }

    @Operation(summary = "For resending otp")
    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.OK)
    public ResendOtpResponseDTO resendOtp(
            @Valid @RequestBody ResendOtpRequestDTO resendOtpRequestDTO) {
        var response = authService.resendOtp(resendOtpRequestDTO);
        return response;
    }

    @Operation(summary = "For logging in")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO loginInUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        var response = authService.login(loginRequestDTO);
        return response;
    }

    @Operation(summary = "For refreshing access token")
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        var response = authService.refreshAccessToken(refreshTokenRequestDTO);
        return response;
    }

    @Operation(summary = "For logging out")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public LogoutResponseDTO logout(@Valid @RequestBody LogoutRequestDTO loginRequestDTO) {
        var response = authService.logoutUser(loginRequestDTO);
        return response;
    }
}
