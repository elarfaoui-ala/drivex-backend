package com.drivex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching      // Redis cache enabled
@EnableAsync        // Async for WebSocket broadcasts
@EnableScheduling   // Scheduled tasks (location cleanup, etc.)
public class DrivexApplication {
    public static void main(String[] args) {
        SpringApplication.run(DrivexApplication.class, args);
    }
}
