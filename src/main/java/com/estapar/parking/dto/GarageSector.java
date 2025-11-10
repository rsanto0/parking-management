package com.estapar.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO representando um setor da garagem com suas configurações de preço e capacidade.
 * 
 * Esta classe mapeia as informações de um setor individual retornadas pelo
 * simulador, incluindo identificação, preço base por hora e capacidade máxima.
 * 
 * Utiliza anotações Jackson para mapear nomes de propriedades JSON que
 * seguem convenção snake_case para propriedades Java em camelCase.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.GarageConfig
 * @see com.estapar.parking.entity.Sector
 */
@Data
public class GarageSector {
    
    /**
     * Identificador do setor (A, B, C, D, etc.).
     * Usado para organizar logicamente as vagas do estacionamento.
     */
    private String sector;
    
    /**
     * Preço base por hora do setor em reais.
     * Este valor é usado como base para cálculos de preço dinâmico
     * considerando lotação e tempo de permanência.
     * 
     * Mapeado do campo JSON "base_price" usando @JsonProperty.
     */
    @JsonProperty("base_price")
    private Double basePrice;
    
    /**
     * Capacidade máxima de vagas do setor.
     * Representa o número total de vagas disponíveis neste setor
     * para cálculos de lotação e disponibilidade.
     * 
     * Mapeado do campo JSON "max_capacity" usando @JsonProperty.
     */
    @JsonProperty("max_capacity")
    private Integer maxCapacity;


}