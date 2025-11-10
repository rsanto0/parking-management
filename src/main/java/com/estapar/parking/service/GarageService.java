package com.estapar.parking.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.estapar.parking.dto.GarageConfig;
import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.entity.Sector;
import com.estapar.parking.repository.ParkingSpotRepository;
import com.estapar.parking.repository.SectorRepository;

/**
 * Serviço responsável pela gestão e inicialização da garagem.
 * 
 * Esta classe gerencia a configuração inicial do estacionamento obtendo
 * dados do simulador externo e calculando métricas de ocupação em tempo real.
 * 
 * Responsabilidades principais:
 * - Carregar configuração de setores e vagas do simulador na inicialização
 * - Calcular taxa de ocupação atual do estacionamento
 * - Verificar se o estacionamento está lotado
 * - Manter sincronização entre dados do simulador e banco local
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.entity.Sector
 * @see com.estapar.parking.entity.ParkingSpot
 * @see com.estapar.parking.dto.GarageConfig
 */
@Service
public class GarageService {
    
    /**
     * Logger para registro de operações de inicialização e cálculos.
     * Utilizado para auditoria e debugging do carregamento de dados.
     */
    private static final Logger log = LoggerFactory.getLogger(GarageService.class);

    /**
     * URL base do simulador externo obtida da configuração.
     * Utilizada para fazer requisições HTTP ao endpoint /garage
     * para obter configuração de setores e vagas.
     */
    @Value("${simulator.url}")
    private String simulatorUrl;

    /**
     * Repositório para persistência de setores no banco de dados.
     * Utilizado para salvar e consultar informações de setores
     * obtidas do simulador.
     */
    private final SectorRepository sectorRepository;
    
    /**
     * Repositório para persistência de vagas no banco de dados.
     * Utilizado para salvar vagas individuais e calcular
     * métricas de ocupação.
     */
    private final ParkingSpotRepository spotRepository;
    
    /**
     * Cliente HTTP para comunicação com o simulador externo.
     * Configurado via injeção de dependência para fazer
     * requisições REST ao simulador.
     */
    private final RestTemplate restTemplate;

    /**
     * Construtor para injeção de dependências.
     * 
     * @param sectorRepository repositório de setores
     * @param spotRepository repositório de vagas
     * @param restTemplate cliente HTTP para comunicação externa
     */
    public GarageService(SectorRepository sectorRepository, ParkingSpotRepository spotRepository, RestTemplate restTemplate) {
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Carrega dados da garagem do simulador na inicialização da aplicação.
     * 
     * Este método é executado automaticamente quando a aplicação Spring Boot
     * termina de inicializar, garantindo que os dados do simulador sejam
     * carregados antes de processar eventos de veículos.
     * 
     * Processo de carregamento:
     * 1. Faz requisição GET para {simulatorUrl}/garage
     * 2. Deserializa resposta JSON para GarageConfig
     * 3. Salva setores no banco (ou atualiza se já existem)
     * 4. Salva vagas individuais com coordenadas geográficas
     * 5. Registra logs de progresso e estatísticas
     * 
     * @apiNote Método executado automaticamente via @EventListener
     * @implNote Utiliza padrão "upsert" para setores (insert ou update)
     * @see org.springframework.boot.context.event.ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadGarageData() {
        log.info("Carregando dados da garagem do simulador...");
        
        try {
            // Obtém configuração completa do simulador
            var config = restTemplate.getForObject(simulatorUrl + "/garage", GarageConfig.class);
            
            if (config == null) {
                log.error("Falha ao carregar configuração da garagem");
                return;
            }

            // Processa e salva setores (batch insert & update)
            var sectors = config.getGarage().stream()
                .map(gs -> {
                    var sector = sectorRepository.findByName(gs.getSector())
                        .orElse(new Sector(gs.getSector(), BigDecimal.valueOf(gs.getBasePrice()), gs.getMaxCapacity()));
                    log.info("Setor {} carregado: {} vagas, preço base R${}", gs.getSector(), gs.getMaxCapacity(), gs.getBasePrice());
                    return sector;
                })
                .toList();
            sectorRepository.saveAll(sectors);

            // Processa e salva vagas individuais (insert)
            var spots = config.getSpots().stream()
                .map(s -> {
                    Sector sector = sectorRepository.findByName(s.getSector()).orElseThrow();
                    return new ParkingSpot(s.getId(), 
                        BigDecimal.valueOf(s.getLat()), 
                        BigDecimal.valueOf(s.getLng()), 
                        sector);
                })
                .toList();
            spotRepository.saveAll(spots);

            log.info("Total de {} vagas carregadas", config.getSpots().size());
        } catch (Exception e) {
            log.error("Erro ao carregar dados da garagem: {}", e.getMessage());
        }
    }

    /**
     * Calcula a taxa de ocupação atual do estacionamento.
     * 
     * Utiliza contagem em tempo real de vagas ocupadas versus
     * total de vagas para determinar o percentual de lotação.
     * 
     * Este valor é utilizado para:
     * - Aplicação de regras de preço dinâmico
     * - Decisões de entrada de novos veículos
     * - Relatórios de utilização
     * 
     * @return percentual de ocupação (0.0 a 100.0)
     * @example 75.5 (representa 75,5% de ocupação)
     * 
     * @implNote Retorna 0 se não houver vagas cadastradas
     */
    public double getOccupancyRate() {
        long totalSpots = spotRepository.count();
        long occupiedSpots = spotRepository.countByOccupiedTrue();
        return totalSpots > 0 ? (occupiedSpots * 100.0) / totalSpots : 0;
    }

    /**
     * Verifica se o estacionamento está completamente lotado.
     * 
     * Compara o número total de vagas com vagas ocupadas
     * para determinar se há disponibilidade para novos veículos.
     * 
     * Utilizado para:
     * - Bloquear entrada de novos veículos quando lotado
     * - Exibir mensagem "Lotado" na cancela
     * - Otimizar consultas de vagas disponíveis
     * 
     * @return true se todas as vagas estão ocupadas, false caso contrário
     * 
     * @implNote Considera lotado apenas quando 100% das vagas estão ocupadas
     */
    public boolean isFull() {
        return spotRepository.count() == spotRepository.countByOccupiedTrue();
    }
}