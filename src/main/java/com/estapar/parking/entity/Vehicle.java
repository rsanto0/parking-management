package com.estapar.parking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade JPA representando um veículo e seu histórico no estacionamento.
 * 
 * Esta classe mapeia a tabela "vehicles" no banco de dados e registra
 * todas as informações sobre a permanência de um veículo, incluindo
 * tempos de entrada/saída, vaga ocupada, valor cobrado e status atual.
 * 
 * O ciclo de vida de um veículo no sistema:
 * 1. ENTRY - Veículo chegou na cancela
 * 2. PARKED - Veículo estacionou em uma vaga
 * 3. EXITED - Veículo saiu e foi cobrado
 * 
 * Utilizada para:
 * - Controle de ocupação de vagas
 * - Cálculos de tempo de permanência
 * - Aplicação de regras de preço dinâmico
 * - Geração de relatórios de receita
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.entity.ParkingSpot
 * @see com.estapar.parking.entity.Sector
 */
@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@org.hibernate.annotations.DynamicInsert
@org.hibernate.annotations.DynamicUpdate
public class Vehicle {
    
    /**
     * Identificador único do registro do veículo.
     * Chave primária auto-incrementada pelo banco de dados.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Placa do veículo no formato brasileiro.
     * Suporta tanto formato antigo (ABC1234) quanto Mercosul (ABC1C34).
     * Utilizada como identificador principal do veículo no sistema.
     * 
     * @example "ABC1234", "XYZ1A23"
     */
    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    /**
     * Timestamp de entrada do veículo no estacionamento.
     * Registra o momento exato em que o veículo chegou na cancela,
     * utilizado para cálculos de tempo de permanência e cobrança.
     */
    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    /**
     * Timestamp de saída do veículo do estacionamento.
     * Pode ser null enquanto o veículo ainda está no estacionamento.
     * Utilizado junto com entryTime para calcular tempo total e valor.
     */
    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    /**
     * Relacionamento many-to-one com a vaga ocupada.
     * Indica qual vaga específica o veículo está ocupando.
     * Pode ser null se o veículo ainda não foi direcionado para uma vaga.
     */
    @ManyToOne
    @JoinColumn(name = "parking_spot_id")
    private ParkingSpot parkingSpot;

    /**
     * Valor total cobrado do veículo em reais.
     * Calculado com base no tempo de permanência, preço do setor,
     * regras de lotação e tolerância de 30 minutos.
     * 
     * Null enquanto o veículo não saiu do estacionamento.
     */
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    /**
     * Status atual do veículo no sistema.
     * Controla o fluxo de estados durante a permanência no estacionamento.
     * Armazenado como string no banco para facilitar leitura e debugging.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    /**
     * Construtor para criar um novo registro de veículo na entrada.
     * 
     * @param licensePlate placa do veículo no formato brasileiro
     * @param entryTime timestamp de entrada no estacionamento
     */
    public Vehicle(String licensePlate, LocalDateTime entryTime) {
        this.licensePlate = licensePlate;
        this.entryTime = entryTime;
        this.status = VehicleStatus.ENTERED; // Status inicial sempre ENTERED
    }

    /**
     * Enumeração dos possíveis status de um veículo no sistema.
     * 
     * Estados do ciclo de vida:
     * - ENTERED: Veículo chegou na cancela e foi autorizado a entrar
     * - PARKED: Veículo foi direcionado e estacionou em uma vaga específica
     * - EXITED: Veículo saiu do estacionamento e foi cobrado
     */
    public enum VehicleStatus {
        /** Veículo autorizado a entrar, aguardando direção para vaga */
        ENTERED,
        /** Veículo estacionado em vaga específica */
        PARKED,
        /** Veículo saiu e foi cobrado */
        EXITED
    }


}