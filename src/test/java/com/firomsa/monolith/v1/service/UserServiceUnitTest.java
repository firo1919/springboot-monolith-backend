package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.firomsa.monolith.exception.ResourceNotFoundException;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.v1.dto.ProfileUpdateDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StorageService storageService;
    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponseDTO userResponseDTO;
    private final String email = "john@example.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("johndoe");
        user.setEmail(email);
        user.setImageKey("profile.jpg");

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUsername("johndoe");
        userResponseDTO.setEmail(email);
    }

    @Test
    @DisplayName("Should update user profile")
    void updateProfile_WhenExists_ShouldUpdateAndReturnProfile() {
        // Arrange
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO("John", "Doe", "johndoe", "newpassword",
                "john@example.com", "1234567890");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(userMapper).updateModelFromDTO(user, updateDTO);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedpassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        UserResponseDTO result = userService.updateProfile(email, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("encodedpassword", user.getPassword());
        assertEquals("http://example.com/profile.jpg", result.getProfilePictureUrl());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userMapper, times(1)).updateModelFromDTO(user, updateDTO);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
    void updateProfile_WhenDoesNotExist_ShouldThrowException() {
        // Arrange
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO("John", "Doe", "johndoe", "newpassword",
                "john@example.com", "1234567890");
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile(email, updateDTO));
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return user profile")
    void getProfile_WhenExists_ShouldReturnProfile() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl("profile.jpg")).thenReturn("http://example.com/profile.jpg");

        // Act
        UserResponseDTO result = userService.getProfile(email);

        // Assert
        assertNotNull(result);
        assertEquals("http://example.com/profile.jpg", result.getProfilePictureUrl());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should add profile picture")
    void addProfilePicture_WhenExists_ShouldUpdateAndReturnProfile() {
        // Arrange
        String objectKey = "new-profile.jpg";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storageService.exists(objectKey)).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);
        when(storageService.getUrl(objectKey)).thenReturn("http://example.com/new-profile.jpg");

        // Act
        UserResponseDTO result = userService.addProfilePicture(email, objectKey);

        // Assert
        assertNotNull(result);
        assertEquals(objectKey, user.getImageKey());
        assertEquals("http://example.com/new-profile.jpg", result.getProfilePictureUrl());
        verify(userRepository, times(1)).findByEmail(email);
        verify(storageService, times(1)).exists(objectKey);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when image does not exist in storage")
    void addProfilePicture_WhenImageDoesNotExist_ShouldThrowException() {
        // Arrange
        String objectKey = "missing-profile.jpg";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storageService.exists(objectKey)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> userService.addProfilePicture(email, objectKey));
        verify(userRepository, times(1)).findByEmail(email);
        verify(storageService, times(1)).exists(objectKey);
        verify(userRepository, never()).save(any());
    }
}
