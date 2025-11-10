# Sistema de Gerenciamento de Estacionamento

Sistema backend para gerenciar estacionamento com controle de vagas, entrada/saída de veículos e cálculo de receita.

## Stack Técnica

- Java 21
- Spring Boot 3.3+
- Spring Data JPA
- MySQL 8
- Maven
- Docker

## Como Executar

### 1. Iniciar o Simulador
```bash
# Para Windows/macOS (mapeamento de porta)
docker run -d -p 8080:3000 cfontes0estapar/garage-sim:1.0.0

# Para Linux (network host)
docker run -d --network="host" cfontes0estapar/garage-sim:1.0.0
```

### 2. Iniciar MySQL
```bash
docker-compose up -d mysql
```

Aguarde 10 segundos para o MySQL inicializar.

### 3. Executar a Aplicação
```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:3003`

## Endpoints

### Webhook (recebe eventos do simulador)
```
POST http://localhost:3003/webhook
```

### Consulta de Receita
```
GET http://localhost:3003/revenue?sector=A&date=2025-01-20
```

## Regras de Negócio

### Sistema de Preços por Setor

**Setores disponíveis:**
- **Setor A:** R$ 40,50/hora (10 vagas)
- **Setor B:** R$ 4,10/hora (20 vagas)

### Tolerância e Cobrança
- **≤ 30 minutos:** Não cobra (tolerância)
- **31+ minutos:** Cobra horas completas baseado no tempo total

**Exemplos:**
- 31 min → Cobra 1 hora
- 65 min → Cobra 2 horas  
- 125 min → Cobra 3 horas

### Preço Dinâmico por Lotação

**Primeira hora (com variação por lotação):**
- **0-25%:** Desconto de 10% do preço base
- **26-50%:** Preço normal do setor
- **51-75%:** Acréscimo de 10% do preço base
- **76-100%:** Acréscimo de 25% do preço base

**Horas adicionais:** Preço base do setor (sem variação)

### Exemplos de Cálculo

**Setor B (R$ 4,10) - Lotação baixa (0-25%):**
- Primeira hora: R$ 4,10 × 0,90 = R$ 3,69
- Horas adicionais: R$ 4,10

**Permanência de 2 horas:**
- Total: R$ 3,69 + R$ 4,10 = R$ 7,79

## Segurança

### Rate Limiting
- 100 requisições por minuto por IP
- Retorna HTTP 429 quando excedido

### Endpoints Públicos
- `/webhook` - Recebe eventos do simulador
- `/revenue` - Consulta de receita

## Testes

### 🧪 Suíte de Testes Automatizados

O sistema possui uma **suíte completa de testes** cobrindo todos os cenários críticos:

#### **Testes Unitários**
- **PricingServiceTest**: 16 cenários de regras de preço e cálculos
- **ParkingServiceSimpleTest**: 3 cenários com mocks (entrada, saída, receita)
- **Cobertura**: Tolerância, arredondamento, preço dinâmico, fluxos principais

#### **Testes de Integração**
- **WebhookControllerTest**: Validação de endpoints e payloads
- **Cobertura**: Endpoints públicos, validação de payloads

#### **Testes de Performance**
- **PerformanceTest**: Concorrência e tempo de resposta
- **Cobertura**: Múltiplos webhooks simultâneos, rate limiting

### 🚀 Executar Testes

```bash
# Todos os testes (recomendado)
mvn test

# Testes específicos
mvn test -Dtest="PricingServiceTest"
mvn test -Dtest="WebhookControllerTest"
mvn test -Dtest="ParkingServiceSimpleTest"

# Testes de performance
mvn test -Dtest="PerformanceTest"
```

### 📊 Status dos Testes

**✅ Todos os testes passando**

- **PricingServiceTest**: 16/16 ✅
- **ParkingServiceSimpleTest**: 3/3 ✅  

- **WebhookControllerTest**: 4/4 ✅
- **PerformanceTest**: 2/2 ✅

**Total**: 23 testes funcionais, 0 falhas

### 📊 Cenários Testados

#### **Regras de Preço**
- ✅ Tolerância ≤ 30 minutos = gratuito
- ✅ Arredondamento: 31 min = 1h, 61 min = 2h
- ✅ Preço dinâmico por lotação (0-25%, 26-50%, 51-75%, 76-100%)
- ✅ Horas adicionais no preço base

#### **Fluxos de Eventos**
- ✅ ENTRY: Entrada com alocação de vaga
- ✅ PARKED: Confirmação de estacionamento
- ✅ EXIT: Saída com cálculo e cobrança
- ✅ Eventos desconhecidos ignorados

#### **Segurança**
- ✅ Endpoints públicos: `/webhook`, `/revenue`
- ✅ Validação de formatos (placas, datas)

#### **Casos Extremos**
- ✅ Estacionamento lotado rejeita entrada
- ✅ EXIT sem ENTRY retorna erro 404
- ✅ Veículo não encontrado
- ✅ Payloads inválidos

#### **Performance**
- ✅ Concorrência: 20+ webhooks simultâneos
- ✅ Rate limiting funcional
- ✅ Batch operations para múltiplas vagas

### Collection Postman
Importe o arquivo `Parking-Management.postman_collection.json` no Postman para testes manuais.

## Monitoramento

### Logs
- Nível DEBUG para `com.estapar.parking`
- Logs detalhados de cálculos de preço
- Logs de segurança e validação

### Banco de Dados
- **Host:** localhost:3306
- **Database:** parking_db
- **Usuário:** parking_user
- **Senha:** parking_pass