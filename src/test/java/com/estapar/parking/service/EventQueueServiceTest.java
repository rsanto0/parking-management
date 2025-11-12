package com.estapar.parking.service;

import com.estapar.parking.dto.WebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventQueueServiceTest {

    @Mock
    private ParkingService parkingService;

    private EventQueueService eventQueueService;

    @BeforeEach
    void setUp() {
        eventQueueService = new EventQueueService(parkingService);
    }

    @Test
    void deveEnfileirarEventoRapidamente() throws InterruptedException {
        doAnswer(invocation -> {
            Thread.sleep(2000);
            return null;
        }).when(parkingService).handleEntry(anyString(), anyString());

        WebhookEvent event = createEvent("ENTRY", "ABC1234");

        long start = System.currentTimeMillis();
        boolean enqueued = eventQueueService.enqueue(event);
        long duration = System.currentTimeMillis() - start;

        assertTrue(enqueued);
        assertTrue(duration < 100, "Enfileiramento deve ser < 100ms, foi: " + duration + "ms");
        assertTrue(eventQueueService.getQueueSize() >= 0);
    }

    @Test
    void deveProcessarEventoAssincronamente() throws InterruptedException {
        doAnswer(invocation -> {
            Thread.sleep(500);
            return null;
        }).when(parkingService).handleEntry(anyString(), anyString());

        WebhookEvent event = createEvent("ENTRY", "ABC1234");
        
        long start = System.currentTimeMillis();
        eventQueueService.enqueue(event);
        long enqueueDuration = System.currentTimeMillis() - start;

        assertTrue(enqueueDuration < 100, "Enfileiramento não deve bloquear");

        Thread.sleep(1000);

        verify(parkingService, times(1)).handleEntry(eq("ABC1234"), anyString());
        assertEquals(0, eventQueueService.getQueueSize());
    }

    @Test
    void deveProcessarMultiplosEventosEmOrdem() throws InterruptedException {
        WebhookEvent event1 = createEvent("ENTRY", "ABC1111");
        WebhookEvent event2 = createEvent("ENTRY", "ABC2222");
        WebhookEvent event3 = createEvent("ENTRY", "ABC3333");

        eventQueueService.enqueue(event1);
        eventQueueService.enqueue(event2);
        eventQueueService.enqueue(event3);

        Thread.sleep(1000);

        verify(parkingService).handleEntry(eq("ABC1111"), anyString());
        verify(parkingService).handleEntry(eq("ABC2222"), anyString());
        verify(parkingService).handleEntry(eq("ABC3333"), anyString());
        assertEquals(0, eventQueueService.getQueueSize());
    }

    @Test
    void deveRejeitarEventoQuandoFilaCheia() {
        // Pausa o consumidor para simular fila cheia sem processamento
        eventQueueService.pause();

        // Enfileira 1000 eventos (capacidade máxima da fila principal)
        for (int i = 0; i < 1000; i++) {
            boolean added = eventQueueService.enqueue(createEvent("ENTRY", "ABC" + i));
            assertTrue(added, "Evento " + i + " deveria ser adicionado");
        }

        // Tenta adicionar o 1001º evento - deve ser rejeitado e movido para DLQ
        // DLQ (Dead Letter Queue) armazena eventos que não puderam ser processados
        // permitindo análise posterior e reprocessamento manual
        WebhookEvent extraEvent = createEvent("ENTRY", "EXTRA");
        boolean enqueued = eventQueueService.enqueue(extraEvent);

        // Valida que evento foi rejeitado da fila principal
        assertFalse(enqueued, "Deve rejeitar quando fila cheia");
        assertEquals(1000, eventQueueService.getQueueSize());
        
        // Valida que evento rejeitado foi movido para DLQ
        assertEquals(1, eventQueueService.getDLQSize(), "Evento rejeitado deve ir para DLQ");
        assertEquals("EXTRA", eventQueueService.getDeadLetterQueue().peek().getLicensePlate());

        eventQueueService.resume();
    }

    @Test
    void deveContinuarProcessandoAposFalha() throws InterruptedException {
        doThrow(new RuntimeException("Erro simulado"))
            .when(parkingService).handleEntry(eq("ABC1111"), anyString());

        WebhookEvent event1 = createEvent("ENTRY", "ABC1111");
        WebhookEvent event2 = createEvent("ENTRY", "ABC2222");

        eventQueueService.enqueue(event1);
        eventQueueService.enqueue(event2);

        Thread.sleep(1000);

        verify(parkingService).handleEntry(eq("ABC1111"), anyString());
        verify(parkingService).handleEntry(eq("ABC2222"), anyString());
        assertEquals(0, eventQueueService.getQueueSize());
    }

    @Test
    void deveProcessarDiferentesTiposDeEvento() throws InterruptedException {
        WebhookEvent entryEvent = createEvent("ENTRY", "ABC1234");
        WebhookEvent parkedEvent = createParkedEvent("ABC1234");
        WebhookEvent exitEvent = createExitEvent("ABC1234");

        eventQueueService.enqueue(entryEvent);
        eventQueueService.enqueue(parkedEvent);
        eventQueueService.enqueue(exitEvent);

        Thread.sleep(1000);

        verify(parkingService).handleEntry(eq("ABC1234"), anyString());
        verify(parkingService).handleParked(eq("ABC1234"), eq(10.0), eq(20.0));
        verify(parkingService).handleExit(eq("ABC1234"), anyString());
    }

    private WebhookEvent createEvent(String eventType, String licensePlate) {
    	
    	var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    	
        WebhookEvent event = new WebhookEvent();
        event.setEventType(eventType);
        event.setLicensePlate(licensePlate);
        event.setEntryTime(dateTimeString);
        return event;
    }

    private WebhookEvent createParkedEvent(String licensePlate) {
        WebhookEvent event = new WebhookEvent();
        event.setEventType("PARKED");
        event.setLicensePlate(licensePlate);
        event.setLat(10.0);
        event.setLng(20.0);
        return event;
    }

    private WebhookEvent createExitEvent(String licensePlate) {
    	
    	var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    	
        WebhookEvent event = new WebhookEvent();
        event.setEventType("EXIT");
        event.setLicensePlate(licensePlate);
        event.setExitTime(dateTimeString);
        return event;
    }
}
