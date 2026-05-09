package com.drivex.websocket;

import com.drivex.dto.Dtos.OrderSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * OrderEventPublisher — pushes order lifecycle events over WebSocket.
 *
 * Topics:
 *   /topic/orders/new              → all online drivers (new available order)
 *   /topic/orders/{id}/status      → status changes (accepted, picked up, etc.)
 *   /user/{driverId}/queue/events  → driver-specific events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final LocationBroadcaster broadcaster;

    @Async
    public void publishNewOrder(OrderSummary order) {
        broadcaster.broadcastGlobal("orders/new", "ORDER_NEW", order);
        log.info("Published new order: {}", order.orderNumber());
    }

    @Async
    public void publishStatusChanged(OrderSummary order) {
        broadcaster.broadcastGlobal(
            "orders/" + order.id() + "/status",
            "ORDER_STATUS_CHANGED",
            order
        );
        // Also notify the assigned driver personally
        if (order.driverId() != null) {
            broadcaster.broadcastToDriver(order.driverId(), "ORDER_STATUS_CHANGED", order);
        }
        log.info("Order {} status → {}", order.orderNumber(), order.status());
    }

    @Async
    public void publishOrderAssigned(String driverId, OrderSummary order) {
        broadcaster.broadcastToDriver(driverId, "ORDER_ASSIGNED", order);
        log.info("Order {} assigned to driver {}", order.orderNumber(), driverId);
    }

    @Async
    public void publishOrderCancelled(OrderSummary order) {
        broadcaster.broadcastGlobal(
            "orders/" + order.id() + "/status",
            "ORDER_CANCELLED",
            order
        );
        if (order.driverId() != null) {
            broadcaster.broadcastToDriver(order.driverId(), "ORDER_CANCELLED", order);
        }
    }
}
