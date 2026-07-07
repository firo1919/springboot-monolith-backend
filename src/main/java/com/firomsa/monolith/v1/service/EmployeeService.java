package com.firomsa.monolith.v1.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponseDTO> getEmployees() {
        var users = userRepository.findByRoleNameNot(Roles.ADMIN);
        var response = new ArrayList<UserResponseDTO>();
        for (var user : users) {
            var userResponse = userMapper.toDTO(user);
            if (user.getImageKey() != null) {
                userResponse.setProfilePictureUrl(storageService.getUrl(user.getImageKey()));
            }
            response.add(userResponse);
        }

        return response;
    }

    public PageResponse<UserResponseDTO> getEmployees(Pageable pageable) {
        Page<User> userPage = userRepository.findByRoleNameNot(Roles.ADMIN, pageable);
        var response = new ArrayList<UserResponseDTO>();
        for (var user : userPage.getContent()) {
            var userResponse = userMapper.toDTO(user);
            if (user.getImageKey() != null) {
                userResponse.setProfilePictureUrl(storageService.getUrl(user.getImageKey()));
            }
            response.add(userResponse);
        }

        return PageResponse.<UserResponseDTO>builder()
                .content(response)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .empty(userPage.isEmpty())
                .build();
    }

    public UserResponseDTO getEmployeeById(UUID id) {
        var user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id.toString() + " User not found"));
        var userResponse = userMapper.toDTO(user);
        if (user.getImageKey() != null) {
            userResponse.setProfilePictureUrl(storageService.getUrl(user.getImageKey()));
        }
        return userResponse;
    }

    public void deactivateEmployee(UUID id) {
        var user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id.toString() + " User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateEmployee(UUID id) {
        var user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id.toString() + " User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    public void deleteEmployee(UUID id) {
        var user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id.toString() + " User not found"));
        userRepository.delete(user);
    }

    public UserResponseDTO updateEmployee(UUID id, UserUpdateRequestDTO userUpdateRequestDTO) {
        var user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id.toString() + " User not found"));
        Role role = roleRepository.findByName(userUpdateRequestDTO.role()).orElseThrow(
                () -> new ResourceNotFoundException("Role: " + userUpdateRequestDTO.role().name()));
        userMapper.updateModelFromDTO(user, userUpdateRequestDTO);
        user.setPassword(passwordEncoder.encode(userUpdateRequestDTO.password()));
        user.setRole(role);
        userRepository.save(user);
        var updatedUserResponse = userMapper.toDTO(user);
        if (user.getImageKey() != null) {
            updatedUserResponse.setProfilePictureUrl(storageService.getUrl(user.getImageKey()));
        }
        return updatedUserResponse;
    }
}
