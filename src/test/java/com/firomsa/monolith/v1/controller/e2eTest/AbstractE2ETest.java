package com.firomsa.monolith.v1.controller.e2eTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
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
    private static final String RUSTFS_ACCESS_KEY = "rustfsadmin";
    private static final String RUSTFS_SECRET_KEY = "rustfsadmin";
    private static final String TEST_BUCKET = "test-bucket";

    @ServiceConnection
    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    private static final GenericContainer<?> mailhog =
            new GenericContainer<>("mailhog/mailhog:latest").withExposedPorts(1025, 8025);

    private static final GenericContainer<?> s3 = new GenericContainer<>("rustfs/rustfs:latest")
            .withExposedPorts(9000, 9001).withEnv("RUSTFS_ACCESS_KEY", RUSTFS_ACCESS_KEY)
            .withEnv("RUSTFS_SECRET_KEY", RUSTFS_SECRET_KEY);

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
