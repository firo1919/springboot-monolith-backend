package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JWTAuthServiceUnitTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private Authentication authentication;
    @Mock
    private Jwt jwt;
    @InjectMocks
    private JWTAuthService jwtAuthService;

    @BeforeEach
    void setUp() {
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // Use lenient() to avoid UnnecessaryStubbingException if a test doesn't use these
        lenient().when(authentication.getName()).thenReturn("john@example.com");
        lenient().doReturn(authorities).when(authentication).getAuthorities();
        lenient().when(jwt.getTokenValue()).thenReturn("mocked-jwt-token");
    }

    @Test
    @DisplayName("Should generate access token")
    void generateToken_ShouldReturnTokenString() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        // Act
        String token = jwtAuthService.generateToken(authentication);

        // Assert
        assertNotNull(token);
        assertEquals("mocked-jwt-token", token);
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("Should generate refresh token")
    void generateRefreshToken_ShouldReturnTokenString() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        // Act
        String token = jwtAuthService.generateRefreshToken(authentication);

        // Assert
        assertNotNull(token);
        assertEquals("mocked-jwt-token", token);
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }
}
