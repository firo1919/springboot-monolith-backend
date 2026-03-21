package com.firomsa.monolith.v1.dto;

import com.firomsa.monolith.model.Roles;

public record LoginResponseDTO(Roles role, String accessToken, String refreshToken, String username,
        String email) {

}
