package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.firomsa.monolith.config.BootstrapConfig;
import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.RefreshTokenRepository;
import com.firomsa.monolith.repository.UserRepository;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@AutoConfigureRestTestClient
@SuppressWarnings("resource")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractE2ETest {
    protected static final String AUTH_BASE_URL = "/api/v1/auth";
    protected static final String ADMIN_BASE_URL = "/api/v1/admin";
    protected static final String DEFAULT_PASSWORD = "password123";
    private static final String RUSTFS_ACCESS_KEY = "rustfsadmin";
    private static final String RUSTFS_SECRET_KEY = "rustfsadmin";
    private static final String TEST_BUCKET = "test-bucket";

    @ServiceConnection
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    private static final GenericContainer<?> mailhog = new GenericContainer<>("mailhog/mailhog:latest")
            .withExposedPorts(1025, 8025);

    private static final GenericContainer<?> s3 = new GenericContainer<>("rustfs/rustfs:latest")
            .withExposedPorts(9000, 9001).withEnv("RUSTFS_ACCESS_KEY", RUSTFS_ACCESS_KEY)
            .withEnv("RUSTFS_SECRET_KEY", RUSTFS_SECRET_KEY);

    protected RestTestClient client;

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BootstrapConfig bootstrapConfig;

    private static void ensureBucketExists() {
        URI endpoint = URI.create("http://" + s3.getHost() + ":" + s3.getMappedPort(9000));

        try (S3Client s3Client = S3Client.builder().endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create(RUSTFS_ACCESS_KEY, RUSTFS_SECRET_KEY)))
                .region(Region.US_EAST_1).serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(TEST_BUCKET).build());
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) {
                    s3Client.createBucket(
                            CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
                } else {
                    throw exception;
                }
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        Startables.deepStart(Stream.of(postgres, mailhog, s3)).join();
        ensureBucketExists();

        registry.add("spring.mail.host", mailhog::getHost);
        registry.add("spring.mail.port", () -> mailhog.getMappedPort(1025));
        registry.add("spring.cloud.aws.s3.endpoint",
                () -> "http://" + s3.getHost() + ":" + s3.getMappedPort(9000));
        registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> true);
        registry.add("spring.cloud.aws.credentials.access-key", () -> RUSTFS_ACCESS_KEY);
        registry.add("spring.cloud.aws.credentials.secret-key", () -> RUSTFS_SECRET_KEY);
        registry.add("s3.bucket-name", () -> TEST_BUCKET);
    }

    @BeforeEach
    void setUpClient() {
        this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterEach
    void tearDownBaseData() {
        refreshTokenRepository.deleteAllInBatch();
        confirmationOtpRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    protected String authorizationHeader(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected String adminEmailForSuffix(String suffix) {
        return "admin." + suffix + "@example.com";
    }

    protected String employeeEmailForSuffix(String suffix) {
        return "employee_" + suffix + "@example.com";
    }

    protected String registerAdminPayload(String suffix) {
        return registerAdminPayload(suffix, bootstrapConfig.getToken());
    }

    protected String registerAdminPayload(String suffix, String bootstrapToken) {
        return """
                {
                    "firstName": "Admin",
                    "lastName": "User",
                    "username": "admin_%s",
                    "password": "%s",
                    "email": "admin.%s@example.com",
                    "phone": "+251900%s",
                    "bootstrapToken": "%s"
                }
                """.formatted(suffix, DEFAULT_PASSWORD, suffix, suffix.substring(0, 6),
                bootstrapToken);
    }

    protected String registerEmployeePayload(String suffix) {
        return """
                {
                    "firstName": "EmpFirst%s",
                    "lastName": "EmpLast%s",
                    "username": "employee_%s",
                    "password": "%s",
                    "email": "employee_%s@example.com",
                    "role": "EMPLOYEE",
                    "phone": "+251911%s"
                }
                """.formatted(suffix, suffix, suffix, DEFAULT_PASSWORD, suffix,
                suffix.substring(0, 6));
    }

    protected String latestOtpForEmail(String email) {
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        return confirmationOtpRepository.findAll().stream()
                .filter(otp -> otp.getUser() != null && userId.equals(otp.getUser().getId()))
                .map(otp -> otp.getOtp()).reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No OTP found for user " + email));
    }

    protected String extractAccessToken(String body) {
        return extractJsonString(body, "accessToken");
    }

    protected String extractJsonString(String body, String field) {
        String marker = "\"" + field + "\":\"";
        int start = body.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int from = start + marker.length();
        int end = body.indexOf('"', from);
        assertThat(end).isGreaterThan(from);
        return body.substring(from, end);
    }

    protected String loginByEmail(String email) {
        String payload = "{\"password\":\"" + DEFAULT_PASSWORD + "\",\"email\":\"" + email + "\"}";
        var response = client.post().uri(AUTH_BASE_URL + "/login").contentType(APPLICATION_JSON)
                .body(payload).exchange();
        response.expectStatus().isOk();
        return extractAccessToken(response.returnResult(String.class).getResponseBody());
    }

    @Test
    void verify() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
        assertThat(mailhog.isCreated()).isTrue();
        assertThat(mailhog.isRunning()).isTrue();
        assertThat(s3.isCreated()).isTrue();
        assertThat(s3.isRunning()).isTrue();
    }
}
