package com.estapar.parking.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.estapar.parking.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    @Query("SELECT v FROM Vehicle v WHERE v.licensePlate = :licensePlate AND v.status != 'EXITED'")
    Optional<Vehicle> findActiveByLicensePlate(String licensePlate);

    @Query("SELECT COALESCE(SUM(v.totalAmount), 0) FROM Vehicle v WHERE v.parkingSpot.sector.name = :sectorName AND v.exitTime >= :startDate AND v.exitTime < :endDate")
    BigDecimal calculateRevenueBySectorAndDate(String sectorName, LocalDateTime startDate, LocalDateTime endDate);

	Optional<Vehicle> findByLicensePlate(String licensePlate);
}