package com.drivex;

import com.drivex.dto.Dtos.EarningsSummary;
import com.drivex.entity.Order;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.repository.OrderRepository;
import com.drivex.service.EarningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarningServiceTest {

    @Mock OrderRepository  orderRepository;
    @Mock DriverRepository driverRepository;

    @InjectMocks EarningService earningService;

    private Order delivered1;
    private Order delivered2;

    @BeforeEach
    void setUp() {
        delivered1 = Order.builder()
            .id("ord-1").orderNumber("#ORD-1")
            .status(Order.Status.DELIVERED)
            .earnings(BigDecimal.valueOf(12.40))
            .tip(BigDecimal.valueOf(3.00))
            .distanceKm(BigDecimal.valueOf(3.2))
            .estimatedMinutes(9)
            .deliveredAt(LocalDateTime.now().minusHours(2))
            .requestedAt(LocalDateTime.now().minusHours(3))
            .build();

        delivered2 = Order.builder()
            .id("ord-2").orderNumber("#ORD-2")
            .status(Order.Status.DELIVERED)
            .earnings(BigDecimal.valueOf(18.20))
            .tip(BigDecimal.valueOf(5.00))
            .distanceKm(BigDecimal.valueOf(5.1))
            .estimatedMinutes(14)
            .deliveredAt(LocalDateTime.now().minusHours(1))
            .requestedAt(LocalDateTime.now().minusHours(2))
            .build();
    }

    @Test
    void getTodayEarnings_withTwoDeliveries_calculatesCorrectTotals() {
        when(driverRepository.existsById("drv-0001")).thenReturn(true);
        when(orderRepository.findByDriverAndDateRange(eq("drv-0001"), any(), any()))
            .thenReturn(List.of(delivered1, delivered2));

        EarningsSummary result = earningService.getTodayEarnings("drv-0001");

        // 12.40 + 3.00 + 18.20 + 5.00 = 38.60
        assertThat(result.totalEarnings()).isEqualByComparingTo("38.60");
        // 3.00 + 5.00 = 8.00
        assertThat(result.totalTips()).isEqualByComparingTo("8.00");
        assertThat(result.totalTrips()).isEqualTo(2);
        // avg = 38.60 / 2 = 19.30
        assertThat(result.avgPerTrip()).isEqualByComparingTo("19.30");
    }

    @Test
    void getTodayEarnings_withNoDeliveries_returnsZeros() {
        when(driverRepository.existsById("drv-0001")).thenReturn(true);
        when(orderRepository.findByDriverAndDateRange(any(), any(), any()))
            .thenReturn(List.of());

        EarningsSummary result = earningService.getTodayEarnings("drv-0001");

        assertThat(result.totalEarnings()).isEqualByComparingTo("0");
        assertThat(result.totalTrips()).isEqualTo(0);
        assertThat(result.daily()).isEmpty();
    }

    @Test
    void getTodayEarnings_cancelledOrdersAreIgnored() {
        var cancelled = Order.builder()
            .id("ord-3").status(Order.Status.CANCELLED)
            .earnings(BigDecimal.ZERO).tip(BigDecimal.ZERO)
            .requestedAt(LocalDateTime.now()).build();

        when(driverRepository.existsById("drv-0001")).thenReturn(true);
        when(orderRepository.findByDriverAndDateRange(any(), any(), any()))
            .thenReturn(List.of(delivered1, cancelled));

        EarningsSummary result = earningService.getTodayEarnings("drv-0001");

        // Only delivered1 should count
        assertThat(result.totalTrips()).isEqualTo(1);
        assertThat(result.totalEarnings()).isEqualByComparingTo("15.40"); // 12.40 + 3.00
    }

    @Test
    void getTodayEarnings_whenDriverNotFound_throwsNotFound() {
        when(driverRepository.existsById("bad-id")).thenReturn(false);

        assertThatThrownBy(() -> earningService.getTodayEarnings("bad-id"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void dailyBreakdown_groupsOrdersByCalendarDay() {
        var yesterday = Order.builder()
            .id("ord-4").status(Order.Status.DELIVERED)
            .earnings(BigDecimal.valueOf(10.00))
            .tip(BigDecimal.valueOf(2.00))
            .estimatedMinutes(12)
            .deliveredAt(LocalDateTime.now().minusDays(1))
            .requestedAt(LocalDateTime.now().minusDays(1).minusHours(1))
            .build();

        when(driverRepository.existsById("drv-0001")).thenReturn(true);
        when(orderRepository.findByDriverAndDateRange(any(), any(), any()))
            .thenReturn(List.of(delivered1, delivered2, yesterday));

        EarningsSummary result = earningService.getWeekEarnings("drv-0001");

        // Should have 2 distinct days
        assertThat(result.daily()).hasSize(2);
    }
}
