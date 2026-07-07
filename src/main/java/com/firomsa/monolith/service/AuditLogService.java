package com.firomsa.monolith.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditLog;
import com.firomsa.monolith.model.AuditStatus;
import com.firomsa.monolith.repository.AuditLogRepository;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.model.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;

    @Async
    public void logAudit(AuditAction action, String resourceType, UUID resourceId,
            Object oldValue, Object newValue, AuditStatus status, String errorMessage) {

        if (!isAuditLoggingEnabled()) {
            return;
        }

        try {
            AuditLog auditLog = buildAuditLog(action, resourceType, resourceId,
                    oldValue, newValue, status, errorMessage);

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved successfully for action: {} on resource: {}",
                    action, resourceType);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    private AuditLog buildAuditLog(AuditAction action, String resourceType, UUID resourceId,
            Object oldValue, Object newValue, AuditStatus status, String errorMessage) {

        String correlationId = MDC.get("correlationId");
        UUID correlationIdUuid = correlationId != null ? UUID.fromString(correlationId) : UUID.randomUUID();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        String username = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            }

            if (username != null) {
                userId = userRepository.findByEmail(username)
                        .map(User::getId)
                        .orElse(null);
            }
        }

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request != null ? getClientIpAddress(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        return AuditLog.builder()
                .correlationId(correlationIdUuid)
                .userId(userId)
                .username(username)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .oldValue(serializeToJson(oldValue))
                .newValue(serializeToJson(newValue))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    private String serializeToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return object.toString();
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isAuditLoggingEnabled() {
        return Boolean.parseBoolean(
                environment.getProperty("audit.logging.enabled", "true"));
    }

    @Transactional
    public void deleteAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        int deletedCount = auditLogRepository.deleteByTimestampBetween(startDate, endDate);
        log.info("Deleted {} audit logs between {} and {}", deletedCount, startDate, endDate);
    }
}
