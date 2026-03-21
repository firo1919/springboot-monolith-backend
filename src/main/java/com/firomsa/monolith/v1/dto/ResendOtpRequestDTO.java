package com.firomsa.monolith.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequestDTO(@NotBlank @Email String email) {

}
