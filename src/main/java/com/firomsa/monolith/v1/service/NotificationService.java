package com.firomsa.monolith.v1.service;

import org.springframework.stereotype.Service;

import com.firomsa.monolith.config.AdminConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final AdminConfig adminConfig;

    public void sendNewUserRegisteredAlert() {
        log.info("Sending new user registered alert");
        try {
            emailService.sendNewUserRegisteredAlert(adminConfig.getEmail());
        } catch (Exception e) {
            log.error("Failed to send new user registered alert: {}", e.getMessage(), e);
        }
    }
}
