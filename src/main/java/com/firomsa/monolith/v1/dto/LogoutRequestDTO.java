package com.firomsa.monolith.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LogoutRequestDTO(@NotNull String refreshToken, @NotBlank @Email String email) {

}
