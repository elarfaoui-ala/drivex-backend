package com.drivex.repository;

import com.drivex.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByDriverId(String driverId, Pageable pageable);

    Page<Order> findByDriverIdAndStatus(String driverId, Order.Status status, Pageable pageable);

    List<Order> findByStatus(Order.Status status);

    List<Order> findByStatusIn(List<Order.Status> statuses);

    @Query("""
        SELECT o FROM Order o
        WHERE o.driver.id = :driverId
          AND o.requestedAt BETWEEN :from AND :to
        ORDER BY o.requestedAt DESC
        """)
    List<Order> findByDriverAndDateRange(
        @Param("driverId") String driverId,
        @Param("from")     LocalDateTime from,
        @Param("to")       LocalDateTime to
    );

    @Query("""
        SELECT COALESCE(SUM(o.earnings + o.tip), 0)
        FROM Order o
        WHERE o.driver.id = :driverId
          AND o.status = 'DELIVERED'
          AND o.deliveredAt BETWEEN :from AND :to
        """)
    BigDecimal sumEarningsByDriverAndPeriod(
        @Param("driverId") String driverId,
        @Param("from")     LocalDateTime from,
        @Param("to")       LocalDateTime to
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.driver.id = :driverId AND o.status = 'DELIVERED'")
    long countDeliveredByDriver(@Param("driverId") String driverId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") Order.Status status);
}
