package com.firomsa.monolith.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.firomsa.monolith.v1.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/employee")
@Tag(name = "Employee", description = "API for employee operations")
@Slf4j
@RequiredArgsConstructor
public class EmployeeController {
    private final UserService userService;

    @Operation(summary = "Get Employee Name")
    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public String getMySales(Authentication authentication) {
        return userService.getProfile(authentication.getName()).getFirstName();
    }
}
