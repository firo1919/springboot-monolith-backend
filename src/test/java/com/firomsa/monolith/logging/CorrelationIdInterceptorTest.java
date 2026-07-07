package com.firomsa.monolith.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class CorrelationIdInterceptorTest {

    @Mock
    private MockHttpServletRequest request;

    @Mock
    private MockHttpServletResponse response;

    @InjectMocks
    private CorrelationIdInterceptor correlationIdInterceptor;

    @Test
    void testPreHandleWithExistingCorrelationId() {
        MDC.put("correlationId", "test-correlation-id");

        boolean result = correlationIdInterceptor.preHandle(request, response, null);

        assertEquals(true, result);
        assertEquals("test-correlation-id", MDC.get("correlationId"));
        MDC.clear();
    }

    @Test
    void testPreHandleWithMissingCorrelationId() {
        MDC.clear();

        boolean result = correlationIdInterceptor.preHandle(request, response, null);

        assertEquals(true, result);
        assertEquals("MISSING", MDC.get("correlationId"));
        MDC.clear();
    }

    @Test
    void testPreHandleWithBlankCorrelationId() {
        MDC.put("correlationId", "   ");

        boolean result = correlationIdInterceptor.preHandle(request, response, null);

        assertEquals(true, result);
        assertEquals("MISSING", MDC.get("correlationId"));
        MDC.clear();
    }
}
