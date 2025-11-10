package com.estapar.parking.dto;

import java.util.List;
import lombok.Data;

/**
 * DTO (Data Transfer Object) para deserialização da configuração da garagem.
 * 
 * Esta classe representa a estrutura JSON retornada pelo simulador externo
 * contendo informações completas sobre setores e vagas do estacionamento.
 * 
 * A configuração é obtida via GET /garage do simulador e contém:
 * - Lista de setores com preços base e capacidades
 * - Lista de vagas individuais com coordenadas geográficas
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.GarageSector
 * @see com.estapar.parking.dto.SpotInfo
 * @see com.estapar.parking.service.GarageService#loadGarageData()
 */
@Data
public class GarageConfig {
    
    /**
     * Lista de setores da garagem com suas configurações.
     * Cada setor contém informações sobre preço base, capacidade máxima
     * e horários de funcionamento.
     */
    private List<GarageSector> garage;
    
    /**
     * Lista de vagas individuais do estacionamento.
     * Cada vaga possui ID único, setor de pertencimento e
     * coordenadas geográficas (latitude/longitude).
     */
    private List<SpotInfo> spots;


}