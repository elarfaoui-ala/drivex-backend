package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Driver;
import com.drivex.entity.Vehicle;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.websocket.LocationBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository   driverRepository;
    private final LocationBroadcaster broadcaster;
    private final PasswordEncoder     passwordEncoder;

    // ── Get profile ───────────────────────────────────────────────────────────
    @Cacheable(value = "driver:profile", key = "#driverId")
    public DriverSummary getProfile(String driverId) {
        return DriverSummary.from(findOrThrow(driverId));
    }

    // ── Update status (ONLINE / OFFLINE / BREAK) ──────────────────────────────
    @Transactional
    @CacheEvict(value = "driver:profile", key = "#driverId")
    public DriverSummary updateStatus(String driverId, StatusUpdateRequest req) {
        int updated = driverRepository.updateStatus(driverId, req.status());
        if (updated == 0) throw ApiException.notFound("Driver", driverId);

        broadcaster.broadcastGlobal(
            "drivers/" + driverId + "/status",
            "DRIVER_STATUS_CHANGED",
            java.util.Map.of("driverId", driverId, "status", req.status().name())
        );

        log.info("Driver {} status → {}", driverId, req.status());
        return DriverSummary.from(findOrThrow(driverId));
    }

    // ── Update GPS location ───────────────────────────────────────────────────
    @Transactional
    public void updateLocation(String driverId, LocationUpdateRequest req) {
        int updated = driverRepository.updateLocation(
            driverId, req.lat(), req.lng(), LocalDateTime.now());
        if (updated == 0) throw ApiException.notFound("Driver", driverId);

        // Push to WebSocket subscribers
        var payload = new LocationPayload(
            driverId, req.lat(), req.lng(), null, null, LocalDateTime.now());
        broadcaster.broadcastDriverLocation(payload);

        log.debug("Driver {} location → {},{}", driverId, req.lat(), req.lng());
    }

    // ── List all online drivers ────────────────────────────────────────────────
    @Cacheable(value = "drivers:online", unless = "#result.isEmpty()")
    public List<DriverSummary> getOnlineDrivers() {
        return driverRepository.findByStatus(Driver.Status.ONLINE)
            .stream()
            .map(DriverSummary::from)
            .toList();
    }

    // ── Get all drivers (admin) ───────────────────────────────────────────────
    public List<DriverSummary> getAllDrivers() {
        return driverRepository.findAll()
            .stream()
            .map(DriverSummary::from)
            .toList();
    }

    // ── Update profile ────────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "driver:profile", key = "#driverId")
    public DriverSummary updateProfile(String driverId, UpdateProfileRequest req) {
        Driver driver = findOrThrow(driverId);

        if (!driver.getEmail().equals(req.email()) && driverRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already in use: " + req.email());
        }

        driver.setName(req.name());
        driver.setEmail(req.email());
        driver.setPhone(req.phone());
        driverRepository.save(driver);

        log.info("Driver {} profile updated", driverId);
        return DriverSummary.from(driver);
    }

    // ── Change password ───────────────────────────────────────────────────────
    @Transactional
    public MessageResponse changePassword(String driverId, ChangePasswordRequest req) {
        Driver driver = findOrThrow(driverId);

        if (!passwordEncoder.matches(req.currentPassword(), driver.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }

        driver.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        driverRepository.save(driver);

        log.info("Driver {} password changed", driverId);
        return new MessageResponse("Password updated successfully");
    }

    // ── Get vehicle ───────────────────────────────────────────────────────────
    public VehicleSummary getVehicle(String driverId) {
        Driver driver = findOrThrow(driverId);
        if (driver.getVehicle() == null) {
            throw ApiException.notFound("Vehicle", driverId);
        }
        return VehicleSummary.from(driver.getVehicle());
    }

    // ── Register / update vehicle ─────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "driver:profile", key = "#driverId")
    public VehicleSummary saveVehicle(String driverId, VehicleRequest req) {
        Driver driver = findOrThrow(driverId);

        Vehicle vehicle = driver.getVehicle();
        if (vehicle == null) {
            vehicle = Vehicle.builder()
                .id(UUID.randomUUID().toString())
                .driver(driver)
                .build();
        }

        vehicle.setMake(req.make());
        vehicle.setModel(req.model());
        vehicle.setModelYear(req.year());
        vehicle.setLicensePlate(req.licensePlate());
        vehicle.setColor(req.color());
        vehicle.setType(req.type());

        driver.setVehicle(vehicle);
        driverRepository.save(driver);

        log.info("Driver {} vehicle {}: {} {} ({})", driverId,
            driver.getVehicle() == null ? "registered" : "updated",
            req.make(), req.model(), req.licensePlate());

        return VehicleSummary.from(vehicle);
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    Driver findOrThrow(String id) {
        return driverRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Driver", id));
    }
}
