# Asset Management Spring Microservice

A Spring Boot microservices application for managing digital assets, users, wallets, and transactions. Demonstrates modern microservices architecture patterns with service discovery, API gateway routing, event streaming via Kafka/Debezium CDC, and a full observability stack.

## 🏗️ Architecture Overview

This is a **Spring Cloud microservices** monorepo with the following key components:

### Microservices Modules

| Module | Purpose | Default Port |
|--------|---------|---------|
| **gateway** | API Gateway with Spring Cloud Gateway & JWT validation | 9099 |
| **eureka** | Service Registry & Discovery | 8761 |
| **user** | User management, authentication, and registration | 8082 |
| **wallet** | Wallet/asset management with Outbox CDC | 8081 |
| **transaction** | Transaction and ledger processing | 8088 |
| **shared-contracts** | Shared Avro schemas and event DTOs | — |
| **shared-module** | Shared utilities: OutBox entity, KafkaTopics, Avro serializer | — |
| **debezium** | Debezium Connect for Outbox CDC | 8083 |

### Shared Infrastructure Modules

- **shared-contracts**: Common Avro schemas (`UserRegisteredEvent`, `TransferDtoEvent`, `TransferResultDtoEvent`, `WithdrawResultDtoEvent`) and enum types
- **shared-module**: `OutBox` JPA entity, `OutBoxRepository`, `KafkaTopics` constants, `AvroPayloadSerializer`, and `SharedInfraAutoConfiguration`

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot 4.1.0**
- **Spring Cloud 2025.1.2**
- **Java 17**

### API & Communication
- **Spring Cloud Gateway** (WebFlux-based) — API gateway with JWT auth filter
- **Spring Cloud Netflix Eureka** — Service discovery
- **Spring Web MVC** — REST APIs (user, wallet, transaction)
- **Spring WebFlux** — Reactive gateway

### Data Management
- **Spring Data JPA / PostgreSQL** — Primary relational database (one DB per service)
- **Spring Data Redis / Redis 7** — Caching (wallet service)

### Event Streaming & CDC
- **Apache Kafka 4.0.0** (KRaft mode, no ZooKeeper)
- **Confluent Schema Registry 8.0.0** (port `8010` externally → `8081` internally)
- **Debezium PostgreSQL Connector** — Outbox CDC for wallet and transaction services
- **Apache Avro 1.12.1** — Event serialization format
- **Confluent Kafka Avro Serializer 8.3.0**

### Security
- **Spring Security** — Endpoint protection
- **JWT (JJWT 0.12.6)** — Token-based authentication; validated at the gateway

### Observability
- **Spring Boot Actuator** — Health checks and metrics
- **Spring Boot OpenTelemetry** — Distributed tracing
- **Micrometer + datasource-micrometer** — DB query tracing
- **OpenTelemetry Collector** — Telemetry fan-out (traces → Jaeger, metrics → Prometheus)
- **Jaeger** — Distributed tracing backend
- **Prometheus** — Metrics collection (scrapes OTEL Collector on port `8889`)
- **Grafana** — Metrics visualization

### Logging
- **Filebeat** — Ships structured (ECS format) JSON log files to Logstash
- **Logstash** — Parses and forwards logs to Elasticsearch
- **Elasticsearch 9.5.2** — Log storage and search
- **Kibana 9.5.2** — Log visualization and querying
- **Elastic APM Server 9.5.2** — Application performance monitoring

### CI/CD
- **Gitea Actions** — Monorepo CI with path-based change detection (only rebuilds affected modules)

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**

Each service has its own PostgreSQL database (started from individual `docker-compose.yml` files or via the per-service DB setup). The root `docker-compose.yml` starts the shared infrastructure (Kafka, Schema Registry, Debezium Connect, observability stack).

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd asset-management-spring-microservice
```

### 2. Start Per-Service Databases

Each service that needs a database has its own `docker-compose.yml`:

```bash
# User service DB (PostgreSQL on host port 5433)
docker-compose -f user/docker-compose.yml up -d

