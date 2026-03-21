package com.firomsa.monolith.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.firomsa.monolith.config.AdminConfig;

@ExtendWith(MockitoExtension.class)
class EmailServiceUnitTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private AdminConfig adminConfig;
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(adminConfig.getEmail()).thenReturn("admin@example.com");
    }

    @Test
    @DisplayName("Should send OTP email")
    void sendOtp_ShouldSendEmail() {
        // Arrange
        String otp = "12345";
        String email = "user@example.com";
        doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendOtp(otp, email);

        // Assert
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
