package com.estapar.parking.dto;

import lombok.Data;

/**
 * DTO representando informações de uma vaga individual do estacionamento.
 * 
 * Esta classe contém dados detalhados de cada vaga obtidos do simulador,
 * incluindo identificação única, setor de pertencimento e localização
 * geográfica precisa.
 * 
 * As coordenadas geográficas são utilizadas para:
 * - Confirmação de estacionamento (evento PARKED)
 * - Localização física da vaga no estacionamento
 * - Integração com sistemas de navegação
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.GarageConfig
 * @see com.estapar.parking.entity.ParkingSpot
 */
@Data
public class SpotInfo {
    
    /**
     * Identificador único da vaga no sistema.
     * Usado para referenciar especificamente esta vaga
     * em operações de estacionamento e liberação.
     */
    private Long id;
    
    /**
     * Identificador do setor ao qual a vaga pertence.
     * Relaciona a vaga com as configurações de preço
     * e políticas do setor correspondente.
     */
    private String sector;
    
    /**
     * Latitude da localização geográfica da vaga.
     * Coordenada em graus decimais (ex: -23.561684)
     * usada para localização precisa da vaga.
     */
    private Double lat;
    
    /**
     * Longitude da localização geográfica da vaga.
     * Coordenada em graus decimais (ex: -46.655981)
     * usada para localização precisa da vaga.
     */
    private Double lng;


}