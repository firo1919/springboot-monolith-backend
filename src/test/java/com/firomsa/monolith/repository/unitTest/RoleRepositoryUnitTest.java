package com.firomsa.monolith.repository.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.repository.RoleRepository;
import com.firomsa.monolith.support.SharedContainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoleRepositoryUnitTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = SharedContainers.POSTGRES;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("should find role by name")
    void shouldFindRoleByName() {
        // Arrange
        Role role = new Role();
        role.setName(Roles.EMPLOYEE);
        roleRepository.save(role);
        // Act
        var foundRole = roleRepository.findByName(Roles.EMPLOYEE);
        // Assert
        assertThat(foundRole).isNotNull();
        assertThat(foundRole.get().getName()).isEqualTo(Roles.EMPLOYEE);

    }

}
