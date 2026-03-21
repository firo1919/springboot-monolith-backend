package com.firomsa.monolith.v1.controller.integrationTest;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.firomsa.monolith.v1.service.EmailService;
import com.firomsa.monolith.v1.service.StorageService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@SuppressWarnings("resource")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    @Container
    protected static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @MockitoBean
    protected StorageService storageService;

    @MockitoBean
    protected EmailService emailService;
}
