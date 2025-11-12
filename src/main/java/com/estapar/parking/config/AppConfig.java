package com.estapar.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração geral da aplicação de gerenciamento de estacionamento.
 * 
 * Esta classe define beans de configuração necessários para o funcionamento
 * da aplicação, incluindo clientes HTTP e outras dependências compartilhadas.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableAsync
public class AppConfig {
    
    /**
     * Cria e configura um bean RestTemplate para requisições HTTP.
     * 
     * O RestTemplate é utilizado para fazer chamadas HTTP para APIs externas,
     * especificamente para comunicar com o simulador de garagem e obter
     * configurações de setores e vagas.
     * 
     * @return instância configurada de RestTemplate para uso em toda a aplicação
     * 
     * @example
     * Uso no GarageService:
     * GarageConfig config = restTemplate.getForObject(simulatorUrl + "/garage", GarageConfig.class);
     * 
     * @see org.springframework.web.client.RestTemplate
     * @see com.estapar.parking.service.GarageService#loadGarageData()
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}