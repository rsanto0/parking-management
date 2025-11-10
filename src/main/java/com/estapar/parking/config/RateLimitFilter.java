package com.estapar.parking.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de limitação de taxa de requisições (Rate Limiting).
 * 
 * Implementa proteção contra ataques de negação de serviço (DoS) e abuso
 * da API limitando o número de requisições por IP em uma janela de tempo.
 * 
 * Utiliza estruturas thread-safe para controlar contadores por IP de forma
 * concorrente e segura em ambiente multi-thread.
 * 
 * @author Sistema de Estacionamento
 * @version 1.0
 * @since 1.0
 */
@Component
public class RateLimitFilter implements Filter {
    
    /**
     * Mapa thread-safe que armazena o contador de requisições por IP.
     * Utiliza AtomicInteger para operações atômicas de incremento.
     */
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    
    /**
     * Mapa thread-safe que armazena o timestamp da primeira requisição
     * na janela de tempo atual para cada IP.
     */
    private final ConcurrentHashMap<String, Long> requestTimes = new ConcurrentHashMap<>();
    
    /**
     * Número máximo de requisições permitidas por IP na janela de tempo.
     * Valor: 5 requisições por minuto.
     */
    private static final int MAX_REQUESTS = 5;
    
    /**
     * Janela de tempo em milissegundos para contagem de requisições.
     * Valor: 60.000ms = 1 minuto.
     */
    private static final long TIME_WINDOW = 10000; // 10 segundos

    /**
     * Método principal do filtro que intercepta todas as requisições HTTP.
     * 
     * Verifica se o IP do cliente excedeu o limite de requisições na janela
     * de tempo. Se excedeu, retorna HTTP 429 (Too Many Requests), caso
     * contrário permite que a requisição continue.
     * 
     * @param request requisição HTTP recebida
     * @param response resposta HTTP a ser enviada
     * @param chain cadeia de filtros para continuar o processamento
     * @throws IOException em caso de erro de I/O
     * @throws ServletException em caso de erro do servlet
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIp(httpRequest);
        long currentTime = System.currentTimeMillis();
        
        if (isRateLimited(clientIp, currentTime)) {
            httpResponse.setStatus(429);
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * Verifica se um IP específico excedeu o limite de requisições.
     * 
     * Implementa lógica de janela deslizante: se a última requisição foi
     * há mais de TIME_WINDOW, reinicia o contador. Caso contrário,
     * incrementa e verifica se excedeu MAX_REQUESTS.
     * 
     * @param clientIp endereço IP do cliente
     * @param currentTime timestamp atual em milissegundos
     * @return true se o limite foi excedido, false caso contrário
     */
    private boolean isRateLimited(String clientIp, long currentTime) {
        Long lastRequestTime = requestTimes.get(clientIp);
        
        if (lastRequestTime == null || currentTime - lastRequestTime > TIME_WINDOW) {
            requestCounts.put(clientIp, new AtomicInteger(1));
            requestTimes.put(clientIp, currentTime);
            return false;
        }
        
        AtomicInteger count = requestCounts.get(clientIp);
        return count != null && count.incrementAndGet() > MAX_REQUESTS;
    }
    
    /**
     * Extrai o endereço IP real do cliente da requisição HTTP.
     * 
     * Considera proxies e load balancers verificando o header
     * X-Forwarded-For antes de usar o IP direto da conexão.
     * 
     * @param request requisição HTTP contendo headers e informações de rede
     * @return endereço IP do cliente (pode ser IPv4 ou IPv6)
     * 
     * @example
     * Com proxy: X-Forwarded-For: "203.0.113.1, 198.51.100.1"
     * Retorna: "203.0.113.1" (primeiro IP da lista)
     * 
     * Sem proxy: request.getRemoteAddr()
     * Retorna: "192.168.1.100"
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}