# Wallet service DB (PostgreSQL on host port 5434) + Redis (port 6379)
docker-compose -f wallet/docker-compose.yml up -d

# Transaction service DB (PostgreSQL on host port 5435)
docker-compose -f transaction/docker-compose.yml up -d
```

> ⚠️ The wallet and transaction databases require `wal_level=logical` for Debezium CDC — this is already set in their `docker-compose.yml` files.

### 3. Start Shared Infrastructure

```bash
docker-compose up -d
```

This starts:

| Service | URL |
|---------|-----|
| Kafka (KRaft) | `localhost:9092` |
| Schema Registry | `http://localhost:8010` |
| Debezium Connect | `http://localhost:8083` |
| Kafka UI | `http://localhost:9095` |
| OpenTelemetry Collector | `localhost:4317` (gRPC) / `localhost:4318` (HTTP) |
| Jaeger | `http://localhost:16686` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Elasticsearch | `http://localhost:9200` |
| Logstash (Beats) | `localhost:5044` |
| Kibana | `http://localhost:5601` |
| APM Server | `http://localhost:8200` |

### 4. Build the Project

```bash
mvn clean install -DskipTests
```

### 5. Seed Initial Data

After building, run the seeders to populate currencies and the admin user:

```bash
./seed_database.sh
```

This runs:
- Wallet service in seed mode → seeds currency data
- User service in seed mode → creates the admin user (phone: `09183385896`, password: `123456`)

### 6. Register Debezium Connectors

```bash
./register_connector.sh wallet-outbox.json
./register_connector.sh transaction-outbox.json
```

### 7. Run the Microservices

Start services in this order:

```bash
# 1. Service Registry
cd eureka && mvn spring-boot:run

# 2. API Gateway
cd gateway && mvn spring-boot:run

# 3. Domain services (order doesn't matter after Eureka is up)
cd user && mvn spring-boot:run
cd wallet && mvn spring-boot:run
cd transaction && mvn spring-boot:run
```

### 8. Verify

- Eureka Dashboard: `http://localhost:8761` — all services should appear as UP
- Kafka UI: `http://localhost:9095` — inspect topics and consumer groups

## 📡 Service Communication

### API Gateway Routing

All external traffic goes through the gateway at port **9099**:

| Path | Upstream Service |
|------|-----------------|
| `/api/auth/**` | User Service (`USER-SERVICE`) |
| `/api/wallets/**` | Wallet Service (`WALLET-SERVICE`) |
| `/api/transaction/**` | Transaction Service (`TRANSACTION-SERVICE`) |

### Event-Driven Architecture (Kafka Topics)

Services communicate asynchronously over Kafka. Topics defined in `KafkaTopics`:

| Topic | Publisher | Consumer |
|-------|-----------|----------|
| `user-registered-topic` | User (via CDC Outbox) | Wallet |
| `wallet-transfer` | Transaction | Wallet |
| `wallet-transfer-response` | Wallet (via CDC Outbox) | Transaction |
| `withdraw-failed` | Wallet | Transaction |
| `wallet-deposit` | Transaction | Wallet |
| `deposit-success` | Wallet | Transaction |
| `deposit-failed` | Wallet | Transaction |

### Outbox Pattern & CDC

Both `wallet` and `transaction` services implement the **Transactional Outbox Pattern**:

1. On a business operation, the service writes an `OutBox` record in the same DB transaction
2. Debezium watches the `outbox_events` table via PostgreSQL logical replication (`pgoutput`)
3. The `EventRouter` transform routes each event to its target Kafka topic (stored in the `topic` column)
4. Avro-serialized payloads are forwarded byte-for-byte with `ByteArrayConverter`

Connector configs: `debezium/wallet-outbox.json`, `debezium/transaction-outbox.json`

## 🔍 Observability

