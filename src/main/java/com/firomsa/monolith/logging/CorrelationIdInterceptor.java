package com.firomsa.monolith.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

        if (correlationId == null || correlationId.isBlank()) {
            MDC.put(CORRELATION_ID_MDC_KEY, "MISSING");
        }

        return true;
    }
}
