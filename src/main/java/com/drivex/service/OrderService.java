package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Driver;
import com.drivex.entity.Order;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.repository.OrderRepository;
import com.drivex.websocket.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository   orderRepository;
    private final DriverRepository  driverRepository;
    private final OrderEventPublisher eventPublisher;

    // ── Get available orders (NEW, not yet assigned) ──────────────────────────
    @Cacheable(value = "orders:available", unless = "#result.isEmpty()")
    public List<OrderSummary> getAvailableOrders() {
        return orderRepository.findByStatus(Order.Status.NEW)
            .stream()
            .map(OrderSummary::from)
            .toList();
    }

    // ── Get order by ID ───────────────────────────────────────────────────────
    @Cacheable(value = "order:detail", key = "#orderId")
    public OrderDetail getOrderDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ApiException.notFound("Order", orderId));
        return OrderDetail.from(order);
    }

    // ── Get driver's order history (paginated) ────────────────────────────────
    public PagedResponse<OrderSummary> getDriverOrders(
        String driverId, Order.Status status, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());

        Page<Order> result = (status != null)
            ? orderRepository.findByDriverIdAndStatus(driverId, status, pageable)
            : orderRepository.findByDriverId(driverId, pageable);

        List<OrderSummary> content = result.getContent()
            .stream()
            .map(OrderSummary::from)
            .toList();

        return new PagedResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    // ── Driver accepts an order ───────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = {"orders:available", "order:detail"}, allEntries = true)
    public OrderSummary acceptOrder(String orderId, String driverId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ApiException.notFound("Order", orderId));

        if (order.getStatus() != Order.Status.NEW) {
            throw ApiException.conflict(
                "Order " + order.getOrderNumber() + " is no longer available (status: " + order.getStatus() + ")");
        }

        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> ApiException.notFound("Driver", driverId));

        if (driver.getStatus() != Driver.Status.ONLINE) {
            throw ApiException.badRequest("Driver must be ONLINE to accept orders");
        }

        order.setDriver(driver);
        order.setStatus(Order.Status.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        orderRepository.save(order);

        var summary = OrderSummary.from(order);
        eventPublisher.publishOrderAssigned(driverId, summary);
        eventPublisher.publishStatusChanged(summary);

        log.info("Order {} accepted by driver {}", order.getOrderNumber(), driver.getName());
        return summary;
    }

    // ── Advance order status ──────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "order:detail", key = "#orderId")
    public OrderSummary updateStatus(String orderId, String driverId, OrderStatusUpdateRequest req) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ApiException.notFound("Order", orderId));

        // Only the assigned driver can change status
        if (order.getDriver() == null || !order.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("You are not assigned to this order");
        }

        validateTransition(order.getStatus(), req.status());

        order.setStatus(req.status());
        applyStatusTimestamp(order, req.status());
        orderRepository.save(order);

        // On delivery — update driver stats
        if (req.status() == Order.Status.DELIVERED) {
            updateDriverStatsOnDelivery(driverId, order);
        }

        var summary = OrderSummary.from(order);
        eventPublisher.publishStatusChanged(summary);

        if (req.status() == Order.Status.CANCELLED) {
            eventPublisher.publishOrderCancelled(summary);
        }

        log.info("Order {} → {} (driver: {})", order.getOrderNumber(), req.status(), driverId);
        return summary;
    }

    // ── Cancel an order ───────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = {"orders:available", "order:detail"}, allEntries = true)
    public OrderSummary cancelOrder(String orderId, String driverId) {
        return updateStatus(orderId, driverId,
            new OrderStatusUpdateRequest(Order.Status.CANCELLED));
    }

    // ── Active orders list (for map / dashboard) ──────────────────────────────
    public List<OrderSummary> getActiveOrders() {
        return orderRepository.findByStatusIn(
            List.of(Order.Status.ACCEPTED, Order.Status.PICKED_UP, Order.Status.EN_ROUTE))
            .stream()
            .map(OrderSummary::from)
            .toList();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Enforce valid status transition */
    private void validateTransition(Order.Status from, Order.Status to) {
        boolean valid = switch (from) {
            case NEW       -> to == Order.Status.ACCEPTED  || to == Order.Status.CANCELLED;
            case ACCEPTED  -> to == Order.Status.PICKED_UP || to == Order.Status.CANCELLED;
            case PICKED_UP -> to == Order.Status.EN_ROUTE  || to == Order.Status.CANCELLED;
            case EN_ROUTE  -> to == Order.Status.DELIVERED || to == Order.Status.CANCELLED;
            default        -> false;
        };
        if (!valid) {
            throw ApiException.badRequest(
                "Invalid status transition: " + from + " → " + to);
        }
    }

    private void applyStatusTimestamp(Order order, Order.Status status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case ACCEPTED  -> order.setAcceptedAt(now);
            case PICKED_UP -> order.setPickedUpAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            default        -> {}
        }
    }

    @Transactional
    void updateDriverStatsOnDelivery(String driverId, Order order) {
        driverRepository.findById(driverId).ifPresent(driver -> {
            driver.setTotalDeliveries(driver.getTotalDeliveries() + 1);
            if (order.getDistanceKm() != null) {
                driver.setTotalDistanceKm(
                    driver.getTotalDistanceKm().add(order.getDistanceKm()));
            }
            driverRepository.save(driver);
        });
    }
}