### Distributed Tracing
- **Jaeger UI**: `http://localhost:16686`
- Each service exports OTLP traces (HTTP) to the OTEL Collector → Jaeger
- DB queries are traced via `datasource-micrometer`
- Kafka listener and template observation is enabled per service

### Metrics
- **Prometheus**: `http://localhost:9090`
- Prometheus scrapes the OTEL Collector's Prometheus exporter on port `8889`
- **Grafana**: `http://localhost:3000` (anonymous access with Admin role)

### Logging (ELK Stack)
- Each service writes structured **ECS-format** JSON logs to `./logs/application.log`
- **Filebeat** ships log files from `./*/logs/*.log` to **Logstash** on port `5044`
- **Logstash** parses the JSON and writes to **Elasticsearch** (daily indices: `application-logs-YYYY.MM.dd`)
- **Kibana**: `http://localhost:5601` — query and visualize logs

### Health Checks

Each service exposes:
```
http://<host>:<port>/actuator/health
http://<host>:<port>/actuator/health/liveness
http://<host>:<port>/actuator/health/readiness
```

## 📁 Project Structure

```
.
├── eureka/                  # Service Registry
├── gateway/                 # API Gateway (JWT validation, routing)
├── user/                    # User Management Service
│   └── docker-compose.yml   # User PostgreSQL DB (host port 5433)
├── wallet/                  # Wallet Service
│   └── docker-compose.yml   # Wallet PostgreSQL DB (port 5434) + Redis (port 6379)
├── transaction/             # Transaction & Ledger Service
│   └── docker-compose.yml   # Transaction PostgreSQL DB (host port 5435)
├── shared-contracts/        # Avro schemas (.avsc) and generated event classes
├── shared-module/           # OutBox entity, KafkaTopics, AvroPayloadSerializer
├── debezium/                # Debezium connector configs
│   ├── wallet-outbox.json
│   └── transaction-outbox.json
├── filebeat/
│   └── filebeat.yml         # Ships ./*/logs/*.log to Logstash
├── logstash/
│   └── pipeline/logstash.conf
├── .gitea/workflows/        # Gitea Actions CI (per-module path filtering)
├── docker-compose.yml       # Shared infrastructure (Kafka, Observability, ELK)
├── pom.xml                  # Parent Maven POM
├── prometheus.yml           # Scrapes OTEL Collector on :8889
├── otel-collector-config.yaml
├── seed_database.sh         # Seeds currencies and admin user
├── register_connector.sh    # Registers a Debezium connector via REST
├── create_new_module.sh     # Scaffolds a new service module from the user template
└── .http                    # IntelliJ HTTP scratch file (register, login, transfer)
```

## 🔧 Configuration

### Per-Service Application Properties

| Service | Config file | Key overrides |
|---------|------------|---------------|
| gateway | `application.yaml` | Routes, JWT secret, OTEL |
| user | `application.properties` | DB port 5433, Kafka producer |
| wallet | `application.properties` | DB port 5434, Redis, Kafka consumer/producer |
| transaction | `application.properties` | DB port 5435, Kafka consumer/producer (idempotent) |
| eureka | `application.properties` | Standalone (no client registration) |

All sensitive values support environment variable overrides (e.g. `DB_URL`, `JWT_SECRET`, `KAFKA_BOOTSTRAP_SERVERS`, `SCHEMA_REGISTRY_URL`, `OTLP_ENDPOINT`).

### Database Setup
PostgreSQL schemas are created automatically on startup (`spring.jpa.hibernate.ddl-auto=update`).

### Kafka & Schema Registry
- Kafka bootstrap: `localhost:9092` (external) / `kafka:29092` (internal Docker)
- Schema Registry: `http://localhost:8010` (external) / `http://schema-registry:8081` (internal)

## 🧪 Testing

```bash
# All modules
mvn clean test

# Single module
cd <module-name>
mvn clean test
```

CI runs per-module tests on push/PR to `main`, triggered only when files in that module change.

## 📦 Building & Deployment

