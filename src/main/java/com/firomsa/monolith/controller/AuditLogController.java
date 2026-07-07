package com.firomsa.monolith.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.firomsa.monolith.dto.AuditLogFilterDTO;
import com.firomsa.monolith.dto.AuditLogResponseDTO;
import com.firomsa.monolith.dto.AuditLogStatisticsDTO;
import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditStatus;
import com.firomsa.monolith.service.AuditLogExportService;
import com.firomsa.monolith.service.AuditLogQueryService;
import com.firomsa.monolith.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Audit Logs", description = "API for managing audit logs")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final AuditLogExportService auditLogExportService;
    private final AuditLogService auditLogService;

    @Operation(summary = "Get audit logs with pagination and filtering")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<AuditLogResponseDTO> getAuditLogs(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "timestamp,desc") String sort) {

        AuditLogFilterDTO filter = new AuditLogFilterDTO(
                correlationId != null ? UUID.fromString(correlationId) : null,
                userId != null ? UUID.fromString(userId) : null,
                username,
                action != null ? AuditAction.valueOf(action) : null,
                resourceType,
                status != null ? AuditStatus.valueOf(status) : null,
                startDate,
                endDate,
                page,
                size,
                sort);

        return auditLogQueryService.getAuditLogs(filter);
    }

    @Operation(summary = "Get audit log by ID")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuditLogResponseDTO getAuditLogById(@PathVariable Long id) {
        return auditLogQueryService.getAuditLogById(id);
    }

    @Operation(summary = "Get audit log statistics")
    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public AuditLogStatisticsDTO getStatistics() {
        return auditLogQueryService.getStatistics();
    }

    @Operation(summary = "Export audit logs to CSV")
    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv(@Valid @RequestBody AuditLogFilterDTO filter) throws IOException {
        byte[] csvData = auditLogExportService.exportToCsv(filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit-logs.csv");
        headers.setContentLength(csvData.length);

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    @Operation(summary = "Export audit logs to JSON")
    @PostMapping("/export/json")
    public ResponseEntity<byte[]> exportToJson(@Valid @RequestBody AuditLogFilterDTO filter) throws IOException {
        byte[] jsonData = auditLogExportService.exportToJson(filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "audit-logs.json");
        headers.setContentLength(jsonData.length);

        return new ResponseEntity<>(jsonData, headers, HttpStatus.OK);
    }

    @Operation(summary = "Bulk delete audit logs by date range")
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void deleteAuditLogs(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        auditLogService.deleteAuditLogsByDateRange(startDate, endDate);
        log.info("Deleted audit logs between {} and {}", startDate, endDate);
    }
}
