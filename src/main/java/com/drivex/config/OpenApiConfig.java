package com.drivex.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "DriveX Driver API",
        version     = "1.0",
        description = """
            Backend API for the DriveX delivery driver mobile app.
            
            **Quick start:**
            1. `POST /api/v1/auth/login` with `alex@drivex.com` / `password123`
            2. Copy the `accessToken` → click **Authorize** button → paste as `Bearer <token>`
            3. Explore all endpoints
            
            **WebSocket:** Connect to `ws://localhost:8080/ws` (SockJS)
            with STOMP header `Authorization: Bearer <token>`
            """,
        contact = @Contact(name = "DriveX Team", email = "dev@drivex.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local dev (H2)"),
        @Server(url = "https://api.drivex.com", description = "Production (PostgreSQL)")
    }
)
@SecurityScheme(
    name   = "bearerAuth",
    type   = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description  = "Paste your JWT access token here (without the 'Bearer ' prefix)"
)
public class OpenApiConfig {}
