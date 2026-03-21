package com.firomsa.monolith.v1.service;

import com.firomsa.monolith.config.AdminConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
