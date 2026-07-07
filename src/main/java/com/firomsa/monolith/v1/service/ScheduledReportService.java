package com.firomsa.monolith.v1.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.firomsa.monolith.config.AdminConfig;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledReportService {

    private final EmailService emailService;
    private final AdminConfig adminConfig;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Scheduled(cron = "0 0 8 * * *", zone = "Africa/Addis_Ababa")
    public void sendDailyReport() {
        log.info("Generating daily monolith report");

        String report = generateReport();
        emailService.sendDailyReport(adminConfig.getEmail(), report);

        log.info("Daily monolith report sent to admin");
    }

    String generateReport() {
        long totalUsers = userRepository.count();
        long totalAuditLogs = auditLogRepository.count();

        StringBuilder report = new StringBuilder();
        report.append("Daily Monolith Report\n");
        report.append("=====================\n\n");
        report.append("Date: ").append(LocalDate.now()).append("\n\n");
        report.append("SYSTEM OVERVIEW\n");
        report.append("---------------\n");
        report.append("Total Users: ").append(totalUsers).append("\n");
        report.append("Total Audit Logs: ").append(totalAuditLogs).append("\n");

        return report.toString();
    }
}
