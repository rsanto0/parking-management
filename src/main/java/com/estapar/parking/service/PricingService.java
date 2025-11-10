package com.estapar.parking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Serviço responsável pelos cálculos de preço dinâmico do estacionamento.
 * 
 * Esta classe implementa as regras de negócio para cobrança de estacionamento,
 * incluindo tolerância de tempo, preços variáveis por lotação e cálculos
 * precisos baseados no tempo de permanência.
 * 
 * Regras implementadas:
 * - Tolerância de 30 minutos (não cobra)
 * - Preço dinâmico da primeira hora baseado na lotação
 * - Horas adicionais sempre no preço base do setor
 * - Arredondamento para cima (31 min = 1 hora)
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.service.ParkingService
 * @see com.estapar.parking.entity.Vehicle
 */
@Service
public class PricingService {
    
    /**
     * Logger para registro detalhado de cálculos de preço.
     * Utilizado para auditoria, debugging e rastreamento
     * de valores cobrados dos veículos.
     */
    private static final Logger log = LoggerFactory.getLogger(PricingService.class);
    
    /**
     * Tolerância em minutos para cobrança.
     * Veículos que permanecem até este tempo não são cobrados.
     * Valor: 30 minutos conforme regra de negócio.
     */
    private static final long TOLERANCE_MINUTES = 30; // Tolerância de 30 minutos

    /**
     * Calcula o preço total a ser cobrado de um veículo.
     * 
     * Este método implementa a lógica completa de cobrança considerando:
     * 
     * 1. **Tolerância**: Até 30 minutos = gratuito
     * 2. **Arredondamento**: Sempre para cima (31 min = 1 hora)
     * 3. **Preço dinâmico**: Primeira hora varia com lotação
     * 4. **Horas adicionais**: Preço base fixo do setor
     * 
     * Fórmula de cálculo:
     * - Total = Primeira Hora (dinâmica) + Horas Adicionais (fixas)
     * - Primeira hora: basePrice * multiplicador de lotação
     * - Horas adicionais: basePrice * número de horas
     * 
     * @param entryTime momento de entrada do veículo
     * @param exitTime momento de saída do veículo
     * @param occupancyRate taxa de ocupação atual (0-100)
     * @param sectorBasePrice preço base por hora do setor
     * @return valor total a ser cobrado com precisão decimal
     * 
     * @example
     * Entrada: 10:00, Saída: 12:30, Lotação: 20%, Preço: R$4,10
     * Cálculo: 150 min = 3 horas
     * Primeira hora: R$4,10 * 0,90 = R$3,69
     * Horas adicionais: R$4,10 * 2 = R$8,20
     * Total: R$11,89
     * 
     * @implNote Utiliza BigDecimal para precisão monetária
     * @see #calculateFirstHourPrice(double, BigDecimal)
     */
    public BigDecimal calculatePrice(LocalDateTime entryTime, LocalDateTime exitTime, double occupancyRate, BigDecimal sectorBasePrice) {
        // Calcula tempo total de permanência em minutos
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        
        // Aplica tolerância de 30 minutos
        if (totalMinutes <= TOLERANCE_MINUTES) {
            log.debug("Permanência de {} minutos - Dentro da tolerância", totalMinutes);
            return BigDecimal.ZERO;
        }

        // Após tolerância, cobra horas completas baseado no tempo total
        // Arredonda para cima: 31 min = 1 hora, 61 min = 2 horas
        long totalHours = (totalMinutes + 59) / 60; // Arredonda para cima
        
        // Calcula preço da primeira hora com base na lotação
        BigDecimal firstHourPrice = calculateFirstHourPrice(occupancyRate, sectorBasePrice);
        BigDecimal firstHourCharge = firstHourPrice; // Primeira hora sempre completa
        
        // Calcula cobrança de horas adicionais (se houver)
        var additionalCharge = BigDecimal.ZERO;
        if (totalHours > 1) {
            long additionalHours = totalHours - 1;
            additionalCharge = sectorBasePrice.multiply(BigDecimal.valueOf(additionalHours));
        }

        // Soma primeira hora + horas adicionais
        BigDecimal total = firstHourCharge.add(additionalCharge);
        
        // Log detalhado para auditoria
        log.info("Cálculo: {} min total, lotação {}%, primeira hora R${}, adicional R${}, total R${}", 
            totalMinutes, String.format("%.1f", occupancyRate), firstHourPrice, additionalCharge, total);
        
        return total;
    }

    /**
     * Calcula o preço da primeira hora baseado na taxa de ocupação.
     * 
     * Implementa sistema de preço dinâmico onde o valor da primeira hora
     * varia conforme a lotação do estacionamento para otimizar utilização
     * e receita.
     * 
     * Faixas de lotação e multiplicadores:
     * - 0% a 25%: Desconto de 10% (0.90x) - Incentiva uso
     * - 26% a 50%: Preço normal (1.00x) - Equilíbrio
     * - 51% a 75%: Acréscimo de 10% (1.10x) - Desestimula uso
     * - 76% a 100%: Acréscimo de 25% (1.25x) - Máximo controle
     * 
     * @param occupancyRate taxa de ocupação atual (0.0 a 100.0)
     * @param basePrice preço base por hora do setor
     * @return preço ajustado da primeira hora
     * 
     * @example
     * Preço base: R$10,00, Lotação: 80%
     * Resultado: R$10,00 * 1,25 = R$12,50
     * 
     * @implNote Horas adicionais sempre usam preço base (sem multiplicação)
     */
    private BigDecimal calculateFirstHourPrice(double occupancyRate, BigDecimal basePrice) {
        if (occupancyRate <= 25) {
            // Lotação baixa: desconto de 10% para incentivar uso
            return basePrice.multiply(BigDecimal.valueOf(0.90));
        } else if (occupancyRate <= 50) {
            // Lotação média: preço normal
            return basePrice;
        } else if (occupancyRate <= 75) {
            // Lotação alta: acréscimo de 10% para desestimular
            return basePrice.multiply(BigDecimal.valueOf(1.10));
        } else {
            // Lotação muito alta: acréscimo de 25% para controle máximo
            return basePrice.multiply(BigDecimal.valueOf(1.25));
        }
    }
}