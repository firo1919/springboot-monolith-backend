package com.firomsa.monolith.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cache.ttl")
@Validated
public class CacheProperties {

    @Positive
    private long employees = 5;
}
