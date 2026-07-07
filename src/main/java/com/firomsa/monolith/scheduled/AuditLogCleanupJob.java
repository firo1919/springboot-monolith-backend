package com.firomsa.monolith.scheduled;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.firomsa.monolith.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogCleanupJob {

    private final AuditLogRepository auditLogRepository;

    @Value("${audit.logging.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${audit.logging.cleanup.cron:0 0 2 * * ?}") // Default: 2 AM daily
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        log.info("Starting audit log cleanup for logs older than {} days (before {})",
                retentionDays, cutoffDate);

        try {
            int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffDate);

            log.info("Audit log cleanup completed. Deleted {} logs older than {}",
                    deletedCount, cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup audit logs: {}", e.getMessage(), e);
        }
    }
}
