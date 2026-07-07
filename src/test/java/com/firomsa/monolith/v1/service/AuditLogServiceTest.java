package com.firomsa.monolith.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.firomsa.monolith.model.AuditAction;
import com.firomsa.monolith.model.AuditLog;
import com.firomsa.monolith.model.AuditStatus;
import com.firomsa.monolith.repository.AuditLogRepository;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.service.AuditLogService;
import com.firomsa.monolith.model.User;
import java.util.Optional;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Environment environment;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLogAuditWhenEnabled() {
        when(environment.getProperty("audit.logging.enabled", "true")).thenReturn("true");

        auditLogService.logAudit(AuditAction.CREATE, "Product", UUID.randomUUID(),
                null, null, AuditStatus.SUCCESS, null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testLogAuditWhenDisabled() {
        when(environment.getProperty("audit.logging.enabled", "true")).thenReturn("false");

        auditLogService.logAudit(AuditAction.CREATE, "Product", UUID.randomUUID(),
                null, null, AuditStatus.SUCCESS, null);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void testLogAuditWithException() {
        when(environment.getProperty("audit.logging.enabled", "true")).thenReturn("true");

        auditLogService.logAudit(AuditAction.CREATE, "Product", UUID.randomUUID(),
                "oldValue", "newValue", AuditStatus.SUCCESS, null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testDeleteAuditLogsByDateRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now();

        auditLogService.deleteAuditLogsByDateRange(startDate, endDate);

        verify(auditLogRepository).deleteByTimestampBetween(eq(startDate), eq(endDate));
        verify(auditLogRepository, never()).findByTimestampBefore(any());
    }

    @Test
    void testLogAuditSetsUserIdFromAuthentication() {
        when(environment.getProperty("audit.logging.enabled", "true")).thenReturn("true");

        String username = "test@example.com";
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder().id(userId).email(username).build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail(username)).thenReturn(Optional.of(mockUser));

        auditLogService.logAudit(AuditAction.CREATE, "Product", UUID.randomUUID(),
                null, null, AuditStatus.SUCCESS, null);

        org.mockito.ArgumentCaptor<AuditLog> captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(username, captor.getValue().getUsername());
    }
}
