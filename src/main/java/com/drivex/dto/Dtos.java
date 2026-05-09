package com.drivex.dto;

import com.drivex.entity.Driver;
import com.drivex.entity.Order;
import com.drivex.entity.Vehicle;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dtos — all request/response records in one place.
 * Java records = immutable, no-boilerplate DTOs.
 */
public final class Dtos {

    private Dtos() {}

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Schema(description = "Login request")
    public record LoginRequest(
        @NotBlank @Email               String email,
        @NotBlank @Size(min = 6)       String password
    ) {}

    @Schema(description = "Registration request")
    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Email                    String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}") String phone,
        @NotBlank @Size(min = 6, max = 72)  String password,
        @NotNull                            VehicleRequest vehicle
    ) {}

    @Schema(description = "JWT token response")
    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long   expiresIn,
        DriverSummary driver
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    // ── Driver ────────────────────────────────────────────────────────────────

    @Schema(description = "Driver profile summary")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DriverSummary(
        String        id,
        String        name,
        String        email,
        String        phone,
        String        status,
        BigDecimal    rating,
        Integer       totalDeliveries,
        BigDecimal    totalDistanceKm,
        BigDecimal    acceptanceRate,
        VehicleSummary vehicle,
        LocalDateTime lastSeenAt
    ) {
        /** Map entity → DTO */
        public static DriverSummary from(Driver d) {
            return new DriverSummary(
                d.getId(), d.getName(), d.getEmail(), d.getPhone(),
                d.getStatus().name(), d.getRating(),
                d.getTotalDeliveries(), d.getTotalDistanceKm(),
                d.getAcceptanceRate(),
                d.getVehicle() != null ? VehicleSummary.from(d.getVehicle()) : null,
                d.getLastSeenAt()
            );
        }
    }

    @Schema(description = "Update driver status")
    public record StatusUpdateRequest(
        @NotNull Driver.Status status
    ) {}

    @Schema(description = "Update driver location")
    public record LocationUpdateRequest(
        @NotNull @DecimalMin("-90")  @DecimalMax("90")  BigDecimal lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lng
    ) {}

    // ── Vehicle ───────────────────────────────────────────────────────────────

    public record VehicleRequest(
        @NotBlank String make,
        @NotBlank String model,
        @NotNull  @Min(1990) @Max(2030) Integer year,
        @NotBlank String licensePlate,
        String color,
        @NotNull Vehicle.Type type
    ) {}

    public record VehicleSummary(
        String id, String make, String model,
        Integer year, String licensePlate,
        String color, String type
    ) {
        public static VehicleSummary from(Vehicle v) {
            return new VehicleSummary(
                v.getId(), v.getMake(), v.getModel(),
                v.getModelYear(), v.getLicensePlate(),
                v.getColor(), v.getType().name()
            );
        }
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    @Schema(description = "Order summary for list views")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderSummary(
        String        id,
        String        orderNumber,
        String        status,
        String        customerName,
        String        pickupAddress,
        String        pickupLabel,
        String        dropoffAddress,
        String        restaurantName,
        BigDecimal    earnings,
        BigDecimal    tip,
        BigDecimal    distanceKm,
        Integer       estimatedMinutes,
        Boolean       isUrgent,
        String        category,
        String        driverId,
        LocalDateTime requestedAt,
        LocalDateTime acceptedAt,
        LocalDateTime deliveredAt
    ) {
        public static OrderSummary from(Order o) {
            return new OrderSummary(
                o.getId(), o.getOrderNumber(), o.getStatus().name(),
                o.getCustomerName(),
                o.getPickupAddress(), o.getPickupLabel(),
                o.getDropoffAddress(), o.getRestaurantName(),
                o.getEarnings(), o.getTip(),
                o.getDistanceKm(), o.getEstimatedMinutes(), o.getIsUrgent(),
                o.getCategory().name(),
                o.getDriver() != null ? o.getDriver().getId() : null,
                o.getRequestedAt(), o.getAcceptedAt(), o.getDeliveredAt()
            );
        }
    }

    @Schema(description = "Full order detail")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderDetail(
        OrderSummary summary,
        BigDecimal   pickupLat,
        BigDecimal   pickupLng,
        BigDecimal   dropoffLat,
        BigDecimal   dropoffLng,
        String       customerPhone,
        String       deliveryNote,
        String       proofOfDeliveryUrl,
        List<OrderItemDto> items
    ) {
        public static OrderDetail from(Order o) {
            return new OrderDetail(
                OrderSummary.from(o),
                o.getPickupLat(), o.getPickupLng(),
                o.getDropoffLat(), o.getDropoffLng(),
                o.getCustomerPhone(), o.getDeliveryNote(),
                o.getProofOfDeliveryUrl(),
                o.getItems().stream().map(OrderItemDto::from).toList()
            );
        }
    }

    public record OrderItemDto(String id, String name, Integer quantity, String notes) {
        public static OrderItemDto from(com.drivex.entity.OrderItem i) {
            return new OrderItemDto(i.getId(), i.getName(), i.getQuantity(), i.getNotes());
        }
    }

    @Schema(description = "Accept order request (driver claims order)")
    public record AcceptOrderRequest(@NotBlank String driverId) {}

    @Schema(description = "Update order status")
    public record OrderStatusUpdateRequest(@NotNull Order.Status status) {}

    // ── Earnings ──────────────────────────────────────────────────────────────

    @Schema(description = "Earnings summary for a time period")
    public record EarningsSummary(
        BigDecimal totalEarnings,
        BigDecimal totalTips,
        Integer    totalTrips,
        BigDecimal avgPerTrip,
        BigDecimal totalHours,
        List<DailyEarning> daily
    ) {}

    public record DailyEarning(
        String     date,
        BigDecimal amount,
        BigDecimal tips,
        Integer    trips,
        BigDecimal hours
    ) {}

    // ── Location (WebSocket payload) ──────────────────────────────────────────

    public record LocationPayload(
        String     driverId,
        BigDecimal lat,
        BigDecimal lng,
        Double     heading,
        Double     speedKmh,
        LocalDateTime timestamp
    ) {}

    // ── WebSocket events ──────────────────────────────────────────────────────

    public record WsEvent(
        String eventType,   // ORDER_NEW | ORDER_STATUS_CHANGED | DRIVER_LOCATION | ...
        Object payload
    ) {}

    // ── Generic responses ─────────────────────────────────────────────────────

    public record MessageResponse(String message) {}

    public record PagedResponse<T>(
        List<T> content,
        int     page,
        int     size,
        long    totalElements,
        int     totalPages
    ) {}
}
