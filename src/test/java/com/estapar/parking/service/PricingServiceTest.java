package com.estapar.parking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PricingService - Cálculos de Preço")
class PricingServiceTest {
    
    private final PricingService pricingService = new PricingService();
    private final BigDecimal SETOR_A_PRICE = BigDecimal.valueOf(40.50);
    private final BigDecimal SETOR_B_PRICE = BigDecimal.valueOf(4.10);

    @Test
    @DisplayName("Tolerância: ≤ 30 minutos = gratuito")
    void shouldNotChargeWithinTolerance() {
        var entry = LocalDateTime.of(2025, 1, 20, 10, 0);
        var exit = LocalDateTime.of(2025, 1, 20, 10, 30);
        
        var price = pricingService.calculatePrice(entry, exit, 50.0, SETOR_B_PRICE);
        
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @DisplayName("Arredondamento: sempre para cima")
    // Testa arredondamento para cima:
    // - 31 minutos é arredondado para 1 hora completa
    // - 60 minutos é exatamente 1 hora
    // - 61 minutos é arredondado para 2 horas completas  
    // - 125 minutos é arredondado para 3 horas completas
    @CsvSource({        
        "31, 1",  // 31 min = 1 hora
        "60, 1",  // 60 min = 1 hora  
        "61, 2",  // 61 min = 2 horas
        "125, 3"  // 125 min = 3 horas
    })
    void shouldRoundUpToFullHours(int minutes, int expectedHours) {
        var entry = LocalDateTime.of(2025, 1, 20, 10, 0);
        var exit = entry.plusMinutes(minutes);
        
        var price = pricingService.calculatePrice(entry, exit, 50.0, SETOR_B_PRICE);
        var expectedPrice = SETOR_B_PRICE.multiply(BigDecimal.valueOf(expectedHours));
        
        assertThat(price).isEqualByComparingTo(expectedPrice);
    }

    @ParameterizedTest
    @DisplayName("Preço dinâmico por lotação - primeira hora")
    @CsvSource({
        "10.0, 0.90",   // 0-25%: -10%
        "25.0, 0.90",   // 0-25%: -10%
        "30.0, 1.00",   // 26-50%: normal
        "50.0, 1.00",   // 26-50%: normal
        "60.0, 1.10",   // 51-75%: +10%
        "75.0, 1.10",   // 51-75%: +10%
        "80.0, 1.25",   // 76-100%: +25%
        "100.0, 1.25"   // 76-100%: +25%
    })
    void shouldApplyDynamicPricingForFirstHour(double occupancy, double multiplier) {
        var entry = LocalDateTime.of(2025, 1, 20, 10, 0);
        var exit = LocalDateTime.of(2025, 1, 20, 11, 0); // 1 hora exata
        
        var price = pricingService.calculatePrice(entry, exit, occupancy, SETOR_B_PRICE);
        var expectedPrice = SETOR_B_PRICE.multiply(BigDecimal.valueOf(multiplier));
        
        assertThat(price).isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("Horas adicionais: preço base fixo")
    void shouldUseBasePriceForAdditionalHours() {
        var entry = LocalDateTime.of(2025, 1, 20, 10, 0);
        var exit = LocalDateTime.of(2025, 1, 20, 12, 30); // 2.5h = 3h
        
        // Lotação alta (80%) = primeira hora com +25%
        var price = pricingService.calculatePrice(entry, exit, 80.0, SETOR_B_PRICE);
        
        var firstHour = SETOR_B_PRICE.multiply(BigDecimal.valueOf(1.25)); // +25%
        var additionalHours = SETOR_B_PRICE.multiply(BigDecimal.valueOf(2)); // 2h no preço base
        var expectedTotal = firstHour.add(additionalHours);
        
        assertThat(price).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Cenário real: Setor A, lotação baixa, 2 horas")
    void shouldCalculateRealScenarioSectorA() {
        var entry = LocalDateTime.of(2025, 1, 20, 14, 15);
        var exit = LocalDateTime.of(2025, 1, 20, 16, 20); // 125 min = 3h
        
        var price = pricingService.calculatePrice(entry, exit, 20.0, SETOR_A_PRICE);
        
        // Primeira hora: R$ 40,50 * 0,90 = R$ 36,45
        // Horas adicionais: R$ 40,50 * 2 = R$ 81,00
        // Total: R$ 117,45
        var expectedPrice = BigDecimal.valueOf(117.45);
        
        assertThat(price).isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("Cenário real: Setor B, lotação alta, permanência longa")
    void shouldCalculateRealScenarioSectorB() {
        var entry = LocalDateTime.of(2025, 1, 20, 8, 0);
        var exit = LocalDateTime.of(2025, 1, 20, 18, 30); // 10.5h = 11h
        
        var price = pricingService.calculatePrice(entry, exit, 85.0, SETOR_B_PRICE);
        
        // Primeira hora: R$ 4,10 * 1,25 = R$ 5,125
        // Horas adicionais: R$ 4,10 * 10 = R$ 41,00
        // Total: R$ 46,125
        var expectedPrice = BigDecimal.valueOf(46.125);
        
        assertThat(price).isEqualByComparingTo(expectedPrice);
    }
}