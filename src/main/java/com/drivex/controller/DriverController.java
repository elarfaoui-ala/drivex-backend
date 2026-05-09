package com.drivex.controller;

import com.drivex.dto.Dtos.*;
import com.drivex.service.DriverService;
import com.drivex.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver profile, status, and location management")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService  driverService;
    private final LocationService locationService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/{driverId}")
    @Operation(summary = "Get driver profile")
    public DriverSummary getProfile(@PathVariable String driverId) {
        return driverService.getProfile(driverId);
    }

    @GetMapping
    @Operation(summary = "List all drivers")
    public List<DriverSummary> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/online")
    @Operation(summary = "List currently online drivers")
    public List<DriverSummary> getOnlineDrivers() {
        return driverService.getOnlineDrivers();
    }

    // ── Status ────────────────────────────────────────────────────────────────

    @PatchMapping("/{driverId}/status")
    @Operation(summary = "Update driver status (ONLINE / OFFLINE / BREAK)")
    public DriverSummary updateStatus(
        @PathVariable String driverId,
        @Valid @RequestBody StatusUpdateRequest req
    ) {
        return driverService.updateStatus(driverId, req);
    }

    // ── Location (REST ping fallback) ─────────────────────────────────────────

    @PostMapping("/{driverId}/location")
    @Operation(summary = "Update driver GPS location (REST fallback — prefer WebSocket)")
    public ResponseEntity<MessageResponse> updateLocation(
        @PathVariable String driverId,
        @Valid @RequestBody LocationUpdateRequest req
    ) {
        locationService.handleRestLocationUpdate(driverId, req);
        return ResponseEntity.ok(new MessageResponse("Location updated"));
    }

    @GetMapping("/{driverId}/location")
    @Operation(summary = "Get last known driver location (from Redis cache)")
    public ResponseEntity<LocationPayload> getLocation(@PathVariable String driverId) {
        LocationPayload loc = locationService.getLastKnownLocation(driverId);
        if (loc == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(loc);
    }
}
