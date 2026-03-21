package com.firomsa.monolith.v1.dto;

import com.firomsa.monolith.model.Roles;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDTO(@NotBlank String firstName, @NotBlank String lastName,
        @NotBlank String username, @NotBlank @Size(min = 8) String password,
        @NotBlank @Email String email, @NotNull Roles role, @NotBlank String phone) {
}
