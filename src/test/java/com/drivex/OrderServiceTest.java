package com.drivex;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.*;
import com.drivex.entity.Order;
import com.drivex.exception.ApiException;
import com.drivex.repository.*;
import com.drivex.service.OrderService;
import com.drivex.websocket.OrderEventPublisher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository    orderRepository;
    @Mock DriverRepository   driverRepository;
    @Mock OrderEventPublisher eventPublisher;

    @InjectMocks OrderService orderService;

    private Driver onlineDriver;
    private Order newOrder;

    @BeforeEach
    void setUp() {
        onlineDriver = Driver.builder()
            .id("drv-0001").name("Alex").email("alex@drivex.com")
            .status(Driver.Status.ONLINE)
            .totalDeliveries(0)
            .totalDistanceKm(BigDecimal.ZERO)
            .build();

        newOrder = Order.builder()
            .id("ord-7741").orderNumber("#ORD-7741")
            .status(Order.Status.NEW)
            .customerName("Sarah").customerPhone("+1555")
            .pickupAddress("350 Main St").dropoffAddress("428 Riverside")
            .earnings(BigDecimal.valueOf(12.40))
            .tip(BigDecimal.valueOf(3.00))
            .distanceKm(BigDecimal.valueOf(3.2))
            .estimatedMinutes(9)
            .isUrgent(false)
            .category(Order.Category.FOOD)
            .build();
    }

    // ── acceptOrder ───────────────────────────────────────────────────────────

    @Test
    void acceptOrder_whenOrderNewAndDriverOnline_shouldSucceed() {
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));
        when(driverRepository.findById("drv-0001")).thenReturn(Optional.of(onlineDriver));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderSummary result = orderService.acceptOrder("ord-7741", "drv-0001");

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.driverId()).isEqualTo("drv-0001");
        verify(eventPublisher).publishOrderAssigned(eq("drv-0001"), any());
        verify(eventPublisher).publishStatusChanged(any());
    }

    @Test
    void acceptOrder_whenOrderAlreadyAccepted_shouldThrowConflict() {
        newOrder.setStatus(Order.Status.ACCEPTED);
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));

        assertThatThrownBy(() -> orderService.acceptOrder("ord-7741", "drv-0001"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("no longer available");
    }

    @Test
    void acceptOrder_whenDriverOffline_shouldThrowBadRequest() {
        onlineDriver.setStatus(Driver.Status.OFFLINE);
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));
        when(driverRepository.findById("drv-0001")).thenReturn(Optional.of(onlineDriver));

        assertThatThrownBy(() -> orderService.acceptOrder("ord-7741", "drv-0001"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("ONLINE");
    }

    @Test
    void acceptOrder_whenOrderNotFound_shouldThrow404() {
        when(orderRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.acceptOrder("bad-id", "drv-0001"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("not found");
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_validTransitionAcceptedToPickedUp_shouldSucceed() {
        newOrder.setStatus(Order.Status.ACCEPTED);
        newOrder.setDriver(onlineDriver);
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderSummary result = orderService.updateStatus(
            "ord-7741", "drv-0001",
            new OrderStatusUpdateRequest(Order.Status.PICKED_UP));

        assertThat(result.status()).isEqualTo("PICKED_UP");
        verify(eventPublisher).publishStatusChanged(any());
    }

    @Test
    void updateStatus_invalidTransition_shouldThrowBadRequest() {
        newOrder.setStatus(Order.Status.NEW);
        newOrder.setDriver(onlineDriver);
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));

        assertThatThrownBy(() -> orderService.updateStatus(
                "ord-7741", "drv-0001",
                new OrderStatusUpdateRequest(Order.Status.DELIVERED)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_byWrongDriver_shouldThrowForbidden() {
        var otherDriver = Driver.builder().id("drv-9999").build();
        newOrder.setStatus(Order.Status.ACCEPTED);
        newOrder.setDriver(otherDriver);
        when(orderRepository.findById("ord-7741")).thenReturn(Optional.of(newOrder));

        assertThatThrownBy(() -> orderService.updateStatus(
                "ord-7741", "drv-0001",
                new OrderStatusUpdateRequest(Order.Status.PICKED_UP)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("not assigned");
    }

    // ── getAvailableOrders ────────────────────────────────────────────────────

    @Test
    void getAvailableOrders_shouldReturnOnlyNewOrders() {
        when(orderRepository.findByStatus(Order.Status.NEW))
            .thenReturn(List.of(newOrder));

        List<OrderSummary> result = orderService.getAvailableOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo("NEW");
    }

    // ── getDriverOrders ───────────────────────────────────────────────────────

    @Test
    void getDriverOrders_paginated_shouldReturnCorrectPage() {
        Page<Order> page = new PageImpl<>(
            List.of(newOrder), PageRequest.of(0, 20), 1);
        when(orderRepository.findByDriverId(eq("drv-0001"), any()))
            .thenReturn(page);

        PagedResponse<OrderSummary> result =
            orderService.getDriverOrders("drv-0001", null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
