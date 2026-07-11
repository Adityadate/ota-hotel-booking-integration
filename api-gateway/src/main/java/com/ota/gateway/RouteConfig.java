package com.ota.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator apiRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Hotels - inventory and availability operations
                .route("hotel-service-hotels",
                        r -> r.path("/api/v1/hotels/**")
                                .uri("lb://hotel-service"))
                // Bookings - guest bookings and reservations
                .route("booking-service-bookings",
                        r -> r.path("/api/v1/bookings/**")
                                .uri("lb://booking-service"))
                // OTA sync - third-party provider integration
                .route("ota-integration-sync",
                        r -> r.path("/api/v1/sync/**")
                                .uri("lb://ota-integration-service"))
                .build();
    }
}
