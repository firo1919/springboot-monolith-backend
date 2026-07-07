package com.firomsa.monolith.v1.controller;

import java.util.UUID;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.firomsa.monolith.v1.dto.PageRequest;
import com.firomsa.monolith.v1.dto.PageResponse;
import com.firomsa.monolith.v1.dto.RegisterRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterResponseDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.dto.UserUpdateRequestDTO;
import com.firomsa.monolith.v1.service.AuthService;
import com.firomsa.monolith.v1.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administrator", description = "API for admin operations")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final EmployeeService employeeService;

    // Employee management endpoints
    @Operation(summary = "For registering an employee")
    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO registerUser(
            @Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        var response = authService.create(registerRequestDTO);
        return response;
    }

    @Operation(summary = "For getting all employees")
    @GetMapping("/employees")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<UserResponseDTO> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        var pageable = PageRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection != null
                        ? sortDirection.equalsIgnoreCase("desc") ? Direction.DESC
                                : Direction.ASC
                        : Direction.ASC)
                .build()
                .toPageable();
        return employeeService.getEmployees(pageable);
    }

    @Operation(summary = "For getting an employee by id")
    @GetMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDTO getEmployeeById(@PathVariable UUID id) {
        var response = employeeService.getEmployeeById(id);
        return response;
    }

    @Operation(summary = "For updating an employee by id")
    @PutMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDTO updateEmployee(@PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequestDTO userUpdateRequestDTO) {
        var response = employeeService.updateEmployee(id, userUpdateRequestDTO);
        return response;
    }

    @Operation(summary = "For deactivating an employee by id")
    @PostMapping("/employees/{id}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public void deactivateEmployee(@PathVariable UUID id) {
        employeeService.deactivateEmployee(id);
    }

    @Operation(summary = "For activating an employee by id")
    @PostMapping("/employees/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public void activateEmployee(@PathVariable UUID id) {
        employeeService.activateEmployee(id);
    }

    @Operation(summary = "For deleting an employee by id")
    @DeleteMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
    }
}
