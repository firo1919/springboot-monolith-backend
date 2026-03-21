package com.firomsa.monolith.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapConfig {
    private String token;
}
