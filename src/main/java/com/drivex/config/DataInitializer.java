package com.drivex.config;

import com.drivex.entity.*;
import com.drivex.repository.DriverRepository;
import com.drivex.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DataInitializer — runs on startup in dev profile (H2).
 * Creates test drivers + sample orders programmatically
 * (more reliable than data.sql with H2 in MODE=PostgreSQL).
 *
 * Test credentials:
 *   alex@drivex.com  / password123  (id: drv-0001)
 *   sara@drivex.com  / password123  (id: drv-0002)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataInitializer {

    private final DriverRepository driverRepository;
    private final OrderRepository  orderRepository;
    private final PasswordEncoder  passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (driverRepository.count() > 0) {
                log.info("Database already seeded — skipping");
                return;
            }

            log.info("Seeding H2 database...");

            // ── Drivers ──────────────────────────────────────────────────────
            var alex = createDriver("drv-0001", "Alex Kumar",   "alex@drivex.com",  "+15553829471",
                4.96, 1842, 8241.0, 94.0,
                "Toyota", "Camry", 2021, "7KXP-842", "Graphite", Vehicle.Type.CAR);

            var sara = createDriver("drv-0002", "Sara Amara",   "sara@drivex.com",  "+15554421100",
                4.88, 934, 4120.5, 89.0,
                "Honda", "CB500F", 2022, "4MRT-219", "Black", Vehicle.Type.MOTORCYCLE);

            var james = createDriver("drv-0003", "James Okafor", "james@drivex.com", "+15556648820",
                4.72, 412, 2340.0, 85.0,
                "Yamaha", "NMAX", 2023, "9PLZ-774", "White", Vehicle.Type.SCOOTER);

            driverRepository.saveAll(List.of(alex, sara, james));

            // ── Orders ───────────────────────────────────────────────────────
            var ord7741 = Order.builder()
                .id("ord-7741").orderNumber("#ORD-7741")
                .status(Order.Status.NEW)
                .customerName("Sarah Mitchell").customerPhone("+15552018847")
                .pickupAddress("350 Main Street").pickupLat(bd("40.7589")).pickupLng(bd("-73.9851"))
                .pickupLabel("Bella Italia")
                .dropoffAddress("428 Riverside Ave, Apt 3B")
                .dropoffLat(bd("40.7505")).dropoffLng(bd("-73.9934"))
                .restaurantName("Bella Italia")
                .earnings(bd("12.40")).tip(bd("3.00"))
                .distanceKm(bd("3.2")).estimatedMinutes(9)
                .isUrgent(false).category(Order.Category.FOOD)
                .requestedAt(LocalDateTime.now().minusMinutes(2))
                .build();

            var ord7740 = Order.builder()
                .id("ord-7740").orderNumber("#ORD-7740")
                    .status(Order.Status.ACCEPTED)
                    .driver(alex)
                    .customerName("Sarah Mitchell").customerPhone("+15552018847")
                    .pickupAddress("350 Main Street").pickupLat(bd("40.7589")).pickupLng(bd("-73.9851"))
                    .pickupLabel("Bella Italia")
                    .dropoffAddress("428 Riverside Ave, Apt 3B")
                    .dropoffLat(bd("40.7505")).dropoffLng(bd("-73.9934"))
                    .restaurantName("Bella Italia")
                    .earnings(bd("12.40")).tip(bd("3.00"))
                    .distanceKm(bd("3.2")).estimatedMinutes(9)
                    .isUrgent(true).category(Order.Category.FOOD)
                    .requestedAt(LocalDateTime.now().minusMinutes(2))
                    .build();

            var ord7742 = Order.builder()
                .id("ord-7742").orderNumber("#ORD-7742")
                .status(Order.Status.NEW)
                    .driver(alex)
                .customerName("James Chen").customerPhone("+15556443312")
                .pickupAddress("88 Oak Boulevard").pickupLat(bd("40.7614")).pickupLng(bd("-73.9776"))
                .pickupLabel("Sushi Palace")
                .dropoffAddress("12 Cherry Lane")
                .dropoffLat(bd("40.7421")).dropoffLng(bd("-73.9897"))
                .restaurantName("Sushi Palace")
                .earnings(bd("18.20")).tip(bd("5.00"))
                .distanceKm(bd("5.1")).estimatedMinutes(14)
                .isUrgent(true).category(Order.Category.FOOD)
                .requestedAt(LocalDateTime.now().minusMinutes(8))
                .build();

            var ord7738 = Order.builder()
                .id("ord-7738").orderNumber("#ORD-7738")
                .status(Order.Status.DELIVERED)
                .driver(alex)
                .customerName("Priya Nair").customerPhone("+15559901234")
                .pickupAddress("22 Harbor Walk").pickupLat(bd("40.7580")).pickupLng(bd("-73.9855"))
                .pickupLabel("Blue Bottle Coffee")
                .dropoffAddress("Downtown Hub, 5th Floor")
                .dropoffLat(bd("40.7530")).dropoffLng(bd("-73.9820"))
                .restaurantName("Blue Bottle Coffee")
                .earnings(bd("7.60")).tip(bd("2.00"))
                .distanceKm(bd("1.8")).estimatedMinutes(10)
                .isUrgent(false).category(Order.Category.FOOD)
                .acceptedAt(LocalDateTime.now().minusHours(3))
                .pickedUpAt(LocalDateTime.now().minusHours(2).minusMinutes(45))
                .deliveredAt(LocalDateTime.now().minusHours(2).minusMinutes(30))
                .requestedAt(LocalDateTime.now().minusHours(3).minusMinutes(5))
                .build();

            var ord7735 = Order.builder()
                .id("ord-7735").orderNumber("#ORD-7735")
                .status(Order.Status.DELIVERED)
                .driver(alex)
                .customerName("David Chen").customerPhone("+15557728820")
                .pickupAddress("710 Market Street").pickupLat(bd("40.7620")).pickupLng(bd("-73.9900"))
                .pickupLabel("The Burger Joint")
                .dropoffAddress("44 Maple Street, Unit 7")
                .dropoffLat(bd("40.7490")).dropoffLng(bd("-73.9960"))
                .restaurantName("The Burger Joint")
                .earnings(bd("14.80")).tip(bd("4.50"))
                .distanceKm(bd("4.4")).estimatedMinutes(19)
                .isUrgent(false).category(Order.Category.FOOD)
                .acceptedAt(LocalDateTime.now().minusHours(5))
                .pickedUpAt(LocalDateTime.now().minusHours(4).minusMinutes(40))
                .deliveredAt(LocalDateTime.now().minusHours(4).minusMinutes(20))
                .requestedAt(LocalDateTime.now().minusHours(5).minusMinutes(3))
                .build();

            var ord7730 = Order.builder()
                .id("ord-7730").orderNumber("#ORD-7730")
                .status(Order.Status.CANCELLED)
                    .driver(alex)
                .customerName("Emma Wilson").customerPhone("+15554316609")
                .pickupAddress("Green Market, Park Ave").pickupLat(bd("40.7540")).pickupLng(bd("-73.9800"))
                .pickupLabel("Whole Foods Market")
                .dropoffAddress("Park View Residences, Tower B")
                .dropoffLat(bd("40.7460")).dropoffLng(bd("-73.9750"))
                .restaurantName("Whole Foods Market")
                .earnings(BigDecimal.ZERO).tip(BigDecimal.ZERO)
                .distanceKm(bd("6.0")).estimatedMinutes(22)
                .isUrgent(false).category(Order.Category.GROCERY)
                .requestedAt(LocalDateTime.now().minusHours(6))
                .build();

            orderRepository.saveAll(List.of(ord7740, ord7741, ord7742, ord7738, ord7735, ord7730));

            log.info("✅ Seeded: 3 drivers, 5 orders");
            log.info("   Login: alex@drivex.com / password123");
            log.info("   H2 Console: http://localhost:8080/h2-console");
            log.info("   Swagger UI: http://localhost:8080/swagger-ui.html");
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Driver createDriver(
        String id, String name, String email, String phone,
        double rating, int deliveries, double distKm, double acceptRate,
        String make, String model, int year, String plate, String color, Vehicle.Type type
    ) {
        var driver = Driver.builder()
            .id(id).name(name).email(email).phone(phone)
            .passwordHash(passwordEncoder.encode("password123"))
            .status(Driver.Status.ONLINE)
            .rating(BigDecimal.valueOf(rating))
            .totalDeliveries(deliveries)
            .totalDistanceKm(BigDecimal.valueOf(distKm))
            .acceptanceRate(BigDecimal.valueOf(acceptRate))
            .build();

        var vehicle = Vehicle.builder()
            .id(UUID.randomUUID().toString())
            .driver(driver).make(make).model(model).modelYear(year)
            .licensePlate(plate).color(color).type(type)
            .build();

        driver.setVehicle(vehicle);
        return driver;
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
