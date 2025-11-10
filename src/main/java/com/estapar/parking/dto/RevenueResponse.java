package com.estapar.parking.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respostas de consulta de receita do estacionamento.
 * 
 * Esta classe representa o resultado de uma consulta de faturamento,
 * contendo o valor total calculado, moeda e timestamp da consulta
 * para auditoria e rastreabilidade.
 * 
 * O valor da receita é calculado somando todos os valores cobrados
 * de veículos que saíram do setor especificado na data consultada.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.dto.RevenueRequest
 * @see com.estapar.parking.controller.RevenueController
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueResponse {
    
    /**
     * Valor total da receita calculada.
     * Soma de todos os valores cobrados de veículos que saíram
     * do setor na data especificada, com precisão decimal.
     */
    private BigDecimal amount;
    
    /**
     * Código da moeda utilizada (sempre "BRL" para Real Brasileiro).
     * Padronização internacional ISO 4217 para identificação da moeda.
     */
    private String currency;
    
    /**
     * Timestamp da consulta em formato ISO 8601.
     * Registra o momento exato em que a consulta foi processada
     * para auditoria e rastreabilidade das operações.
     */
    private String timestamp;


}