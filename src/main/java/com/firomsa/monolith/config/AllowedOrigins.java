package com.firomsa.monolith.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "cors")
public class AllowedOrigins {
    private List<String> origins;
}
