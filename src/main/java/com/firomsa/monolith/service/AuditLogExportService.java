package com.firomsa.monolith.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.firomsa.monolith.dto.AuditLogFilterDTO;
import com.firomsa.monolith.dto.AuditLogResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogExportService {

    private final AuditLogQueryService auditLogQueryService;

    public byte[] exportToCsv(AuditLogFilterDTO filter) throws IOException {
        List<AuditLogResponseDTO> allLogs = getAllLogsForExport(filter);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {

            // Write CSV header
            writer.println("ID,Correlation ID,User ID,Username,Action,Resource Type,Resource ID," +
                    "Old Value,New Value,IP Address,User Agent,Timestamp,Status,Error Message");

            // Write CSV rows
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (AuditLogResponseDTO log : allLogs) {
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\",%s,%s,%s,%s,\"%s\"",
                        log.id(),
                        log.correlationId(),
                        log.userId() != null ? log.userId() : "",
                        escapeCsv(log.username()),
                        log.action(),
                        escapeCsv(log.resourceType()),
                        log.resourceId() != null ? log.resourceId() : "",
                        escapeCsv(log.oldValue()),
                        escapeCsv(log.newValue()),
                        escapeCsv(log.ipAddress()),
                        escapeCsv(log.userAgent()),
                        log.timestamp() != null ? log.timestamp().format(formatter) : "",
                        log.status(),
                        escapeCsv(log.errorMessage())));
            }

            writer.flush();
            return outputStream.toByteArray();
        }
    }

    public byte[] exportToJson(AuditLogFilterDTO filter) throws IOException {
        List<AuditLogResponseDTO> allLogs = getAllLogsForExport(filter);

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (int i = 0; i < allLogs.size(); i++) {
            AuditLogResponseDTO log = allLogs.get(i);
            jsonBuilder.append("  {\n");
            jsonBuilder.append(String.format("    \"id\": %d,\n", log.id()));
            jsonBuilder.append(String.format("    \"correlationId\": \"%s\",\n", log.correlationId()));
            jsonBuilder.append(
                    String.format("    \"userId\": %s,\n", log.userId() != null ? "\"" + log.userId() + "\"" : "null"));
            jsonBuilder.append(String.format("    \"username\": %s,\n",
                    log.username() != null ? "\"" + escapeJson(log.username()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"action\": \"%s\",\n", log.action()));
            jsonBuilder.append(String.format("    \"resourceType\": %s,\n",
                    log.resourceType() != null ? "\"" + escapeJson(log.resourceType()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"resourceId\": %s,\n",
                    log.resourceId() != null ? "\"" + log.resourceId() + "\"" : "null"));
            jsonBuilder.append(String.format("    \"oldValue\": %s,\n",
                    log.oldValue() != null ? "\"" + escapeJson(log.oldValue()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"newValue\": %s,\n",
                    log.newValue() != null ? "\"" + escapeJson(log.newValue()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"ipAddress\": %s,\n",
                    log.ipAddress() != null ? "\"" + escapeJson(log.ipAddress()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"userAgent\": %s,\n",
                    log.userAgent() != null ? "\"" + escapeJson(log.userAgent()) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"timestamp\": %s,\n",
                    log.timestamp() != null ? "\"" + log.timestamp().format(formatter) + "\"" : "null"));
            jsonBuilder.append(String.format("    \"status\": \"%s\",\n", log.status()));
            jsonBuilder.append(String.format("    \"errorMessage\": %s\n",
                    log.errorMessage() != null ? "\"" + escapeJson(log.errorMessage()) + "\"" : "null"));
            jsonBuilder.append(i < allLogs.size() - 1 ? "  }," : "  }\n");
        }

        jsonBuilder.append("]\n");
        return jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<AuditLogResponseDTO> getAllLogsForExport(AuditLogFilterDTO filter) {
        // For export, we need to fetch all pages
        // This is a simplified version - in production, you might want to limit the
        // export size
        // or use a streaming approach for large datasets
        AuditLogFilterDTO exportFilter = new AuditLogFilterDTO(
                filter.correlationId(),
                filter.userId(),
                filter.username(),
                filter.action(),
                filter.resourceType(),
                filter.status(),
                filter.startDate(),
                filter.endDate(),
                0, // page
                10000, // size - limit to prevent memory issues
                filter.sort());

        return auditLogQueryService.getAuditLogs(exportFilter).getContent();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
