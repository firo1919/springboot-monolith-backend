package com.firomsa.monolith.scheduled;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.firomsa.monolith.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogCleanupJobUnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogCleanupJob auditLogCleanupJob;

    @Test
    @DisplayName("Should execute bulk delete for audit logs older than retention period")
    void cleanupOldAuditLogs_ShouldExecuteBulkDelete() {
        // Arrange
        when(auditLogRepository.deleteByTimestampBefore(any(LocalDateTime.class))).thenReturn(5);

        // Act
        auditLogCleanupJob.cleanupOldAuditLogs();

        // Assert
        verify(auditLogRepository, times(1)).deleteByTimestampBefore(any(LocalDateTime.class));
        verify(auditLogRepository, never()).findByTimestampBefore(any());
        verify(auditLogRepository, never()).deleteAll(any());
    }
}
