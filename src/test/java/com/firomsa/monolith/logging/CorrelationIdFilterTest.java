package com.firomsa.monolith.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    @Test
    void testGenerateNewCorrelationIdWhenNotProvided() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        UUID.fromString(correlationId); // Verify it's a valid UUID
        verify(filterChain).doFilter(eq(request), eq(response));
    }

    @Test
    void testUseExistingCorrelationIdWhenProvided() throws ServletException, java.io.IOException {
        String existingId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertEquals(existingId, correlationId);
        verify(filterChain).doFilter(eq(request), eq(response));
    }

    @Test
    void testGenerateNewCorrelationIdWhenBlankProvided() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assert !correlationId.isBlank();
        verify(filterChain).doFilter(eq(request), eq(response));
    }
}
