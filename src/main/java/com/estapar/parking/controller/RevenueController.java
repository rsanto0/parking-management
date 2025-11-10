package com.estapar.parking.controller;

import com.estapar.parking.dto.RevenueRequest;
import com.estapar.parking.dto.RevenueResponse;
import com.estapar.parking.service.ParkingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller REST para consulta de receita do sistema de estacionamento.
 * 
 * Este controller fornece endpoints para consulta de faturamento
 * por setor e data, permitindo análise financeira detalhada do sistema.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.service.ParkingService
 * @see com.estapar.parking.dto.RevenueRequest
 * @see com.estapar.parking.dto.RevenueResponse
 */
@RestController
@RequestMapping("/revenue")
public class RevenueController {
    
    /**
     * Serviço de estacionamento responsável pelos cálculos de receita.
     * Injetação de dependência via construtor para garantir imutabilidade.
     */
    private final ParkingService parkingService;

    /**
     * Construtor para injeção de dependência do serviço de estacionamento.
     * 
     * @param parkingService serviço responsável pelos cálculos de receita e gestão de veículos
     * @throws IllegalArgumentException se parkingService for null
     */
    public RevenueController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * Consulta a receita total de um setor específico em uma data.
     * 
     * Este endpoint calcula e retorna o faturamento total gerado por um setor
     * do estacionamento em uma data específica, considerando todos os veículos
     * que saíram naquele dia e tiveram valores cobrados.
     * 
     * O cálculo inclui:
     * - Veículos que saíram na data especificada
     * - Valores calculados com base no tempo de permanência
     * - Aplicação de regras de preço dinâmico por lotação
     * - Tolerância de 30 minutos
     * 
     * @param request objeto contendo os parâmetros de consulta:
     *                - sector: nome do setor (A, B, C, D)
     *                - date: data no formato YYYY-MM-DD
     * @return ResponseEntity contendo:
     *         - amount: valor total da receita em BigDecimal
     *         - currency: moeda (sempre "BRL")
     *         - timestamp: timestamp da consulta em formato ISO
     * 
     * @example
     * GET /revenue?sector=A&date=2025-01-20
     * 
     * Response:
     * {
     *   "amount": 125.50,
     *   "currency": "BRL",
     *   "timestamp": "2025-01-20T15:30:45.123Z"
     * }
     * 
     * @implNote Utiliza timezone UTC para timestamp de resposta
     * @see com.estapar.parking.service.ParkingService#getRevenue(String, String)
     */
    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(
            @RequestParam String sector, 
            @RequestParam String date) {
    	
    	/* Neste caso não convem trocar pq o tipo retornado não é obvio
    	*   var ? = parkingService.getRevenue(request.getSector(), request.getDate());
    	*/
        // Calcula receita através do serviço de estacionamento
        BigDecimal amount = parkingService.getRevenue(sector, date);
        
        
        //Pq estou trocando a sintaxe
		/*
		 * é preferivel usar quando: 
		 * 	o tipo é óbvio pelo contexto (format() retorna String) 
		 *  menos verboso
		 *  estilo moderno
		 */

        //String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        // Gera timestamp atual em formato ISO para auditoria
        var timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        
        // Constrói resposta padronizada com moeda brasileira
        var response = new RevenueResponse(amount, "BRL", timestamp);
        return ResponseEntity.ok(response);
    }
}