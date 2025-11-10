package com.estapar.parking.service;

import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.entity.Vehicle;
import com.estapar.parking.repository.ParkingSpotRepository;
import com.estapar.parking.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

/**
 * Serviço principal para gestão de operações de estacionamento.
 * 
 * Esta classe coordena todas as operações relacionadas à movimentação de
 * veículos no estacionamento, desde a entrada até a saída e cobrança.
 * 
 * Responsabilidades principais:
 * - Processar eventos de entrada, estacionamento e saída
 * - Gerenciar alocação de vagas de forma aleatória
 * - Aplicar regras de negócio e cálculos de preço
 * - Controlar status de ocupação das vagas
 * - Calcular receita por setor e data
 * 
 * Fluxo de operação:
 * 1. ENTRY: Veículo chega, verifica lotação, aloca vaga
 * 2. PARKED: Confirmação de estacionamento (opcional)
 * 3. EXIT: Veículo sai, calcula preço, libera vaga
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.entity.Vehicle
 * @see com.estapar.parking.entity.ParkingSpot
 * @see com.estapar.parking.service.PricingService
 */
@Service
public class ParkingService {
    
    /**
     * Logger para registro de operações de estacionamento.
     * Utilizado para auditoria, debugging e rastreamento
     * de movimentações de veículos.
     */
    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

    /**
     * Repositório para persistência de dados de veículos.
     * Utilizado para salvar, consultar e atualizar registros
     * de veículos no sistema.
     */
    private final VehicleRepository vehicleRepository;
    
    /**
     * Repositório para gestão de vagas de estacionamento.
     * Utilizado para consultar disponibilidade e atualizar
     * status de ocupação das vagas.
     */
    private final ParkingSpotRepository spotRepository;
    
    /**
     * Serviço para cálculos de lotação e disponibilidade.
     * Utilizado para verificar se o estacionamento está lotado
     * e obter taxas de ocupação para preços dinâmicos.
     */
    private final GarageService garageService;
    
    /**
     * Serviço para cálculos de preço dinâmico.
     * Utilizado para calcular valores a serem cobrados
     * baseados em tempo, lotação e regras de negócio.
     */
    private final PricingService pricingService;
    
    /**
     * Gerador de números aleatórios para seleção de vagas.
     * Utilizado para distribuir veículos aleatoriamente
     * entre as vagas disponíveis.
     */
    private final Random random = new Random();

    /**
     * Construtor para injeção de dependências.
     * 
     * @param vehicleRepository repositório de veículos
     * @param spotRepository repositório de vagas
     * @param garageService serviço de gestão da garagem
     * @param pricingService serviço de cálculos de preço
     */
    public ParkingService(VehicleRepository vehicleRepository, ParkingSpotRepository spotRepository,
                         GarageService garageService, PricingService pricingService) {
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.garageService = garageService;
        this.pricingService = pricingService;
    }

    /**
     * Processa evento de entrada de veículo no estacionamento.
     * 
     * Este método implementa a lógica completa de entrada:
     * 
     * 1. **Captura da placa**: Registra identificação do veículo
     * 2. **Verificação de lotação**: Bloqueia se estiver lotado
     * 3. **Cálculo de preço vigente**: Informa valor baseado na lotação
     * 4. **Alocação de vaga**: Seleciona vaga aleatória disponível
     * 5. **Registro**: Salva veículo e marca vaga como ocupada
     * 
     * @param licensePlate placa do veículo no formato brasileiro
     * @param entryTime timestamp de entrada em formato ISO 8601
     * @throws RuntimeException se estacionamento estiver lotado
     * @throws RuntimeException se não houver vagas disponíveis
     * 
     * @example
     * handleEntry("ABC1234", "2025-01-20T10:00:00.000Z")
     * 
     * @apiNote Método transacional - rollback automático em caso de erro
     * @implNote Vaga é selecionada aleatoriamente entre as disponíveis
     */
    @Transactional
    public void handleEntry(String licensePlate, String entryTime) {
        log.info("Placa capturada: {}", licensePlate);

        // Verifica se estacionamento está lotado
        if (garageService.isFull()) {
            log.warn("Estacionamento LOTADO - entrada negada para {}", licensePlate);
            throw new RuntimeException("Lotado");
        }

        // Calcula e registra lotação atual
        double occupancy = garageService.getOccupancyRate();
        log.info("Lotação atual: {}%", String.format("%.1f", occupancy));

        // Converte timestamp e cria registro do veículo
        LocalDateTime entry = LocalDateTime.parse(entryTime);
        var vehicle = new Vehicle(licensePlate, entry);
        
        // Seleciona vaga aleatória e aloca para o veículo
        ParkingSpot spot = selectRandomAvailableSpot();
        vehicle.setParkingSpot(spot);
        vehicle.setStatus(Vehicle.VehicleStatus.PARKED);
        
        // Marca vaga como ocupada e salva dados
        spot.setOccupied(true);
        spotRepository.save(spot);
        vehicleRepository.save(vehicle);

        log.info("Veículo {} estacionado na vaga {} do setor {}", licensePlate, spot.getId(), spot.getSector().getName());
    }

