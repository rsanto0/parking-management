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
     * Record para encapsular resposta de erro com status e mensagem.
     */
    private record ErrorResponse(HttpStatus status, String message) {}

    /**
     * Processa eventos de movimentação usando programação funcional moderna.
     */
    @PostMapping
    public ResponseEntity<String> handleEvent(@Valid @RequestBody WebhookEvent event) {
        log.info("Evento recebido: {} para veículo {}", event.getEventType(), event.getLicensePlate());

        return processEvent(event)
            .map(error -> ResponseEntity.status(error.status()).body(error.message()))
            .orElse(ResponseEntity.ok().build());
    }

    /**
     * Processa evento e retorna Optional com erro se houver falha.
     */
    private java.util.Optional<ErrorResponse> processEvent(WebhookEvent event) {
        try {
        	
        	/*
        	 * Switch Expression do Java 21 com yield
			 * Type-safe: Cada case retorna Optional<ErrorResponse>
			 * Null-safe: Não há Map intermediário que pode falhar
        	 * */
        	
            return switch (event.getEventType()) {
                case "ENTRY" -> {
                    parkingService.handleEntry(event.getLicensePlate(), event.getEntryTime());
                    yield java.util.Optional.<ErrorResponse>empty();
                }
                case "PARKED" -> {
                    parkingService.handleParked(event.getLicensePlate(), event.getLat(), event.getLng());
                    yield java.util.Optional.<ErrorResponse>empty();
                }
                case "EXIT" -> {
                    parkingService.handleExit(event.getLicensePlate(), event.getExitTime());
                    yield java.util.Optional.<ErrorResponse>empty();
                }
                default -> {
                    log.warn("Tipo de evento desconhecido: {}", event.getEventType());
                    yield java.util.Optional.<ErrorResponse>empty();
                }
            };
        } catch (RuntimeException e) {
            log.error("Erro ao processar evento: {}", e.getMessage());
            
            var status = e.getMessage().contains("Veículo não encontrado") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST;
            
            return java.util.Optional.of(new ErrorResponse(status, e.getMessage()));
        }
    }
    
    
//    /**
//     * Processa eventos de movimentação usando programação funcional moderna.
//     */
//    @PostMapping
//    public ResponseEntity<String> handleEvent(@Valid @RequestBody WebhookEvent event) {
//        log.info("Evento recebido: {} para veículo {}", event.getEventType(), event.getLicensePlate());
//
//        return processEvent(event)
//            .map(error -> ResponseEntity.status(error.status()).body(error.message()))
//            .orElse(ResponseEntity.ok().build());
//    }

//    /**
//     * Processa evento e retorna Optional com erro se houver falha.
//     */
//    private java.util.Optional<ErrorResponse> processEvent(WebhookEvent event) {
//        try {
    
    			/*
    			 * Map.of() não aceita valores null, e alguns métodos do WebhookEvent podem retornar null
    			 * Lambdas com parâmetros null causavam NullPointerException
    			 * */
    
//            var eventHandlers = java.util.Map.of(
//                "ENTRY", () -> parkingService.handleEntry(event.getLicensePlate(), event.getEntryTime()),
//                "PARKED", () -> parkingService.handleParked(event.getLicensePlate(), event.getLat(), event.getLng()),
//                "EXIT", () -> parkingService.handleExit(event.getLicensePlate(), event.getExitTime())
//            );
//
//            java.util.Optional.ofNullable(eventHandlers.get(event.getEventType()))
//                .ifPresentOrElse(
//                    Runnable::run,
//                    () -> log.warn("Tipo de evento desconhecido: {}", event.getEventType())
//                );
//            
//            return java.util.Optional.empty();
//        } catch (RuntimeException e) {
//            log.error("Erro ao processar evento: {}", e.getMessage());
//            
//            var status = e.getMessage().contains("Veículo não encontrado") 
//                ? HttpStatus.NOT_FOUND 
//                : HttpStatus.BAD_REQUEST;
//            
//            return java.util.Optional.of(new ErrorResponse(status, e.getMessage()));
//        }
//    }
    
}