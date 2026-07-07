package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.firomsa.monolith.exception.ResourceNotFoundException;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.RoleRepository;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.v1.dto.PageResponse;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.dto.UserUpdateRequestDTO;
import com.firomsa.monolith.v1.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private EmployeeService employeeService;

    private User user;
    private UserResponseDTO userResponseDTO;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setActive(true);
        user.setImageKey("profile.jpg");

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(userId);
        userResponseDTO.setUsername("johndoe");
        userResponseDTO.setEmail("john@example.com");
        userResponseDTO.setActive(true);
    }

    @Test
    @DisplayName("Should return all employees excluding ADMIN users")
    void getEmployees_ShouldReturnListOfEmployees() {
        // Arrange
        when(userRepository.findByRoleNameNot(Roles.ADMIN)).thenReturn(List.of(user));
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        List<UserResponseDTO> result = employeeService.getEmployees();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("johndoe", result.get(0).getUsername());
        assertEquals("http://example.com/profile.jpg", result.get(0).getProfilePictureUrl());

        verify(userRepository, times(1)).findByRoleNameNot(Roles.ADMIN);
        verify(userMapper, times(1)).toDTO(user);
    }

    @Test
    @DisplayName("Should return page of employees excluding ADMIN users")
    void getEmployeesPageable_ShouldReturnPageOfEmployees() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findByRoleNameNot(Roles.ADMIN, pageable)).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        PageResponse<UserResponseDTO> result = employeeService.getEmployees(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("johndoe", result.getContent().get(0).getUsername());
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());

        verify(userRepository, times(1)).findByRoleNameNot(Roles.ADMIN, pageable);
    }

    @Test
    @DisplayName("Should return employee by ID with profile picture URL")
    void getEmployeeById_WhenExists_ShouldReturnEmployee() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        UserResponseDTO result = employeeService.getEmployeeById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("http://example.com/profile.jpg", result.getProfilePictureUrl());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when employee ID does not exist")
    void getEmployeeById_WhenDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.getEmployeeById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should deactivate employee")
    void deactivateEmployee_WhenExists_ShouldSetInactive() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Act
        employeeService.deactivateEmployee(userId);

        // Assert
        assertFalse(user.isActive());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should activate employee")
    void activateEmployee_WhenExists_ShouldSetActive() {
        // Arrange
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Act
        employeeService.activateEmployee(userId);

        // Assert
        assertTrue(user.isActive());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should delete employee")
    void deleteEmployee_WhenExists_ShouldDelete() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        // Act
        employeeService.deleteEmployee(userId);

        // Assert
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("Should update employee")
    void updateEmployee_WhenExists_ShouldUpdateAndReturnEmployee() {
        // Arrange
        UserUpdateRequestDTO updateRequest = new UserUpdateRequestDTO("John", "Doe", "johndoe",
                "newpassword", "john@example.com", Roles.EMPLOYEE, "1234567890");
        Role role = new Role();
        role.setName(Roles.EMPLOYEE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(Roles.EMPLOYEE)).thenReturn(Optional.of(role));
        doNothing().when(userMapper).updateModelFromDTO(user, updateRequest);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedpassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        UserResponseDTO result = employeeService.updateEmployee(userId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("encodedpassword", user.getPassword());
        assertEquals(role, user.getRole());
        verify(userRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(Roles.EMPLOYEE);
        verify(userMapper, times(1)).updateModelFromDTO(user, updateRequest);
        verify(userRepository, times(1)).save(user);
    }
}
