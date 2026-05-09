package com.drivex.websocket;

import com.drivex.dto.Dtos.LocationPayload;
import com.drivex.dto.Dtos.WsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * LocationBroadcaster — pushes real-time driver location to:
 *   /topic/drivers/{driverId}/location   → dispatcher / admin view
 *   /topic/orders/{orderId}/tracking     → customer tracking (future)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationBroadcaster {

    private final SimpMessagingTemplate messaging;

    @Async
    public void broadcastDriverLocation(LocationPayload payload) {
        String dest = "/topic/drivers/" + payload.driverId() + "/location";
        messaging.convertAndSend(dest, new WsEvent("DRIVER_LOCATION", payload));
        log.debug("Location broadcast → {} [{}, {}]", dest, payload.lat(), payload.lng());
    }

    @Async
    public void broadcastToDriver(String driverId, String eventType, Object data) {
        // Personal queue — only visible to authenticated driver
        messaging.convertAndSendToUser(
            driverId,
            "/queue/events",
            new WsEvent(eventType, data)
        );
        log.debug("WS event {} → driver {}", eventType, driverId);
    }

    @Async
    public void broadcastGlobal(String topic, String eventType, Object data) {
        messaging.convertAndSend("/topic/" + topic, new WsEvent(eventType, data));
        log.debug("WS broadcast {} → /topic/{}", eventType, topic);
    }
}
