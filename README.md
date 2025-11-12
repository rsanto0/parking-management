# Sistema de Gerenciamento de Estacionamento

Sistema backend para gerenciar estacionamento com controle de vagas, entrada/saída de veículos e cálculo de receita.

## Stack Técnica

- Java 21
- Spring Boot 3.3+
- Spring Data JPA
- MySQL 8
- Maven
- Docker
- Lombok

## 🚀 Setup Rápido

### Pré-requisitos
- **Java 21** ou superior
- **Maven 3.6+**
- **Docker Desktop**
- **IDE** (Eclipse, IntelliJ, VS Code)

### Configuração do Ambiente

1. **Clone o repositório:**
```bash
git clone https://github.com/seu-usuario/parking-management.git
cd parking-management
```

2. **Compile o projeto:**
```bash
mvn clean install
```

3. **Configure Lombok na IDE:**

#### Eclipse:
1. **Baixe o Lombok:**
   - Acesse: https://projectlombok.org/download
   - Baixe o arquivo `lombok.jar`

2. **Execute o instalador:**
   ```bash
   java -jar lombok.jar
   ```
   - Selecione sua instalação do Eclipse
   - Clique "Install/Update"
   - Reinicie o Eclipse

#### IntelliJ IDEA:
- Instale o plugin "Lombok" via Settings → Plugins
- Habilite "Annotation Processing" em Settings → Build → Compiler → Annotation Processors

#### VS Code:
- Instale a extensão "Lombok Annotations Support for VS Code"

### Verificação do Setup
```bash
# Teste se compila sem erros
mvn compile

# Execute os testes
mvn test
```

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
**Resposta:**
- `202 Accepted` - Evento enfileirado para processamento assíncrono
- `503 Service Unavailable` - Fila cheia (evento enviado para DLQ)

### Consulta de Receita
```
GET http://localhost:3003/revenue?sector=A&date=2025-01-20
```

### Dead Letter Queue (DLQ)
```
GET http://localhost:3003/dlq        # Lista eventos rejeitados
GET http://localhost:3003/dlq/size   # Quantidade de eventos na DLQ
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

## Arquitetura Assíncrona

### Processamento de Eventos

O sistema utiliza **arquitetura assíncrona** para processar eventos do simulador:

**Benefícios:**
- ⚡ **Alta Performance**: Webhook responde em <100ms
- 🔄 **Desacoplamento**: Controller não bloqueia aguardando processamento
- 📊 **Backpressure**: Fila absorve picos de carga (1000 eventos)
- 🛡️ **Resiliência**: DLQ captura eventos quando fila está cheia

**Fluxo:**
1. Webhook recebe evento → Enfileira → Retorna HTTP 202
2. Thread consumidora processa eventos em ordem (FIFO)
3. Se fila cheia → Evento vai para DLQ → Retorna HTTP 503

**Configuração:**
- **Capacidade da fila:** 1000 eventos
- **DLQ:** Ilimitada (eventos rejeitados)
- **Threads assíncronas:** 2-4 (configurável)

### Dead Letter Queue (DLQ)

Eventos rejeitados quando a fila principal está cheia são automaticamente enviados para a DLQ:

```bash
# Consultar eventos rejeitados
curl http://localhost:3003/dlq

