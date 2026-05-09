package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.websocket.LocationBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * LocationService — two entry points for location updates:
 *   1. REST  POST /api/v1/drivers/{id}/location  (polling fallback)
 *   2. STOMP /app/location                       (WebSocket, preferred)
 *
 * Redis stores the last known position with 10-minute TTL.
 * The position is also persisted to H2/PostgreSQL every N pings (configurable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private static final String REDIS_KEY_PREFIX  = "driver:location:";
    private static final Duration LOCATION_TTL    = Duration.ofMinutes(10);
    private static final int      PERSIST_EVERY_N = 5;  // write to DB every 5 pings

    private final DriverRepository   driverRepository;
    private final LocationBroadcaster broadcaster;
    private final RedisTemplate<String, Object> redisTemplate;

    // Ping counters per driver (in-memory, reset on restart — acceptable)
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> pingCount =
        new java.util.concurrent.ConcurrentHashMap<>();

    // ── REST ping (used when WebSocket is not available) ──────────────────────
    @Transactional
    public void handleRestLocationUpdate(String driverId, LocationUpdateRequest req) {
        if (!driverRepository.existsById(driverId)) {
            throw ApiException.notFound("Driver", driverId);
        }

        var payload = new LocationPayload(
            driverId, req.lat(), req.lng(), null, null, LocalDateTime.now());

        cacheAndBroadcast(driverId, payload);
        maybePersistedToDb(driverId, req);
    }

    // ── WebSocket ping (/app/location) ────────────────────────────────────────
    public void handleWsLocationUpdate(LocationPayload payload) {
        var updateReq = new LocationUpdateRequest(payload.lat(), payload.lng());
        cacheAndBroadcast(payload.driverId(), payload);
        maybePersistedToDb(payload.driverId(), updateReq);
    }

    // ── Get last known position from Redis ────────────────────────────────────
    public LocationPayload getLastKnownLocation(String driverId) {
        Object cached = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + driverId);
        if (cached instanceof LocationPayload loc) return loc;

        // Fallback to DB
        return driverRepository.findById(driverId)
            .filter(d -> d.getCurrentLat() != null)
            .map(d -> new LocationPayload(
                driverId, d.getCurrentLat(), d.getCurrentLng(),
                null, null, d.getLastSeenAt()))
            .orElse(null);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void cacheAndBroadcast(String driverId, LocationPayload payload) {
        // Store in Redis with TTL
        redisTemplate.opsForValue().set(
            REDIS_KEY_PREFIX + driverId, payload, LOCATION_TTL);

        // Broadcast to WebSocket subscribers
        broadcaster.broadcastDriverLocation(payload);
    }

    private void maybePersistedToDb(String driverId, LocationUpdateRequest req) {
        int count = pingCount.merge(driverId, 1, Integer::sum);
        if (count % PERSIST_EVERY_N == 0) {
            driverRepository.updateLocation(
                driverId, req.lat(), req.lng(), LocalDateTime.now());
            log.debug("Location persisted to DB for driver {} (ping #{})", driverId, count);
        }
    }
}

// ── WebSocket message handler (STOMP /app/location) ──────────────────────────
// Kept in same file to co-locate location logic
@Controller
class LocationWsHandler {

    private final LocationService locationService;

    LocationWsHandler(LocationService locationService) {
        this.locationService = locationService;
    }

    @MessageMapping("/location")
    public void receiveLocation(@Payload LocationPayload payload) {
        locationService.handleWsLocationUpdate(payload);
    }
}
