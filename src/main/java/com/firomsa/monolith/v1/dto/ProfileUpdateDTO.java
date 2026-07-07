package com.firomsa.monolith.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateDTO(@NotBlank String firstName, @NotBlank String lastName,
        @NotBlank String username, @NotBlank @Size(min = 8) String password,
        @NotBlank @Email String email, @NotBlank String phone) {

}
