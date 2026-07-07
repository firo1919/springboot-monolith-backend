package com.firomsa.monolith.v1.controller.integrationTest;

import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.firomsa.monolith.support.SharedContainers;
import com.firomsa.monolith.v1.service.EmailService;
import com.firomsa.monolith.v1.service.StorageService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@SuppressWarnings("resource")
@Isolated
public abstract class AbstractIntegrationTest {

    // One Postgres shared by all integration and e2e tests (see SharedContainers).
    // Wiring it through
    // @ServiceConnection lets Spring Boot point the datasource at the
    // already-running container.
    @ServiceConnection
    protected static final PostgreSQLContainer postgres = SharedContainers.POSTGRES;

    @MockitoBean
    protected StorageService storageService;

    @MockitoBean
    protected EmailService emailService;
}
