package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Driver;
import com.drivex.entity.Vehicle;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.security.DriverUserDetailsService;
import com.drivex.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final DriverRepository         driverRepository;
    private final JwtService               jwtService;
    private final DriverUserDetailsService userDetailsService;
    private final AuthenticationManager    authManager;
    private final PasswordEncoder          passwordEncoder;

    // ── Register ──────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (driverRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already registered: " + req.email());
        }

        // Build vehicle entity
        var vr = req.vehicle();
        var vehicle = Vehicle.builder()
            .id(UUID.randomUUID().toString())
            .make(vr.make())
            .model(vr.model())
            .modelYear(vr.year())
            .licensePlate(vr.licensePlate())
            .color(vr.color())
            .type(vr.type())
            .build();

        // Build driver entity
        var driver = Driver.builder()
            .id(UUID.randomUUID().toString())
            .name(req.name())
            .email(req.email())
            .phone(req.phone())
            .passwordHash(passwordEncoder.encode(req.password()))
            .status(Driver.Status.OFFLINE)
            .build();

        vehicle.setDriver(driver);
        driver.setVehicle(vehicle);

        driverRepository.save(driver);
        log.info("New driver registered: {} ({})", driver.getName(), driver.getEmail());

        return buildAuthResponse(driver);
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException if invalid → handled by GlobalExceptionHandler
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        var driver = driverRepository.findByEmail(req.email())
            .orElseThrow(() -> ApiException.notFound("Driver", req.email()));

        log.info("Driver logged in: {}", req.email());
        return buildAuthResponse(driver);
    }

    // ── Refresh token ─────────────────────────────────────────────────────────
    public AuthResponse refresh(RefreshRequest req) {
        String email;
        try {
            email = jwtService.extractSubject(req.refreshToken());
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid refresh token");
        }

        if (jwtService.isExpired(req.refreshToken())) {
            throw ApiException.unauthorized("Refresh token has expired — please log in again");
        }

        var driver = driverRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.unauthorized("Driver not found"));

        log.debug("Token refreshed for: {}", email);
        return buildAuthResponse(driver);
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        var driver = driverRepository.findByEmail(req.email())
            .orElseThrow(() -> ApiException.notFound("Driver", req.email()));

        String token = UUID.randomUUID().toString();
        driver.setResetToken(token);
        driver.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        driverRepository.save(driver);

        log.info("Password reset token generated for: {}", req.email());
        return new MessageResponse("Reset link sent to " + req.email());
    }

    // ── Reset password ────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest req) {
        var driver = driverRepository.findByResetToken(req.token())
            .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));

        if (driver.getResetTokenExpiry() == null || driver.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Reset token has expired");
        }

        driver.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        driver.setResetToken(null);
        driver.setResetTokenExpiry(null);
        driverRepository.save(driver);

        log.info("Password reset completed for: {}", driver.getEmail());
        return buildAuthResponse(driver);
    }

    // ── Internal helper ───────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(Driver driver) {
        UserDetails user = userDetailsService.loadUserByUsername(driver.getEmail());
        String access  = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        return new AuthResponse(
            access, refresh,
            86400L,  // 24h in seconds
            DriverSummary.from(driver)
        );
    }
}
