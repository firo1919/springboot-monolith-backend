package com.firomsa.monolith.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditStatus;

public record AuditLogResponseDTO(
        Long id,
        UUID correlationId,
        UUID userId,
        String username,
        AuditAction action,
        String resourceType,
        UUID resourceId,
        String oldValue,
        String newValue,
        String ipAddress,
        String userAgent,
        LocalDateTime timestamp,
        AuditStatus status,
        String errorMessage) {
}
