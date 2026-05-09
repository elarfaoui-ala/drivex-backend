package com.drivex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════════════
// Order
// ═══════════════════════════════════════════════════════════════════════════════
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_status",    columnList = "status"),
    @Index(name = "idx_order_driver",    columnList = "driver_id"),
    @Index(name = "idx_order_requested", columnList = "requested_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    public enum Status {
        NEW, ACCEPTED, PICKED_UP, EN_ROUTE, DELIVERED, CANCELLED
    }

    public enum Category { FOOD, GROCERY, PHARMACY, OTHER }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NEW;

    // Customer info
    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    // Pickup
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_lat", precision = 10, scale = 7)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 10, scale = 7)
    private BigDecimal pickupLng;

    @Column(name = "pickup_label", length = 100)
    private String pickupLabel;

    // Dropoff
    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    @Column(name = "dropoff_lat", precision = 10, scale = 7)
    private BigDecimal dropoffLat;

    @Column(name = "dropoff_lng", precision = 10, scale = 7)
    private BigDecimal dropoffLng;

    // Restaurant / store
    @Column(name = "restaurant_name", length = 120)
    private String restaurantName;

    // Financials
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal earnings = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tip = BigDecimal.ZERO;

    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "is_urgent")
    @Builder.Default
    private Boolean isUrgent = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Category category = Category.FOOD;

    // Driver assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "proof_of_delivery_url")
    private String proofOfDeliveryUrl;

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


