package com.firomsa.monolith.repository.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import com.firomsa.monolith.model.RefreshToken;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.RefreshTokenRepository;
import com.firomsa.monolith.repository.UserRepository;

@DataJpaTest
public class RefreshTokenRepositoryUnitTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // add a user with a refresh token to the database for testing
        User user = User.builder().firstName("John").lastName("Doe").username("john_doe")
                .password("password123").phone("12345678").email("john.doe@example.com").build();
        var savedUser = userRepository.save(user);
        RefreshToken refreshTokenOne =
                RefreshToken.builder().token("test-refresh-token").user(savedUser).build();
        RefreshToken refreshTokenTwo =
                RefreshToken.builder().token("test-refresh-token-2").user(savedUser).build();
        refreshTokenRepository.save(refreshTokenOne);
        refreshTokenRepository.save(refreshTokenTwo);
    }

    @Test
    @DisplayName("should delete all refresh tokens for a given user")
    void shouldDeleteAllByUser() {
        // Arrange
        var user = userRepository.findByEmail("john.doe@example.com");
        // Act
        refreshTokenRepository.deleteAllByUser(user.get());
        // Assert
        user.get().getRefreshTokens().forEach(rt -> assertThat(rt.getToken())
                .isNotIn("test-refresh-token", "test-refresh-token-2"));
    }

    @Test
    @DisplayName("should find refresh token by token")
    void shouldFindByToken() {
        // Act
        var refreshToken = refreshTokenRepository.findByToken("test-refresh-token");
        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.get().getToken()).isEqualTo("test-refresh-token");
    }


    @Test
    @DisplayName("should find refresh token by token and user")
    void shouldFindByTokenAndUser() {
        // Arrange
        var user = userRepository.findByEmail("john.doe@example.com");
        // Act
        var refreshToken =
                refreshTokenRepository.findByTokenAndUser("test-refresh-token", user.get());
        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.get().getToken()).isEqualTo("test-refresh-token");
        assertThat(refreshToken.get().getUser().getEmail()).isEqualTo("john.doe@example.com");
    }

}
