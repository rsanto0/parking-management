package com.estapar.parking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador global de exceções para o sistema de estacionamento.
 * 
 * Esta classe intercepta e trata todas as exceções não capturadas
 * nos controllers, fornecendo respostas padronizadas e logs apropriados.
 * 
 * Utiliza @RestControllerAdvice para aplicar o tratamento globalmente
 * a todos os controllers REST da aplicação.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Logger para registrar eventos de exceções e erros.
     * Utilizado para auditoria e debugging do sistema.
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Manipula erros de validação de entrada de dados.
     * 
     * Captura exceções do tipo MethodArgumentNotValidException que ocorrem
     * quando os dados enviados nas requisições não passam nas validações
     * definidas com anotações como @NotBlank, @Pattern, etc.
     * 
     * @param ex exceção de validação contendo detalhes dos campos inválidos
     * @return ResponseEntity com HTTP 400 e mapa de erros por campo
     * 
     * @example
     * Entrada inválida:
     * {
     *   "license_plate": "",
     *   "event_type": "INVALID"
     * }
     * 
     * Resposta:
     * {
     *   "license_plate": "License plate is required",
     *   "event_type": "Invalid event type"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Erro de validação detectado: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Manipula exceções de runtime não específicas.
     * 
     * Captura RuntimeException e suas subclasses que não foram
     * tratadas especificamente por outros handlers, fornecendo
     * uma resposta padronizada de erro interno.
     * 
     * @param ex exceção de runtime contendo a mensagem de erro
     * @return ResponseEntity com HTTP 500 e mensagem de erro
     * 
     * @example
     * Exceção: "Veículo não encontrado"
     * 
     * Resposta:
     * {
     *   "error": "Veículo não encontrado"
     * }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        log.error("Erro de runtime capturado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}