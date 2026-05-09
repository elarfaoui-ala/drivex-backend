package com.drivex.repository;

import com.drivex.entity.Driver;
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
public interface DriverRepository extends JpaRepository<Driver, String> {

    Optional<Driver> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Driver> findByStatus(Driver.Status status);

    @Modifying
    @Query("UPDATE Driver d SET d.status = :status, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") Driver.Status status);

    @Modifying
    @Query("""
        UPDATE Driver d
        SET d.currentLat = :lat, d.currentLng = :lng,
            d.lastSeenAt = :now, d.updatedAt   = :now
        WHERE d.id = :id
        """)
    int updateLocation(
        @Param("id")  String id,
        @Param("lat") BigDecimal lat,
        @Param("lng") BigDecimal lng,
        @Param("now") LocalDateTime now
    );
}
