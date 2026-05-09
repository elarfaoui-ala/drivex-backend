package com.drivex;

import com.drivex.config.TestRedisConfig;
import com.drivex.dto.Dtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DrivexAuthIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;

    static String accessToken;

    // ── Login with seeded driver ──────────────────────────────────────────────
    @Test
    @Order(1)
    void loginWithSeededDriver_shouldReturn200AndToken() throws Exception {
        var req = new LoginRequest("alex@drivex.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.driver.email").value("alex@drivex.com"))
            .andReturn();

        String body  = result.getResponse().getContentAsString();
        AuthResponse auth = objectMapper.readValue(body, AuthResponse.class);
        accessToken = auth.accessToken();
    }

    // ── Wrong password ────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void loginWithWrongPassword_shouldReturn401() throws Exception {
        var req = new LoginRequest("alex@drivex.com", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ── Protected endpoint without token ─────────────────────────────────────
    @Test
    @Order(3)
    void getProfile_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/drv-0001"))
            .andExpect(status().isForbidden());
    }

    // ── Protected endpoint with valid token ───────────────────────────────────
    @Test
    @Order(4)
    void getProfile_withValidToken_shouldReturn200() throws Exception {
        Assumptions.assumeTrue(accessToken != null, "Access token must be set by login test");

        mockMvc.perform(get("/api/v1/drivers/drv-0001")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("drv-0001"))
            .andExpect(jsonPath("$.email").value("alex@drivex.com"));
    }

    // ── Available orders (public) ─────────────────────────────────────────────
    @Test
    @Order(5)
    void getAvailableOrders_noAuth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ── Validation error ──────────────────────────────────────────────────────
    @Test
    @Order(6)
    void loginWithInvalidEmail_shouldReturn400WithValidationErrors() throws Exception {
        var req = new LoginRequest("not-an-email", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.validationErrors.email").exists());
    }
}