    /**
     * Processa evento de confirmação de estacionamento.
     * 
     * Este evento é enviado pelo simulador quando o veículo
     * efetivamente estaciona na vaga, incluindo coordenadas
     * geográficas da posição final.
     * 
     * Atualmente apenas registra o evento para auditoria,
     * mas pode ser expandido para validações de localização.
     * 
     * @param licensePlate placa do veículo
     * @param lat latitude da posição onde estacionou
     * @param lng longitude da posição onde estacionou
     * 
     * @apiNote Método transacional por consistência
     * @implNote Atualmente apenas registra log, sem persistência
     */
    @Transactional
    public void handleParked(String licensePlate, Double lat, Double lng) {
        log.debug("Confirmação PARKED para {} em [{}, {}]", licensePlate, lat, lng);
    }

    /**
     * Processa evento de saída de veículo do estacionamento.
     * 
     * Este método implementa a lógica completa de saída:
     * 
     * 1. **Localiza veículo**: Busca registro ativo por placa
     * 2. **Registra saída**: Atualiza timestamp e status
     * 3. **Calcula preço**: Aplica regras dinâmicas e tolerância
     * 4. **Libera vaga**: Marca como disponível para novos veículos
     * 5. **Finaliza**: Salva dados e registra valor cobrado
     * 
     * @param licensePlate placa do veículo
     * @param exitTime timestamp de saída em formato ISO 8601
     * @throws RuntimeException se veículo não for encontrado
     * 
     * @example
     * handleExit("ABC1234", "2025-01-20T12:30:00.000Z")
     * 
     * @apiNote Método transacional - garante consistência dos dados
     * @implNote Preço calculado com base na lotação no momento da saída
     */
    @Transactional
    public void handleExit(String licensePlate, String exitTime) {
        // Localiza veículo ativo no sistema
        var vehicle = vehicleRepository.findActiveByLicensePlate(licensePlate)
            .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        // Registra timestamp de saída e atualiza status
        LocalDateTime exit = LocalDateTime.parse(exitTime);
        vehicle.setExitTime(exit);
        vehicle.setStatus(Vehicle.VehicleStatus.EXITED);

        // Calcula preço baseado em lotação atual e tempo de permanência
        double occupancy = garageService.getOccupancyRate();
        BigDecimal sectorBasePrice = vehicle.getParkingSpot().getSector().getBasePrice();
        BigDecimal amount = pricingService.calculatePrice(vehicle.getEntryTime(), exit, occupancy, sectorBasePrice);
        vehicle.setTotalAmount(amount);

        // Libera vaga para novos veículos
        var spot = vehicle.getParkingSpot();
        spot.setOccupied(false);
        spotRepository.save(spot);
        vehicleRepository.save(vehicle);

        log.info("Veículo {} saiu - Valor cobrado: R${}", licensePlate, amount);
    }

    /**
     * Seleciona uma vaga disponível de forma aleatória.
     * 
     * Este método implementa a estratégia de alocação de vagas
     * distribuíndo veículos aleatoriamente entre as vagas livres
     * para evitar concentração em setores específicos.
     * 
     * Algoritmo:
     * 1. Consulta todas as vagas do banco
     * 2. Filtra apenas as não ocupadas
     * 3. Seleciona uma aleatoriamente
     * 
     * @return vaga disponível selecionada aleatoriamente
     * @throws RuntimeException se não houver vagas disponíveis
     * 
     * @implNote Utiliza Random.nextInt() para seleção
     * @see java.util.Random#nextInt(int)
     */
    private ParkingSpot selectRandomAvailableSpot() {
        // Obtém lista de vagas disponíveis
        List<ParkingSpot> available = spotRepository.findAll().stream()
            .filter(s -> !s.getOccupied())
            .toList();
        
        // Verifica se há vagas disponíveis
        if (available.isEmpty()) {
            throw new RuntimeException("Nenhuma vaga disponível");
        }
        
        // Seleciona vaga aleatoriamente
        return available.get(random.nextInt(available.size()));
    }

    /**
     * Calcula receita total de um setor em uma data específica.
     * 
     * Este método consulta todos os veículos que saíram do setor
     * especificado na data informada e soma os valores cobrados
     * para gerar relatório de faturamento.
     * 
     * Critérios de cálculo:
     * - Apenas veículos com status EXITED
     * - Que saíram na data especificada (00:00 a 23:59)
     * - Do setor informado
     * - Com valor cobrado (totalAmount não nulo)
     * 
     * @param sector nome do setor (A, B, C, D, etc.)
     * @param date data no formato YYYY-MM-DD
     * @return valor total da receita com precisão decimal
     * 
     * @example
     * getRevenue("A", "2025-01-20") // Receita do setor A em 20/01/2025
     * 
     * @implNote Utiliza query customizada no repositório para performance
     * @see com.estapar.parking.repository.VehicleRepository#calculateRevenueBySectorAndDate
     */
    public BigDecimal getRevenue(String sector, String date) {
        // Converte data para range de 24 horas
        LocalDateTime start = LocalDateTime.parse(date + "T00:00:00");
        LocalDateTime end = start.plusDays(1);
        
        // Executa query de cálculo de receita
        return vehicleRepository.calculateRevenueBySectorAndDate(sector, start, end);
    }
}