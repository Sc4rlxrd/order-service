# 📦 Order Service

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?style=for-the-badge&logo=rabbitmq)

</div>

---

## 📋 Descrição

Serviço responsável pelo gerenciamento completo de pedidos dentro da arquitetura de microserviços do **BookCommerce**. O **Order Service** atua como **orquestrador do fluxo de compra**, integrando-se com o `catalog-service` e o `payment-service` via eventos assíncronos utilizando **RabbitMQ**.

---

## 🧠 Responsabilidades Principais

- ✨ **Criar pedidos** - Validação e persistência de novos pedidos
- 💾 **Persistir itens do pedido** - Armazenamento de itens e quantidades
- 🔍 **Iniciar fluxo de validação de livros** - Comunicação com catalog-service
- 💰 **Calcular valor total do pedido** - Cálculo automático de totais
- 💳 **Enviar solicitação de pagamento** - Integração com payment-service
- 🔄 **Atualizar status do pedido** - Transição de estados baseada em eventos
- 🛡️ **Rate Limiting** - Proteção contra abuso de requisições
- ⚡ **Virtual Threads** - Processamento de alta performance com Java 21

---

## 🏗️ Arquitetura & Stack Tecnológico

### Backend
- **Java 21** - Linguagem principal com suporte a Virtual Threads
- **Spring Boot 4.0.3** - Framework web e DI
- **Spring Data JPA** - Persistência de dados
- **Spring AMQP** - Comunicação assíncrona via RabbitMQ
- **Validation** - Validações em DTOs com Jakarta Validation
- **MapStruct 1.6.3** - Mapeamento de objetos
- **Lombok** - Redução de boilerplate

### Banco de Dados
- **PostgreSQL 15** - Banco de dados relacional
- **Flyway** - Versionamento e migração de schema

### Mensageria
- **RabbitMQ** - Message broker para comunicação assíncrona

### Resiliência & Segurança
- **Resilience4j 2.4.0** - Circuit breaker, retry, timeout
- **Bucket4j 8.10.1** - Rate limiting por requisição
- **Interceptors** - Rate limit em camada HTTP

### Testes
- Spring Boot Test com Flyway Test
- Suporte a data-jpa-test e validation-test

---

## 📁 Estrutura de Diretórios

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/com/scarlxrd/order_service/
│   │   │   ├── config/
│   │   │   │   ├── rabbitmq/          # Configurações do RabbitMQ
│   │   │   │   ├── client/            # REST clients (RestTemplate)
│   │   │   │   ├── RateLimitInterceptor.java
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── OrderController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── service/
│   │   │   │   └── OrderService.java
│   │   │   ├── repository/
│   │   │   │   └── OrderRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   └── OrderStatus.java (enum)
│   │   │   ├── dto/
│   │   │   │   ├── CreateOrderDTO.java
│   │   │   │   ├── OrderResponseDTO.java
│   │   │   │   ├── OrderItemDTO.java
│   │   │   │   ├── BookValidationRequest.java
│   │   │   │   ├── BookValidatedEvent.java
│   │   │   │   ├── PaymentRequestDTO.java
│   │   │   │   ├── PaymentResponseDTO.java
│   │   │   │   └── OrderCreatedEvent.java
│   │   │   ├── mapper/
│   │   │   │   └── OrderMapper.java (MapStruct)
│   │   │   ├── exception/
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── RateLimitException.java
│   │   │   └── Application.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/
│   │           ├── V1__create_orders.sql
│   │           └── V2__create_order_items.sql
│   └── test/
│       └── java/com/scarlxrd/order_service/
│           └── ApplicationTests.java
├── docker/
│   └── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 🚀 Início Rápido

### Pré-requisitos
- Java 21+
- Maven 3.8.1+
- Docker & Docker Compose
- PostgreSQL 15 (ou via Docker)
- RabbitMQ (ou via Docker)

### Instalação & Execução

#### 1️⃣ Clonar o repositório
```bash
git clone https://github.com/seu-usuario/BookCommerce.git
cd order-service
```

#### 2️⃣ Iniciar dependências com Docker
```bash
docker-compose -f docker/docker-compose.yml up -d
```

Isso iniciará:
- **PostgreSQL** na porta 5432
- **RabbitMQ** (se configurado no docker-compose)

> ⚠️ **Nota**: O docker-compose atual contém apenas PostgreSQL. Configure RabbitMQ se necessário.

#### 3️⃣ Compilar o projeto
```bash
./mvnw clean install
```

#### 4️⃣ Executar a aplicação
```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8082`

---

## 🔌 Endpoints da API

### Criar Pedido
```http
POST /orders
Content-Type: application/json
X-User-Email: user@example.com

{
  "items": [
    {
      "bookId": "123e4567-e89b-12d3-a456-426614174000",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

**Resposta (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "user-uuid",
  "items": [
    {
      "bookId": "123e4567-e89b-12d3-a456-426614174000",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "totalAmount": 59.98,
  "status": "PENDING",
  "createdAt": "2024-04-12T10:30:00Z"
}
```

---

## 📊 Schema do Banco de Dados

### Tabela: `orders`
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

