package com.estapar.parking.controller;

import com.estapar.parking.dto.WebhookEvent;
import com.estapar.parking.service.EventQueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebhookController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@ActiveProfiles("test")
@DisplayName("WebhookController - Eventos do Simulador")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventQueueService eventQueueService;

    @Test
    @DisplayName("ENTRY: deve enfileirar entrada de veículo")
    void shouldProcessEntryEvent() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("ENTRY");
        event.setLicensePlate("ABC1234");
        event.setEntryTime(dateTimeString);

        when(eventQueueService.enqueue(any(WebhookEvent.class))).thenReturn(true);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        verify(eventQueueService).enqueue(any(WebhookEvent.class));
    }

    @Test
    @DisplayName("EXIT: deve enfileirar saída de veículo")
    void shouldProcessExitEvent() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("EXIT");
        event.setLicensePlate("DEF5678");
        event.setExitTime(dateTimeString);

        when(eventQueueService.enqueue(any(WebhookEvent.class))).thenReturn(true);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        verify(eventQueueService).enqueue(any(WebhookEvent.class));
    }

    @Test
    @DisplayName("Fila cheia: deve retornar 503")
    void shouldReturn503WhenQueueFull() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("ENTRY");
        event.setLicensePlate("ABC1D23");
        event.setEntryTime(dateTimeString);

        when(eventQueueService.enqueue(any(WebhookEvent.class))).thenReturn(false);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isServiceUnavailable());
    }
}