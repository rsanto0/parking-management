package com.estapar.parking.controller;

import com.estapar.parking.dto.WebhookEvent;
import com.estapar.parking.service.ParkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * Controller REST para recepção de eventos do simulador de estacionamento.
 * 
 * Este controller atua como um webhook que recebe eventos em tempo real
 * do simulador externo, processando movimentações de veículos no estacionamento.
 * 
 * Os eventos processados incluem:
 * - ENTRY: Veículo chegando na cancela de entrada
 * - PARKED: Veículo estacionado em uma vaga específica
 * - EXIT: Veículo saindo do estacionamento
 * 
 * Este endpoint é público (sem autenticação) para permitir que o simulador
 * externo envie eventos sem complicações de segurança.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.WebhookEvent
 * @see com.estapar.parking.service.ParkingService
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    /**
     * Logger para registro de eventos e erros do webhook.
     * Utilizado para auditoria e debugging de eventos recebidos.
     */
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    
    /**
     * Serviço de estacionamento responsável pelo processamento dos eventos.
     * Injetação de dependência via construtor para garantir imutabilidade.
     */
    private final ParkingService parkingService;

    /**
     * Construtor para injeção de dependência do serviço de estacionamento.
     * 
     * @param parkingService serviço responsável pelo processamento de eventos de veículos
     * @throws IllegalArgumentException se parkingService for null
     */
    public WebhookController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * Processa eventos de movimentação de veículos.
     * Exceções são tratadas pelo GlobalExceptionHandler.
     */
    @PostMapping
    public ResponseEntity<Void> handleEvent(@Valid @RequestBody WebhookEvent event) {
        log.info("Evento recebido: {} para veículo {}", event.getEventType(), event.getLicensePlate());

        switch (event.getEventType()) {
            case "ENTRY" -> parkingService.handleEntry(event.getLicensePlate(), event.getEntryTime());
            case "PARKED" -> parkingService.handleParked(event.getLicensePlate(), event.getLat(), event.getLng());
            case "EXIT" -> parkingService.handleExit(event.getLicensePlate(), event.getExitTime());
            default -> log.warn("Tipo de evento desconhecido: {}", event.getEventType());
        }

        return ResponseEntity.ok().build();
    }
}