### Tabela: `order_items`
```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    book_id UUID NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## 🔄 Fluxo de Processamento de Pedido

```
┌─────────────────────────────────────────────────────────────┐
│                   Cliente envia POST /orders                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │  Validação do DTO          │
        │  (Bean Validation)         │
        └────────────┬───────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │  Criar Order (PENDING)     │
        │  Persistir OrderItems      │
        └────────────┬───────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │  Publicar OrderCreatedEvent│
        │  para RabbitMQ             │
        └────────────┬───────────────┘
                     │
           ┌─────────┴─────────┐
           │                   │
           ▼                   ▼
    ┌──────────────┐    ┌──────────────┐
    │ Catalog-    │    │ Payment-     │
    │ Service     │    │ Service      │
    │ (validar    │    │ (processar   │
    │  livros)    │    │  pagamento)  │
    └──────────────┘    └──────────────┘
           │                   │
           └─────────┬─────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │  Atualizar Status da Order │
        │  (CONFIRMED ou CANCELED)   │
        └────────────────────────────┘
```

---

## 🔐 Rate Limiting

O serviço implementa **Rate Limiting** através do **Bucket4j** e um interceptor customizado.

### Configuração
- **Interceptor**: `RateLimitInterceptor.java`
- **Requisições por minuto**: Configurável
- **Exceção**: `RateLimitException` retorna HTTP 429 (Too Many Requests)

---

## ⚙️ Configuração da Aplicação

### `application.yaml`

```yaml
server:
  port: 8082
  error:
    include-message: always

spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/orders
    username: postgres
    password: root
  threads:
    virtual:
      enabled: true        # Virtual Threads habilitadas
  rabbitmq:
    host: localhost
    port: 5672
    username: book_user
    password: book_password

flyway:
  enabled: true
  locations: classpath:db/migration

jpa:
  hibernate:
    ddl-auto: validate    # Valida schema, não cria
```

### Variáveis de Ambiente
```bash
DB_URL=jdbc:postgresql://db-host:5432/orders
DB_USERNAME=postgres
DB_PASSWORD=root

RABBITMQ_HOST=rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USER=book_user
RABBITMQ_PASSWORD=book_password

PAYMENT_SERVICE_URL=http://payment-service:8080
CATALOG_SERVICE_URL=http://catalog-service:8081
```

---

## 🧪 Testes

### Executar todos os testes
```bash
./mvnw test
```

### Executar com cobertura
```bash
./mvnw test jacoco:report
```

### Teste específico
```bash
./mvnw test -Dtest=OrderServiceTest
```

---

## 🐳 Containerização

### Build da imagem Docker
```bash
docker build -t bookcommerce/order-service:latest .
```

### Executar container
```bash
docker run -d \
  --name order-service \
  -p 8082:8082 \
  -e DB_URL=jdbc:postgresql://db-host:5432/orders \
  -e RABBITMQ_HOST=rabbitmq-host \
  bookcommerce/order-service:latest
```

---

## 🔗 Integração com Outros Serviços

### Catalog Service
- **Objetivo**: Validar disponibilidade e preço de livros
- **Tipo**: Event-driven (RabbitMQ)
- **Queue**: `book.validation.requests`

### Payment Service
- **Objetivo**: Processar pagamento do pedido
- **Tipo**: Event-driven (RabbitMQ)
- **Queue**: `payment.requests`

---

## 🛠️ Ferramentas de Desenvolvimento

### Build & Compilação
```bash
# Compilar
./mvnw clean compile

# Build JAR
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

### Logging
- Nível: `DEBUG` para Spring Framework
- Logs estruturados recomendados para produção

---

## 📝 Padrões de Projeto Utilizados

| Padrão | Implementação |
|--------|---------------|
| **MVC** | Controller → Service → Repository |
| **DTO** | Separação entre entrada/saída de dados |
| **Mapper** | MapStruct para transformação de objetos |
| **Repository** | Spring Data JPA |
| **Event-Driven** | RabbitMQ com listeners |
| **Exception Handling** | GlobalExceptionHandler |
| **Rate Limiting** | Bucket4j + Interceptor |

---

## 🚨 Tratamento de Erros

### Exceções Customizadas

**BusinessException**: Erro de lógica de negócio
```java
throw new BusinessException("Pedido não encontrado");
```

**RateLimitException**: Limite de requisições atingido
```java
throw new RateLimitException("Rate limit excedido");
```

### Respostas de Erro
```json
{
  "message": "Descrição do erro",
  "timestamp": "2024-04-12T10:30:00Z",
  "status": 400
}
```

---

## 📚 Dependências Principais

| Dependência | Versão | Propósito |
|-------------|--------|----------|
| Spring Boot | 4.0.3 | Framework principal |
| Spring Data JPA | Latest | Persistência |
| Spring AMQP | Latest | Messaging |
| PostgreSQL Driver | Latest | BD relacional |
| Flyway | Latest | Migrações |
| MapStruct | 1.6.3 | Mapeamento de objetos |
| Lombok | Latest | Boilerplate reduction |
| Resilience4j | 2.4.0 | Circuit breaker |
| Bucket4j | 8.10.1 | Rate limiting |

---


## 🙌 Agradecimentos

Desenvolvido como parte do projeto **BookCommerce** - Microserviços para gerenciamento de livraria online.

---

<div align="center">

**⭐ Se gostou do projeto, deixe uma estrela! ⭐**

</div>

