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

O sistema implementa **rate limiting** para proteger contra ataques DoS e abuso da API:

**Configuração:**
- **Limite:** 5 requisições por IP
- **Janela:** 10 segundos
- **Resposta:** HTTP 429 (Too Many Requests)
- **Reset:** Automático após janela de tempo

**Funcionamento:**
1. Cada IP pode fazer até 5 requisições em 10 segundos
2. A 6ª requisição retorna erro 429
3. Após 10 segundos, o contador reseta
4. Considera proxies via header `X-Forwarded-For`

**Exemplo de uso:**
```bash
# Primeiras 5 requisições: HTTP 200 OK
curl -X POST http://localhost:3003/webhook -d '{...}'

# 6ª requisição: HTTP 429 Too Many Requests
curl -X POST http://localhost:3003/webhook -d '{...}'
# Response: {"error":"Rate limit exceeded"}

# Após 10 segundos: HTTP 200 OK novamente
```

### Endpoints Públicos
- `/webhook` - Recebe eventos do simulador (com rate limiting)
- `/revenue` - Consulta de receita (com rate limiting)

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
- **PerformanceTest**: Rate limiting e concorrência
- **Cobertura**: Limite de 5 req/10s, reset de janela, HTTP 429

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

**Total**: 25 testes funcionais, 0 falhas

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

#### **Performance e Rate Limiting**
- ✅ Rate limiting: 5 requisições por janela de 10s
- ✅ Bloqueio correto após limite excedido (HTTP 429)
- ✅ Reset automático da janela de tempo
- ✅ Concorrência: múltiplos webhooks simultâneos

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
│   ├── controller/     # REST Controllers
│   ├── service/        # Lógica de negócio
│   ├── entity/         # Entidades JPA
│   ├── dto/            # Data Transfer Objects
│   └── config/         # Configurações
└── test/               # Testes automatizados
```