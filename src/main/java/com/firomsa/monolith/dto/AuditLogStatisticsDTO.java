package com.firomsa.monolith.dto;

import java.util.Map;

public record AuditLogStatisticsDTO(
        long totalLogs,
        long successCount,
        long failureCount,
        Map<String, Long> actionCounts,
        Map<String, Long> resourceTypeCounts) {
}
