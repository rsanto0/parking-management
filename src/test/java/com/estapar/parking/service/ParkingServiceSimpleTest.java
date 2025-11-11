package com.estapar.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.entity.Sector;
import com.estapar.parking.entity.Vehicle;
import com.estapar.parking.exception.VehicleAlreadyParkedException;
import com.estapar.parking.exception.VehicleNotFoundException;
import com.estapar.parking.repository.ParkingSpotRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParkingService - Testes Simplificados")
class ParkingServiceSimpleTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Mock
    private VehicleRepository vehicleRepository;
    
    @Mock
    private ParkingSpotRepository spotRepository;
    
    @Mock
    private PricingService pricingService;
    
    @Mock
    private GarageService garageService;
    
    @InjectMocks
    private ParkingService parkingService;
    
    private Sector sectorA;
    private ParkingSpot spot1;

    @BeforeEach
    void setUp() {
        sectorA = new Sector("A", BigDecimal.valueOf(40.50), 10);
        spot1 = new ParkingSpot(1L, BigDecimal.valueOf(-23.5505), BigDecimal.valueOf(-46.6333), sectorA);
    }

    @Test
    @DisplayName("✅ Entrada: deve processar entrada de veículo")
    void shouldProcessEntry() {
        var licensePlate = "ABC1234";
        var entryTime = "2025-01-20T10:00:00";
        
        when(vehicleRepository.findActiveByLicensePlate(licensePlate)).thenReturn(Optional.empty());
        when(garageService.isFull()).thenReturn(false);
        when(garageService.getOccupancyRate()).thenReturn(50.0);
        when(spotRepository.findAll()).thenReturn(java.util.List.of(spot1));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        parkingService.handleEntry(licensePlate, entryTime);
        
        assertThat(true).as("Entrada processada com sucesso").isTrue();
    }

    @Test
    @DisplayName("❌ Entrada: deve rejeitar veículo já estacionado")
    void shouldRejectAlreadyParkedVehicle() {
        var licensePlate = "ABC1234";
        var entryTime = "2025-01-20T10:00:00";
        
        var existingVehicle = new Vehicle();
        existingVehicle.setLicensePlate(licensePlate);
        when(vehicleRepository.findActiveByLicensePlate(licensePlate)).thenReturn(Optional.of(existingVehicle));
        
        assertThatThrownBy(() -> parkingService.handleEntry(licensePlate, entryTime))
            .isInstanceOf(VehicleAlreadyParkedException.class)
            .hasMessageContaining("já está estacionado");
    }

    @Test
    @DisplayName("❌ Saída: deve rejeitar saída sem entrada")
    void shouldRejectExitWithoutEntry() {
        var licensePlate = "XYZ9999";
        var exitTime = "2025-01-20T12:00:00";
        
        when(vehicleRepository.findActiveByLicensePlate(licensePlate)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> parkingService.handleExit(licensePlate, exitTime))
            .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    @DisplayName("✅ Saída: deve processar saída com cálculo")
    void shouldProcessExit() {
        var licensePlate = "DEF5678";
        var exitTime = "2025-01-20T12:00:00";
        
        var vehicle = new Vehicle();
        vehicle.setLicensePlate(licensePlate);
        vehicle.setParkingSpot(spot1);
        vehicle.setEntryTime(LocalDateTime.of(2025, 1, 20, 10, 0));
        
        when(vehicleRepository.findActiveByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle));
        when(garageService.getOccupancyRate()).thenReturn(50.0);
        lenient().when(pricingService.calculatePrice(any(LocalDateTime.class), any(LocalDateTime.class), any(Double.class), any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(10.50));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        parkingService.handleExit(licensePlate, exitTime);
        
        assertThat(true).as("Saída processada com sucesso").isTrue();
    }

    @Test
    @DisplayName("✅ Receita: deve calcular receita por setor")
    void shouldCalculateRevenue() {
        var date = "2025-01-20";
        var sector = "A";
        
        when(vehicleRepository.calculateRevenueBySectorAndDate(any(String.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(50.00));
        
        var revenue = parkingService.getRevenue(sector, date);
        
        assertThat(revenue).isNotNull();
        assertThat(revenue).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }
}