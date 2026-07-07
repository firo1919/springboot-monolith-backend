package com.firomsa.monolith.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.firomsa.monolith.dto.AuditLogFilterDTO;
import com.firomsa.monolith.dto.AuditLogResponseDTO;
import com.firomsa.monolith.dto.AuditLogStatisticsDTO;
import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditLog;
import com.firomsa.monolith.model.AuditStatus;
import com.firomsa.monolith.repository.AuditLogRepository;
import com.firomsa.monolith.repository.specification.AuditLogSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponseDTO> getAuditLogs(AuditLogFilterDTO filter) {
        Pageable pageable = createPageable(filter);

        Page<AuditLog> auditLogs = auditLogRepository.findAll(
                AuditLogSpecification.withFilters(filter), pageable);

        return auditLogs.map(this::toResponseDTO);
    }

    public AuditLogResponseDTO getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found with id: " + id));
        return toResponseDTO(auditLog);
    }

    public AuditLogStatisticsDTO getStatistics() {
        long totalLogs = auditLogRepository.count();
        long successCount = auditLogRepository.countByStatus(AuditStatus.SUCCESS);
        long failureCount = auditLogRepository.countByStatus(AuditStatus.FAILURE);

        Map<String, Long> actionCounts = new HashMap<>();
        for (AuditAction action : AuditAction.values()) {
            long count = auditLogRepository.countByAction(action);
            if (count > 0) {
                actionCounts.put(action.name(), count);
            }
        }

        List<Object[]> resourceTypeCountsList = auditLogRepository.countByResourceTypeGrouped();
        Map<String, Long> resourceTypeCounts = new HashMap<>();
        for (Object[] row : resourceTypeCountsList) {
            String resourceType = (String) row[0];
            if (resourceType != null) {
                resourceTypeCounts.put(resourceType, (Long) row[1]);
            }
        }

        return new AuditLogStatisticsDTO(totalLogs, successCount, failureCount, actionCounts, resourceTypeCounts);
    }

    private Pageable createPageable(AuditLogFilterDTO filter) {
        Sort sort = parseSort(filter.sort());
        return PageRequest.of(filter.page(), filter.size(), sort);
    }

    private Sort parseSort(String sortParam) {
        String[] parts = sortParam.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "timestamp");
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();

        Sort.Direction sortDirection = direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(sortDirection, field);
    }

    private AuditLogResponseDTO toResponseDTO(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getId(),
                auditLog.getCorrelationId(),
                auditLog.getUserId(),
                auditLog.getUsername(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getTimestamp(),
                auditLog.getStatus(),
                auditLog.getErrorMessage());
    }
}