### Build JAR Files
```bash
mvn clean package -DskipTests
```

### Docker Build

Each service has a multi-stage Dockerfile that builds only the required modules:

```bash
# Build from project root (context must be root for shared deps)
docker build -f wallet/Dockerfile -t wallet-service .
docker build -f user/Dockerfile -t user-service .
docker build -f transaction/Dockerfile -t transaction-service .
docker build -f gateway/Dockerfile -t gateway-service .
docker build -f eureka/Dockerfile -t eureka-service .
```

## 🤝 Adding a New Module

Use the provided scaffold script:

```bash
./create_new_module.sh <module-name>
```

This copies the `user` module structure, updates `pom.xml` artifact IDs, and registers the module in the root `pom.xml`. Then:

1. Update `src/main/resources/application.properties` (port, DB URL, etc.)
2. Add a Eureka client dependency if service discovery is needed
3. Add a Dockerfile following the same multi-stage pattern
4. Add a Gitea Actions job in `.gitea/workflows/buid-test.yaml`

## 📝 API Quick Reference

All requests go through the gateway at `http://localhost:9099`:

```
POST /api/auth/register   — Register a new user
POST /api/auth/login      — Login, returns JWT
POST /api/transaction/    — Submit a transfer (requires Bearer token)
GET  /api/transaction/    — List transactions (requires Bearer token)
```

See `.http` for ready-to-run IntelliJ HTTP Client requests including token extraction.

## 🛡️ Security

- **JWT authentication** validated at the gateway by `JwtAuthenticationGatewayFilter`
- JWT secret and expiry configurable via `jwt.secret` / `jwt.access-expiration`
- Role-based access control with `ADMIN` and `USER` roles (set at registration/seeding)
- Public endpoints: `/api/auth/register`, `/api/auth/login`

## 🐛 Troubleshooting

**Services not registering in Eureka**
- Confirm Eureka is running: `http://localhost:8761`
- Check `eureka.client.service-url.defaultZone` in each service's properties

**Kafka/Schema Registry connection issues**
```bash
docker-compose ps
docker exec kafka-server kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

**Debezium connector not streaming**
- Check connector status: `curl http://localhost:8083/connectors/wallet-outbox-connector/status`
- Verify PostgreSQL has `wal_level=logical` enabled
- Re-register if needed: `./register_connector.sh wallet-outbox.json`

**No traces in Jaeger**
- Confirm OTEL Collector is running: `docker-compose ps`
- Verify `management.opentelemetry.tracing.export.otlp.endpoint` in application properties

**No logs in Kibana**
- Confirm Filebeat, Logstash, Elasticsearch are running: `docker-compose ps`
- Check Filebeat picks up logs: `docker logs filebeat`
- Create an index pattern in Kibana matching `application-logs-*`

## 🔗 Useful Commands

### Maven
```bash
mvn clean install -DskipTests        # Build without tests
mvn clean install                    # Build with tests
cd <module-name> && mvn spring-boot:run
```

### Docker Compose
```bash
docker-compose up -d                 # Start shared infra
docker-compose down                  # Stop shared infra
docker-compose logs -f <service>     # Tail logs
docker-compose restart <service>     # Restart a service
docker-compose ps                    # Check running containers
```

### Kafka
```bash
# List topics
docker exec kafka-server kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create a topic
docker exec kafka-server kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic <topic-name> --partitions 4 --replication-factor 1

# Consume from beginning
docker exec kafka-server kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic <topic-name> --from-beginning
```

### Debezium
```bash
# Register a connector
./register_connector.sh wallet-outbox.json

# Check connector status
curl http://localhost:8083/connectors/wallet-outbox-connector/status | jq .

# List all connectors
curl http://localhost:8083/connectors | jq .
```

## 📄 License

Add your license information here.

## 👥 Authors

- Project Team

## 📚 References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [Debezium Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Elastic Stack Documentation](https://www.elastic.co/guide/index.html)