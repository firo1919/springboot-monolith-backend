package com.firomsa.monolith.v1.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String profilePictureUrl;
    private String createdAt;
    private boolean active;
    private boolean enabled;
}
