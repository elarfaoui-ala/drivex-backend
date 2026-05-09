package com.drivex.controller;

import com.drivex.dto.Dtos.*;
import com.drivex.service.EarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/earnings")
@RequiredArgsConstructor
@Tag(name = "Earnings", description = "Driver earnings — daily, weekly, monthly")
@SecurityRequirement(name = "bearerAuth")
public class EarningsController {

    private final EarningService earningService;

    @GetMapping("/{driverId}/today")
    @Operation(summary = "Today's earnings summary")
    public EarningsSummary today(@PathVariable String driverId) {
        return earningService.getTodayEarnings(driverId);
    }

    @GetMapping("/{driverId}/week")
    @Operation(summary = "Current week earnings summary (Mon–now)")
    public EarningsSummary week(@PathVariable String driverId) {
        return earningService.getWeekEarnings(driverId);
    }

    @GetMapping("/{driverId}/month")
    @Operation(summary = "Current month earnings summary")
    public EarningsSummary month(@PathVariable String driverId) {
        return earningService.getMonthEarnings(driverId);
    }

    @GetMapping("/{driverId}/custom")
    @Operation(summary = "Custom date range earnings (ISO format: 2024-01-01)")
    public EarningsSummary custom(
        @PathVariable String driverId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return earningService.getCustomEarnings(driverId, from, to);
    }
}
