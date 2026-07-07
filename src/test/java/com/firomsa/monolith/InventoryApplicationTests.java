package com.firomsa.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.firomsa.monolith.support.SharedContainers;

@SpringBootTest
@Testcontainers
class monolithApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = SharedContainers.POSTGRES;

    @Test
    void contextLoads() {
    }
}
