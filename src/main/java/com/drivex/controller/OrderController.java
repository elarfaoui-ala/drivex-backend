package com.drivex.controller;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Order;
import com.drivex.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    // ── Available orders ──────────────────────────────────────────────────────

    @GetMapping("/available")
    @Operation(summary = "List all NEW orders (no JWT required — visible to any driver)")
    public List<OrderSummary> getAvailable() {
        return orderService.getAvailableOrders();
    }

    @GetMapping("/active")
    @Operation(summary = "List orders currently in-progress (ACCEPTED / PICKED_UP / EN_ROUTE)")
    public List<OrderSummary> getActive() {
        return orderService.getActiveOrders();
    }

    // ── Single order ──────────────────────────────────────────────────────────

    @GetMapping("/{orderId}")
    @Operation(summary = "Get full order detail including items, coordinates, and customer info")
    public OrderDetail getDetail(@PathVariable String orderId) {
        return orderService.getOrderDetail(orderId);
    }

    // ── Driver's order history (paginated) ────────────────────────────────────

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get paginated order history for a driver")
    public PagedResponse<OrderSummary> getDriverOrders(
        @PathVariable String driverId,
        @Parameter(description = "Filter by status (optional)")
        @RequestParam(required = false) Order.Status status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.getDriverOrders(driverId, status, page, size);
    }

    // ── Accept order ──────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Driver accepts (claims) an available order")
    public OrderSummary accept(
        @PathVariable String orderId,
        @Valid @RequestBody AcceptOrderRequest req
    ) {
        return orderService.acceptOrder(orderId, req.driverId());
    }

    // ── Update status ─────────────────────────────────────────────────────────

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Advance order status: ACCEPTED → PICKED_UP → EN_ROUTE → DELIVERED")
    public OrderSummary updateStatus(
        @PathVariable String orderId,
        @RequestParam String driverId,
        @Valid @RequestBody OrderStatusUpdateRequest req
    ) {
        return orderService.updateStatus(orderId, driverId, req);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order (driver must be the assigned driver)")
    public OrderSummary cancel(
        @PathVariable String orderId,
        @RequestParam String driverId
    ) {
        return orderService.cancelOrder(orderId, driverId);
    }
}
