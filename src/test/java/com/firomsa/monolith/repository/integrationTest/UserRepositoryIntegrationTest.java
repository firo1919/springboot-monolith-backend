package com.firomsa.monolith.repository.integrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.UserRepository;

public class UserRepositoryIntegrationTest extends AbstractIntegrationTestRepo {

    @Autowired
    private UserRepository userRepository;

    private final User user = User.builder().firstName("John").lastName("Doe").username("john_doe")
            .password("password123").email("john.doe@example.com").phone("1234567890").build();


    @Test
    @DisplayName("should find user by email")
    void shouldFindUserByEmail() {
        // Arrange
        userRepository.save(user);
        // Act
        Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");
        // Assert
        assertThat(foundUser).isNotNull();
        assertAll(() -> {
            assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(foundUser.get().getFirstName()).isEqualTo("John");
            assertThat(foundUser.get().getLastName()).isEqualTo("Doe");
            assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
            assertThat(foundUser.get().getPassword()).isEqualTo("password123");
            assertThat(foundUser.get().getPhone()).isEqualTo("1234567890");
        });

    }

    @Test
    @DisplayName("should find user by username")
    void shouldFindUserByUsername() {
        // Arrange
        userRepository.save(user);
        // Act
        Optional<User> foundUser = userRepository.findByUsername("john_doe");
        // Assert
        assertThat(foundUser).isNotNull();
        assertAll(() -> {
            assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(foundUser.get().getFirstName()).isEqualTo("John");
            assertThat(foundUser.get().getLastName()).isEqualTo("Doe");
            assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
            assertThat(foundUser.get().getPassword()).isEqualTo("password123");
            assertThat(foundUser.get().getPhone()).isEqualTo("1234567890");
        });
    }

}
