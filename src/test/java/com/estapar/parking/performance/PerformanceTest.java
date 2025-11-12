package com.estapar.parking.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.estapar.parking.entity.Sector;
import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.ParkingSpotRepository;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// Esta anotação configura um teste de integração com Spring Boot
// webEnvironment = RANDOM_PORT significa que a aplicação será iniciada
// com um servidor web em uma porta aleatória disponível
// Isso é útil para testes que precisam fazer requisições HTTP reais
// e evita conflitos de porta com outras aplicações
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @ActiveProfiles("test") é uma anotação do Spring que indica qual perfil de configuração 
// deve ser ativado durante a execução dos testes. Neste caso, o perfil "test" será usado,
// permitindo carregar configurações específicas para testes (como banco de dados em memória,
// mocks, etc) que são diferentes das configurações de produção
@ActiveProfiles("test")
// @DirtiesContext indica que o contexto do Spring deve ser reiniciado
// Esta anotação é usada quando precisamos garantir que cada teste comece com um estado limpo
// classMode = BEFORE_EACH_TEST_METHOD significa que o contexto será reiniciado antes de cada método de teste
// Isso é útil para testes que modificam o estado da aplicação e precisam começar do zero a cada execução
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Performance - Testes de Carga")
class PerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private SectorRepository sectorRepository;
    
    @Autowired
    private ParkingSpotRepository spotRepository;
    
    @BeforeEach
    void setUp() {
        // Cria dados de teste se não existirem
        if (sectorRepository.findByName("A").isEmpty()) {
            var sector = new Sector("A", BigDecimal.valueOf(40.50), 10);
            sectorRepository.save(sector);
            
            // Cria múltiplas vagas para testes de concorrência
            for (int i = 1; i <= 20; i++) {
                var spot = new ParkingSpot(String.format("A%03d", i), 
                    BigDecimal.valueOf(-23.5505), BigDecimal.valueOf(-46.6333), sector);
                spotRepository.save(spot);
            }
        }
    }

    @Test
    @DisplayName("Rate Limit: deve permitir 5 requests e bloquear o 6º")
    void shouldEnforceRateLimit() throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Envia 5 requisições (deve passar)
        for (int i = 0; i < 5; i++) {
            var payload = String.format("""
                {
                    "event_type": "ENTRY",
                    "license_plate": "TST%04d",
                    "entry_time": "2025-01-20T10:00:00"
                }
                """, i);
            
            var entity = new HttpEntity<>(payload, headers);
            var response = restTemplate.postForEntity("/webhook", entity, String.class);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }
        
        // 6ª requisição deve ser bloqueada
        var payload = """
            {
                "event_type": "ENTRY",
                "license_plate": "TST9999",
                "entry_time": "2025-01-20T10:00:00"
            }
            """;
        
        var entity = new HttpEntity<>(payload, headers);
        var response = restTemplate.postForEntity("/webhook", entity, String.class);
        
        // Deve retornar 429 (Too Many Requests)
        assertThat(response.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    @DisplayName("Rate Limit: deve resetar após janela de tempo")
    void shouldResetAfterTimeWindow() throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Esgota o limite (5 requests)
        for (int i = 0; i < 5; i++) {
            var payload = String.format("""
                {
                    "event_type": "ENTRY",
                    "license_plate": "RST%04d",
                    "entry_time": "2025-01-20T10:00:00"
                }
                """, i);
            
            var entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity("/webhook", entity, String.class);
        }
        
        // Aguarda janela de tempo (10 segundos + margem)
        Thread.sleep(11000);
        
        // Nova requisição deve passar após reset
        var payload = """
            {
                "event_type": "ENTRY",
                "license_plate": "RST9999",
                "entry_time": "2025-01-20T10:00:00"
            }
            """;
        
        var entity = new HttpEntity<>(payload, headers);
        var response = restTemplate.postForEntity("/webhook", entity, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }
    

}