package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.firomsa.monolith.config.AdminConfig;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUnitTest {

    @Mock
    private EmailService emailService;

    @Mock
    private AdminConfig adminConfig;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldSendNewUserRegisteredAlertEmail() {
        // Arrange
        when(adminConfig.getEmail()).thenReturn("admin@example.com");

        // Act
        notificationService.sendNewUserRegisteredAlert();

        // Assert
        verify(emailService).sendNewUserRegisteredAlert("admin@example.com");
    }

    @Test
    void shouldCatchExceptionAndNotRethrowWhenEmailFails() {
        // Arrange
        when(adminConfig.getEmail()).thenReturn("admin@test.com");
        doThrow(new RuntimeException("Mail server down"))
                .when(emailService).sendNewUserRegisteredAlert("admin@test.com");

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.sendNewUserRegisteredAlert());
        verify(emailService).sendNewUserRegisteredAlert("admin@test.com");
    }
}
