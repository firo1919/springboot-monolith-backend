package com.firomsa.monolith.actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.firomsa.monolith.config.S3Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3HealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final S3Config s3Config;

    @Override
    public Health health() {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(s3Config.getBucketName())
                    .maxKeys(1)
                    .build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            return Health.up()
                    .withDetail("bucket", s3Config.getBucketName())
                    .withDetail("objectsFound", response.keyCount())
                    .build();
        } catch (S3Exception e) {
            log.error("S3 health check failed", e);
            return Health.down()
                    .withDetail("bucket", s3Config.getBucketName())
                    .withDetail("error", e.getMessage())
                    .withDetail("statusCode", e.statusCode())
                    .build();
        } catch (Exception e) {
            log.error("S3 health check failed with unexpected error", e);
            return Health.down()
                    .withDetail("bucket", s3Config.getBucketName())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
