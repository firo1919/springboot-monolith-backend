package com.firomsa.monolith.v1.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmOtpRequestDTO(@NotBlank String otp, @NotBlank String email) {
}
