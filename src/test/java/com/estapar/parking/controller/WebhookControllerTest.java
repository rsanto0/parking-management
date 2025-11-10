package com.estapar.parking.controller;

import com.estapar.parking.dto.WebhookEvent;
import com.estapar.parking.service.ParkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebhookController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("WebhookController - Eventos do Simulador")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingService parkingService;

    @Test
    @DisplayName("ENTRY: deve processar entrada de veículo")
    void shouldProcessEntryEvent() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("ENTRY");
        event.setLicensePlate("ABC1234");
        event.setEntryTime(dateTimeString);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        verify(parkingService).handleEntry(event.getLicensePlate(), event.getEntryTime());
    }

    @Test
    @DisplayName("EXIT: deve processar saída de veículo")
    void shouldProcessExitEvent() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("EXIT");
        event.setLicensePlate("DEF5678");
        event.setExitTime(dateTimeString);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        verify(parkingService).handleExit(event.getLicensePlate(), event.getExitTime());
    }

    @Test
    @DisplayName("Evento desconhecido: deve rejeitar com 400")
    void shouldIgnoreUnknownEventType() throws Exception {
        var event = new WebhookEvent();
        event.setEventType("UNKNOWN");
        event.setLicensePlate("GHI9012");

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(parkingService);
    }

    @Test
    @DisplayName("Erro: deve retornar 400")
    void shouldReturn400OnError() throws Exception {
        var dateTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        var event = new WebhookEvent();
        event.setEventType("ENTRY");
        event.setLicensePlate("ERROR123");
        event.setEntryTime(dateTimeString);

        doThrow(new RuntimeException("Erro de processamento"))
                .when(parkingService).handleEntry(anyString(), any());

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }
}