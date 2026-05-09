package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Order;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverRepository;
import com.drivex.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarningService {

    private final OrderRepository  orderRepository;
    private final DriverRepository driverRepository;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Today's earnings ──────────────────────────────────────────────────────
    @Cacheable(value = "earnings:today", key = "#driverId")
    public EarningsSummary getTodayEarnings(String driverId) {
        ensureDriverExists(driverId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);
        return buildSummary(driverId, startOfDay, endOfDay);
    }

    // ── Weekly earnings ───────────────────────────────────────────────────────
    @Cacheable(value = "earnings:week", key = "#driverId")
    public EarningsSummary getWeekEarnings(String driverId) {
        ensureDriverExists(driverId);
        LocalDateTime start = LocalDate.now()
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return buildSummary(driverId, start, end);
    }

    // ── Monthly earnings ──────────────────────────────────────────────────────
    @Cacheable(value = "earnings:month", key = "#driverId")
    public EarningsSummary getMonthEarnings(String driverId) {
        ensureDriverExists(driverId);
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end   = LocalDateTime.now();
        return buildSummary(driverId, start, end);
    }

    // ── Custom range ──────────────────────────────────────────────────────────
    public EarningsSummary getCustomEarnings(String driverId, LocalDate from, LocalDate to) {
        ensureDriverExists(driverId);
        return buildSummary(driverId, from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }

    // ── Core builder ──────────────────────────────────────────────────────────
    private EarningsSummary buildSummary(
        String driverId, LocalDateTime from, LocalDateTime to
    ) {
        List<Order> orders = orderRepository.findByDriverAndDateRange(driverId, from, to)
            .stream()
            .filter(o -> o.getStatus() == Order.Status.DELIVERED)
            .toList();

        BigDecimal totalEarnings = orders.stream()
            .map(o -> o.getEarnings().add(o.getTip()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTips = orders.stream()
            .map(Order::getTip)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int trips = orders.size();

        BigDecimal avgPerTrip = trips > 0
            ? totalEarnings.divide(BigDecimal.valueOf(trips), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Estimate hours: sum of estimated minutes ÷ 60
        BigDecimal totalHours = orders.stream()
            .filter(o -> o.getEstimatedMinutes() != null)
            .map(o -> BigDecimal.valueOf(o.getEstimatedMinutes()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        List<DailyEarning> daily = buildDailyBreakdown(orders);

        return new EarningsSummary(totalEarnings, totalTips, trips, avgPerTrip, totalHours, daily);
    }

    /** Group orders by calendar day and aggregate per-day figures */
    private List<DailyEarning> buildDailyBreakdown(List<Order> orders) {
        Map<LocalDate, List<Order>> byDay = orders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getDeliveredAt().toLocalDate()));

        return byDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                List<Order> dayOrders = e.getValue();
                BigDecimal amount = dayOrders.stream()
                    .map(o -> o.getEarnings().add(o.getTip()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal tips = dayOrders.stream()
                    .map(Order::getTip)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                int trips = dayOrders.size();
                BigDecimal hours = dayOrders.stream()
                    .filter(o -> o.getEstimatedMinutes() != null)
                    .map(o -> BigDecimal.valueOf(o.getEstimatedMinutes()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                return new DailyEarning(e.getKey().format(DAY_FMT), amount, tips, trips, hours);
            })
            .toList();
    }

    private void ensureDriverExists(String driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw ApiException.notFound("Driver", driverId);
        }
    }
}
