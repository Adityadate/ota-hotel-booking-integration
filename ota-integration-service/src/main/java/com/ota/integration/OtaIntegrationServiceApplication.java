package com.ota.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ota")
@EnableFeignClients
@EnableRetry
@EnableScheduling
public class OtaIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtaIntegrationServiceApplication.class, args);
    }
}
