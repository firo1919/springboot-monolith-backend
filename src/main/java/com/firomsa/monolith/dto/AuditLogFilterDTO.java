package com.firomsa.monolith.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditStatus;

public record AuditLogFilterDTO(
        UUID correlationId,
        UUID userId,
        String username,
        AuditAction action,
        String resourceType,
        AuditStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer page,
        Integer size,
        String sort) {
    public AuditLogFilterDTO {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        if (sort == null || sort.isBlank()) {
            sort = "timestamp,desc";
        }
    }
}
