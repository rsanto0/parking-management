package com.estapar.parking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade JPA representando um setor do estacionamento.
 * 
 * Esta classe mapeia a tabela "sectors" no banco de dados e contém
 * informações sobre cada setor lógico do estacionamento, incluindo
 * configurações de preço, capacidade e relacionamentos com vagas.
 * 
 * Os setores são divisões lógicas (não físicas) do estacionamento
 * utilizadas para:
 *  Organização do pool de vagas
 *  Definição de preços diferenciados
 *  Cálculos de lotação e disponibilidade
 *  Relatórios de receita segmentados
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.entity.ParkingSpot
 * @see com.estapar.parking.entity.Vehicle
 */
@Entity
@Table(name = "sectors")
@Data
@NoArgsConstructor
@org.hibernate.annotations.DynamicInsert
@org.hibernate.annotations.DynamicUpdate
public class Sector {
    
    /**
     * Identificador único do setor gerado automaticamente.
     * Chave primária auto-incrementada pelo banco de dados.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome identificador do setor (único no sistema).
     * Utilizado para identificação lógica do setor (A, B, C, D, etc.).
     * Deve ser único para evitar conflitos de nomenclatura.
     * 
     * @example "A", "B", "C", "D"
     */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Preço base por hora do setor em reais.
     * Valor utilizado como base para cálculos de preço dinâmico,
     * considerando variações por lotação e tempo de permanência.
     * 
     * Armazenado com precisão decimal para cálculos monetários precisos.
     * 
     * @example 40.50 (Setor A), 4.10 (Setor B)
     */
    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    /**
     * Capacidade máxima de vagas do setor.
     * Número total de vagas que podem ser alocadas neste setor,
     * utilizado para cálculos de lotação e disponibilidade.
     * 
     * @example 10 (Setor A), 20 (Setor B)
     */
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    /**
     * Relacionamento one-to-many com as vagas do setor.
     * Lista de todas as vagas que pertencem a este setor.
     * 
     * Configurações:
     * - mappedBy: indica que o relacionamento é mapeado pelo campo "sector" em ParkingSpot
     * - cascade: operações em cascata para todas as vagas do setor
     */
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL)
    private List<ParkingSpot> spots;

    /**
     * Construtor para criar um novo setor com configurações completas.
     * 
     * @param name nome identificador do setor
     * @param basePrice preço base por hora em reais
     * @param maxCapacity capacidade máxima de vagas
     */
    public Sector(String name, BigDecimal basePrice, Integer maxCapacity) {
        this.name = name;
        this.basePrice = basePrice;
        this.maxCapacity = maxCapacity;
    }


}