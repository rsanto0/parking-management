package com.estapar.parking.controller;

import com.estapar.parking.dto.WebhookEvent;
import com.estapar.parking.service.EventQueueService;
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
 * do simulador externo. Os eventos são enfileirados para processamento
 * assíncrono, garantindo alta disponibilidade e resiliência.
 * 
 * Benefícios do processamento assíncrono:
 * - Desacoplamento: Controller responde imediatamente sem esperar processamento
 * - Performance: Absorve picos de carga com fila de buffer
 * - Resiliência: Eventos podem ser reprocessados em caso de falha
 * - Escalabilidade: Suporta múltiplos eventos simultâneos sem bloqueio
 * 
 * @author Sistema de Estacionamento
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final EventQueueService eventQueueService;

    public WebhookController(EventQueueService eventQueueService) {
        this.eventQueueService = eventQueueService;
    }

    /**
     * Recebe e enfileira eventos para processamento assíncrono.
     * Retorna imediatamente após enfileirar, sem bloquear o thread HTTP.
     */
    @PostMapping
    public ResponseEntity<Void> handleEvent(@Valid @RequestBody WebhookEvent event) {
        log.info("Evento recebido: {} - {} (fila: {})", 
            event.getEventType(), event.getLicensePlate(), eventQueueService.getQueueSize());

        boolean enqueued = eventQueueService.enqueue(event);
        
        if (!enqueued) {
            log.error("Fila cheia - evento rejeitado: {} - {}", 
                event.getEventType(), event.getLicensePlate());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.accepted().build();
    }
}
