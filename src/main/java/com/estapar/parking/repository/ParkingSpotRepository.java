package com.estapar.parking.repository;

import com.estapar.parking.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    long countByOccupiedTrue();
}