# Verificar quantidade na DLQ
curl http://localhost:3003/dlq/size
```

### Endpoints Públicos
- `/webhook` - Recebe eventos do simulador (processamento assíncrono)
- `/revenue` - Consulta de receita
- `/dlq` - Gerenciamento de Dead Letter Queue

## Testes

### 🧪 Suíte de Testes Automatizados

O sistema possui uma **suíte completa de testes** cobrindo todos os cenários críticos:

#### **Testes Unitários**
- **PricingServiceTest**: 16 cenários de regras de preço e cálculos
- **ParkingServiceSimpleTest**: 3 cenários com mocks (entrada, saída, receita)
- **Cobertura**: Tolerância, arredondamento, preço dinâmico, fluxos principais

#### **Testes de Integração**
- **WebhookControllerTest**: Validação de endpoints e payloads
- **Cobertura**: Enfileiramento assíncrono, HTTP 202/503, validação de payloads

#### **Testes Assíncronos**
- **EventQueueServiceTest**: Fila, DLQ e processamento assíncrono
- **Cobertura**: Enfileiramento rápido (<100ms), FIFO, backpressure, DLQ, resiliência

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
- **WebhookControllerTest**: 3/3 ✅
- **EventQueueServiceTest**: 6/6 ✅

**Total**: 28 testes funcionais, 0 falhas

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
- ✅ Processamento assíncrono com fila
- ✅ HTTP 202 Accepted para eventos enfileirados
- ✅ HTTP 503 quando fila cheia

#### **Segurança**
- ✅ Endpoints públicos: `/webhook`, `/revenue`
- ✅ Validação de formatos (placas, datas)

#### **Casos Extremos**
- ✅ Estacionamento lotado rejeita entrada
- ✅ EXIT sem ENTRY retorna erro 404
- ✅ Veículo não encontrado
- ✅ Payloads inválidos

#### **Performance e Assincronismo**
- ✅ Enfileiramento rápido: <100ms por evento
- ✅ Processamento FIFO: eventos em ordem
- ✅ Backpressure: fila cheia envia para DLQ
- ✅ Resiliência: falhas não param consumidor
- ✅ Concorrência: múltiplos webhooks simultâneos
- ✅ DLQ: captura eventos rejeitados

### Collection Postman
Importe o arquivo `Parking-Management.postman_collection.json` no Postman para testes manuais.

## 🛠️ Troubleshooting

### Erros de Compilação com Lombok
**Problema:** IDE mostra erros "cannot find symbol" para getters/setters

**Solução:**
1. Verifique se Lombok está instalado na IDE
2. Refresh do projeto (F5 no Eclipse)
3. Clean + Rebuild: `mvn clean compile`
4. Reinicie a IDE

### Docker não inicia
**Problema:** "Docker daemon not running"

**Solução:**
1. Inicie o Docker Desktop
2. Aguarde a inicialização completa
3. Verifique: `docker --version`

### MySQL Connection Error
**Problema:** "Connection refused" ao conectar no MySQL

**Solução:**
1. Aguarde 10-15 segundos após `docker-compose up -d mysql`
2. Verifique se está rodando: `docker ps`
3. Reinicie se necessário: `docker-compose restart mysql`

## 📋 Checklist para Avaliadores

- [ ] Java 21 instalado
- [ ] Docker Desktop rodando
- [ ] Lombok configurado na IDE
- [ ] Projeto compila: `mvn compile`
- [ ] Testes passam: `mvn test`
- [ ] MySQL iniciado: `docker-compose up -d mysql`
- [ ] Aplicação roda: `mvn spring-boot:run`
- [ ] Endpoints respondem: `http://localhost:3003/revenue?sector=A&date=2025-01-20`

## 📊 Monitoramento

### Logs
- Nível DEBUG para `com.estapar.parking`
- Logs detalhados de cálculos de preço
- Logs de segurança e validação

### Banco de Dados
- **Host:** localhost:3306
- **Database:** parking_db
- **Usuário:** parking_user
- **Senha:** parking_pass

## 🤝 Contribuição

Este projeto foi desenvolvido como sistema de gerenciamento de estacionamento com foco em:
- Arquitetura limpa e testável
- Cobertura completa de testes
- Documentação detalhada
- Configuração simplificada para avaliação

### Estrutura do Projeto
```
src/
├── main/java/com/estapar/parking/
│   ├── controller/     # REST Controllers (Webhook, Revenue, DLQ)
│   ├── service/        # Lógica de negócio
│   │   ├── EventQueueService.java    # Fila assíncrona + DLQ
│   │   ├── ParkingService.java       # Regras de negócio
│   │   └── PricingService.java       # Cálculo de preços
│   ├── entity/         # Entidades JPA
│   ├── dto/            # Data Transfer Objects
│   └── config/         # Configurações (Async, Security)
└── test/               # Testes automatizados
```

### Documentação Adicional

Para detalhes técnicos da arquitetura assíncrona, consulte:
- `ASYNC_ARCHITECTURE.md` - Documentação completa da fila e DLQ