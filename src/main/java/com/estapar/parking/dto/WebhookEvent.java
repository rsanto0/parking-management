package com.estapar.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para eventos de webhook enviados pelo simulador de estacionamento.
 * 
 * Esta classe representa os diferentes tipos de eventos que podem ocorrer
 * durante a movimentação de veículos no estacionamento, incluindo entrada,
 * estacionamento e saída.
 * 
 * Utiliza Bean Validation para garantir integridade dos dados recebidos
 * e anotações Jackson para mapear campos JSON em snake_case para
 * propriedades Java em camelCase.
 * 
 * Tipos de eventos suportados:
 * - ENTRY: Veículo chegando na cancela
 * - PARKED: Veículo estacionado em vaga
 * - EXIT: Veículo saindo do estacionamento
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.controller.WebhookController
 * @see jakarta.validation.Valid
 */
@Data
public class WebhookEvent {
    
    /**
     * Placa do veículo no formato brasileiro.
     * Suporta tanto formato antigo (ABC1234) quanto Mercosul (ABC1A34).
     * 
     * Validações aplicadas:
     * - @NotBlank: campo obrigatório
     * - @Pattern: formato válido de placa brasileira
     * 
     * Mapeado do campo JSON "license_plate".
     */
    @JsonProperty("license_plate")
    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$", message = "Invalid license plate format")
    private String licensePlate;

    /**
     * Timestamp de entrada do veículo em formato ISO 8601.
     * Utilizado apenas em eventos do tipo ENTRY para registrar
     * o momento exato da chegada na cancela.
     * 
     * Mapeado do campo JSON "entry_time".
     * 
     * @example "2025-01-20T10:00:00.000Z"
     */
    @JsonProperty("entry_time")
    private String entryTime;

    /**
     * Timestamp de saída do veículo em formato ISO 8601.
     * Utilizado apenas em eventos do tipo EXIT para calcular
     * tempo de permanência e valor a ser cobrado.
     * 
     * Mapeado do campo JSON "exit_time".
     * 
     * @example "2025-01-20T12:30:00.000Z"
     */
    @JsonProperty("exit_time")
    private String exitTime;

    /**
     * Latitude da posição onde o veículo estacionou.
     * Utilizada apenas em eventos do tipo PARKED para
     * confirmar a localização exata da vaga ocupada.
     * 
     * Mapeado do campo JSON "lat".
     * 
     * @example -23.561684
     */
    @JsonProperty("lat")
    private Double lat;

    /**
     * Longitude da posição onde o veículo estacionou.
     * Utilizada apenas em eventos do tipo PARKED para
     * confirmar a localização exata da vaga ocupada.
     * 
     * Mapeado do campo JSON "lng".
     * 
     * @example -46.655981
     */
    @JsonProperty("lng")
    private Double lng;

    /**
     * Tipo do evento de movimentação do veículo.
     * 
     * Valores válidos:
     * - "ENTRY": Veículo chegando na cancela
     * - "PARKED": Veículo estacionado em vaga
     * - "EXIT": Veículo saindo do estacionamento
     * 
     * Validações aplicadas:
     * - @NotBlank: campo obrigatório
     * - @Pattern: apenas valores válidos aceitos
     * 
     * Mapeado do campo JSON "event_type".
     */
    @JsonProperty("event_type")
    @NotBlank(message = "Event type is required")
    @Pattern(regexp = "^(ENTRY|PARKED|EXIT)$", message = "Invalid event type")
    private String eventType;


}