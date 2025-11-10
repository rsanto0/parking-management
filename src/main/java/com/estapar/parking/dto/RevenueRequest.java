package com.estapar.parking.dto;

import lombok.Data;

/**
 * DTO para requisições de consulta de receita do estacionamento.
 * 
 * Esta classe representa os parâmetros necessários para consultar
 * o faturamento de um setor específico em uma data determinada.
 * 
 * Utilizada no endpoint protegido GET /revenue para filtrar
 * e calcular a receita baseada em veículos que saíram do
 * estacionamento com valores cobrados.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.RevenueResponse
 * @see com.estapar.parking.controller.RevenueController
 */
@Data
public class RevenueRequest {
	
	public RevenueRequest() {
	
	}
    
	public RevenueRequest(String date, String sector) {
		super();
		this.date = date;
		this.sector = sector;
	}



	/**
     * Data para consulta de receita no formato YYYY-MM-DD.
     * Utilizada para filtrar veículos que saíram nesta data
     * específica e tiveram valores cobrados.
     * 
     * @example "2025-01-20"
     */
    private String date;
    
    /**
     * Identificador do setor para consulta de receita.
     * Filtra apenas veículos que estacionaram no setor
     * especificado (A, B, C, D, etc.).
     * 
     * @example "A", "B"
     */
    private String sector;


}