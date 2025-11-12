package com.estapar.parking.config;

import com.estapar.parking.dto.ErrorResponse;
import com.estapar.parking.exception.ParkingFullException;
import com.estapar.parking.exception.VehicleAlreadyParkedException;
import com.estapar.parking.exception.VehicleNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manipulador global de exceções para o sistema de estacionamento.
 * 
 * Esta classe intercepta e trata todas as exceções não capturadas
 * nos controllers, fornecendo respostas padronizadas e logs apropriados.
 * 
 * Utiliza @RestControllerAdvice para aplicar o tratamento globalmente
 * a todos os controllers REST da aplicação.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 */
@RestControllerAdvice // Intercepta TODOS os @RestController da aplicação
@Slf4j
public class GlobalExceptionHandler {

	// Cada @ExceptionHandler captura um tipo específico de exceção
    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFoundException ex) {
    	
    	// 1. Loga o erro
        log.warn("Vehicle not found: {}", ex.getMessage());
        
        // 2. Cria resposta padronizada
        ErrorResponse error = new ErrorResponse("VEHICLE_NOT_FOUND", ex.getMessage());
        
        // 3. Retorna HTTP 404 com corpo JSON
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ParkingFullException.class)
    public ResponseEntity<ErrorResponse> handleParkingFull(ParkingFullException ex) {
        log.warn("Parking full: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("PARKING_FULL", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(VehicleAlreadyParkedException.class)
    public ResponseEntity<ErrorResponse> handleVehicleAlreadyParked(VehicleAlreadyParkedException ex) {
        log.warn("Vehicle already parked: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("VEHICLE_ALREADY_PARKED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", message);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}