package com.firomsa.monolith.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "s3")
@Validated
public class S3Config {
    @NotBlank
    private String bucketName;
    @Positive
    private int getLinkExpiryMinutes;
    @Positive
    private int uploadLinkExpiryMinutes;
}
