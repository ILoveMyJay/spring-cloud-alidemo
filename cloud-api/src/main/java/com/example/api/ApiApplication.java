package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.api.config.SecurityProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(SecurityProperties.class)
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
} 