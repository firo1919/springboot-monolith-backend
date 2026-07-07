package com.firomsa.monolith.v1.service;

import com.firomsa.monolith.config.AdminConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final AdminConfig adminConfig;

    public void sendOtp(String otp, String email) throws MailException {
        log.info("Sending confirmation otp to: {}", email);
        var mail = new SimpleMailMessage();
        mail.setTo(email);
        mail.setFrom(adminConfig.getEmail());
        mail.setText(otp);
        javaMailSender.send(mail);
    }

    public void sendDailyReport(String adminEmail, String report) throws MailException {
        log.info("Sending daily report to: {}", adminEmail);
        var mail = new SimpleMailMessage();
        mail.setTo(adminEmail);
        mail.setFrom(adminConfig.getEmail());
        mail.setSubject("Daily monolith Report - " + LocalDate.now());
        mail.setText(report);
        javaMailSender.send(mail);
    }

    public void sendNewUserRegisteredAlert(String adminEmail) throws MailException {
        log.info("Sending new user registered alert to: {}", adminEmail);
        var mail = new SimpleMailMessage();
        mail.setTo(adminEmail);
        mail.setFrom(adminConfig.getEmail());
        mail.setSubject("New User Registered Alert");
        mail.setText("A new user has registered on the platform.");
        javaMailSender.send(mail);
    }
}
