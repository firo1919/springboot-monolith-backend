package com.firomsa.monolith.aspect;

import java.lang.reflect.Method;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditStatus;
import com.firomsa.monolith.service.AuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object auditControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String resourceType = extractResourceType(method);
        AuditAction action = extractAction(method);

        if (action == null || resourceType == null) {
            return joinPoint.proceed();
        }

        UUID resourceId = extractResourceId(joinPoint.getArgs());
        Object oldValue = null;
        Object newValue = null;

        try {
            Object result = joinPoint.proceed();

            auditLogService.logAudit(action, resourceType, resourceId, oldValue, newValue,
                    AuditStatus.SUCCESS, null);

            return result;
        } catch (Exception e) {
            auditLogService.logAudit(action, resourceType, resourceId, oldValue, newValue,
                    AuditStatus.FAILURE, e.getMessage());
            throw e;
        }
    }

    private String extractResourceType(Method method) {
        RequestMapping classMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            String path = classMapping.value()[0];
            return extractResourceFromPath(path);
        }
        return null;
    }

    private AuditAction extractAction(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) {
            return AuditAction.CREATE;
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            return AuditAction.READ;
        } else if (method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)) {
            return AuditAction.UPDATE;
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            return AuditAction.DELETE;
        }
        return null;
    }

    private String extractResourceFromPath(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty() && !parts[i].startsWith("{") && !parts[i].equals("api") && !parts[i].equals("v1")) {
                return parts[i];
            }
        }
        return "UNKNOWN";
    }

    private UUID extractResourceId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
            if (arg instanceof String) {
                try {
                    return UUID.fromString((String) arg);
                } catch (IllegalArgumentException e) {
                    // Not a UUID, continue
                }
            }
        }
        return null;
    }
}
