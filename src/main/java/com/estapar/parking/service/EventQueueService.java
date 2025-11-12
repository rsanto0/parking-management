package com.estapar.parking.service;

import com.estapar.parking.dto.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Queue;

@Service
public class EventQueueService {
    
    private static final Logger log = LoggerFactory.getLogger(EventQueueService.class);
    private final BlockingQueue<WebhookEvent> eventQueue = new LinkedBlockingQueue<>(1000);
    private final Queue<WebhookEvent> deadLetterQueue = new ConcurrentLinkedQueue<>();
    private final ParkingService parkingService;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public EventQueueService(ParkingService parkingService) {
        this.parkingService = parkingService;
        startConsumer();
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }

    public boolean enqueue(WebhookEvent event) {
        boolean added = eventQueue.offer(event);
        if (!added) {
            log.error("Fila cheia! Evento movido para DLQ: {} - {}", event.getEventType(), event.getLicensePlate());
            deadLetterQueue.offer(event);
        }
        return added;
    }

    @Async
    public void startConsumer() {
        new Thread(() -> {
            log.info("Consumidor de eventos iniciado");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    while (paused.get()) {
                        Thread.sleep(100);
                    }
                    WebhookEvent event = eventQueue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Consumidor interrompido");
                    break;
                } catch (Exception e) {
                    log.error("Erro ao processar evento", e);
                }
            }
        }, "event-consumer").start();
    }

    private void processEvent(WebhookEvent event) {
        try {
            log.debug("Processando evento: {} - {}", event.getEventType(), event.getLicensePlate());
            
            switch (event.getEventType()) {
                case "ENTRY" -> parkingService.handleEntry(event.getLicensePlate(), event.getEntryTime());
                case "PARKED" -> parkingService.handleParked(event.getLicensePlate(), event.getLat(), event.getLng());
                case "EXIT" -> parkingService.handleExit(event.getLicensePlate(), event.getExitTime());
                default -> log.warn("Tipo de evento desconhecido: {}", event.getEventType());
            }
            
            log.debug("Evento processado com sucesso: {} - {}", event.getEventType(), event.getLicensePlate());
        } catch (Exception e) {
            log.error("Falha ao processar evento {} - {}: {}", 
                event.getEventType(), event.getLicensePlate(), e.getMessage());
        }
    }

    public int getQueueSize() {
        return eventQueue.size();
    }

    public int getDLQSize() {
        return deadLetterQueue.size();
    }

    public Queue<WebhookEvent> getDeadLetterQueue() {
        return deadLetterQueue;
    }
}
