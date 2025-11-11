package com.estapar.parking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade JPA representando uma vaga individual do estacionamento.
 * 
 * Esta classe mapeia a tabela "parking_spots" no banco de dados e contém
 * informações detalhadas sobre cada vaga, incluindo localização geográfica,
 * status de ocupação e relacionamento com o setor.
 * 
 * Cada vaga possui:
 *  Identificação única obtida do simulador
 *  Coordenadas geográficas precisas (latitude/longitude)
 *  Status de ocupação (livre/ocupada)
 *  Relacionamento com setor para cálculos de preço
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 * @see com.estapar.parking.entity.Sector
 * @see com.estapar.parking.entity.Vehicle
 */
@Entity
@Table(name = "parking_spots")
@Data
@NoArgsConstructor
// @DynamicInsert indica ao Hibernate para incluir apenas campos não-nulos no SQL INSERT
// Isso otimiza a query evitando valores NULL desnecessários e permitindo que valores default
// do banco de dados sejam aplicados, o mesmo conceito se aplica ao DynamicUpdate
@org.hibernate.annotations.DynamicInsert
@org.hibernate.annotations.DynamicUpdate
public class ParkingSpot {
    
	
	
	
    public ParkingSpot(Long id, BigDecimal latitude, BigDecimal longitude, Boolean occupied, Sector sector) {
		super();
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.occupied = occupied;
		this.sector = sector;
	}

	/**
     * Identificador único da vaga.
     * Utiliza o mesmo ID fornecido pelo simulador para manter consistência
     * entre o sistema externo e o banco de dados interno.
     */
    @Id
    private Long id;

    /**
     * Coordenada de latitude da vaga em graus decimais.
     * Utilizada para localização geográfica precisa da vaga no estacionamento.
     * Armazenada com precisão decimal para evitar erros de arredondamento.
     * 
     * @example -23.561684
     */
    @Column(nullable = false)
    private BigDecimal latitude;

    /**
     * Coordenada de longitude da vaga em graus decimais.
     * Utilizada para localização geográfica precisa da vaga no estacionamento.
     * Armazenada com precisão decimal para evitar erros de arredondamento.
     * 
     * @example -46.655981
     */
    @Column(nullable = false)
    private BigDecimal longitude;

    /**
     * Status de ocupação da vaga.
     * - true: vaga ocupada por um veículo
     * - false: vaga livre e disponível
     * 
     * Inicializada como false (livre) por padrão.
     */
    @Column(nullable = false)
    private Boolean occupied = false;

    /**
     * Relacionamento many-to-one com o setor.
     * Cada vaga pertence a exatamente um setor, que define
     * as regras de preço e políticas de estacionamento.
     * 
     * Mapeado pela coluna "sector_id" na tabela.
     */
    @ManyToOne
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    /**
     * Construtor para criar uma nova vaga com dados completos.
     * 
     * @param id identificador único da vaga (do simulador)
     * @param latitude coordenada de latitude em graus decimais
     * @param longitude coordenada de longitude em graus decimais
     * @param sector setor ao qual a vaga pertence
     */
    public ParkingSpot(Long id, BigDecimal latitude, BigDecimal longitude, Sector sector) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sector = sector;
        this.occupied = false; // Nova vaga sempre começa livre
    }

	public ParkingSpot(String string, BigDecimal latitude, BigDecimal longitude, Sector sector) {
		
		this.latitude = latitude;
        this.longitude = longitude;
        this.sector = sector;
	}


}