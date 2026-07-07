package com.firomsa.monolith.repository.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.repository.RoleRepository;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.support.SharedContainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryUnitTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = SharedContainers.POSTGRES;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

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

    @Test
    @DisplayName("should check if user exists by role")
    void shouldCheckIfExistsByRole() {
        // Arrange
        Role adminRole = roleRepository.findByName(Roles.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Roles.ADMIN).build()));
        user.setRole(adminRole);
        userRepository.save(user);

        // Act
        boolean existsAdmin = userRepository.existsByRole(adminRole);
        Role employeeRole = roleRepository.findByName(Roles.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Roles.EMPLOYEE).build()));
        boolean existsEmployee = userRepository.existsByRole(employeeRole);

        // Assert
        assertThat(existsAdmin).isTrue();
        assertThat(existsEmployee).isFalse();
    }

    @DisplayName("should exclude users with ADMIN role")
    void shouldExcludeUsersWithAdminRole() {
        Role adminRole = roleRepository.save(Role.builder().name(Roles.ADMIN).build());
        Role employeeRole = roleRepository.save(Role.builder().name(Roles.EMPLOYEE).build());

        User adminUser = User.builder().firstName("Admin").lastName("User").username("admin_user")
                .password("password123").email("admin@example.com").phone("1111111111").role(adminRole).build();
        User empUser = User.builder().firstName("Emp").lastName("User").username("emp_user")
                .password("password123").email("emp@example.com").phone("2222222222").role(employeeRole).build();

        userRepository.save(adminUser);
        userRepository.save(empUser);

        List<User> nonAdmins = userRepository.findByRoleNameNot(Roles.ADMIN);
        assertThat(nonAdmins).extracting(User::getUsername).contains("emp_user").doesNotContain("admin_user");
    }
}
