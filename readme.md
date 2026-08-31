# Asset Management Spring Microservice

A comprehensive Spring Boot microservices application for managing digital assets, users, wallets, and transactions. This project demonstrates modern microservices architecture patterns with service discovery, API gateway routing, event streaming, and distributed tracing.

## 🏗️ Architecture Overview

This is a **Spring Cloud microservices** architecture with the following key components:

### Microservices Modules

| Module | Purpose | Port |
|--------|---------|------|
| **gateway** | API Gateway with Spring Cloud Gateway & Security | 8080 |
| **eureka** | Service Registry & Discovery | 8761 |
| **user** | User management service | 8081 |
| **wallet** | Wallet/asset management service | 8082 |
| **transaction** | Transaction processing service | 8083 |
| **shared-contracts** | Shared data contracts and DTOs | - |
| **shared-module** | Shared utilities and base infrastructure | - |
| **debezium** | Change Data Capture (CDC) for event streaming | 8083 |

### Shared Infrastructure

- **shared-contracts**: Contains common Avro schemas and event contracts
- **shared-module**: Shared utilities, configurations, and base classes

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot 4.1.0** - Application framework
- **Spring Cloud 2025.1.2** - Microservices orchestration
- **Java 17** - Programming language

### API & Communication
- **Spring Cloud Gateway** - API Gateway with routing and filtering
- **Spring Cloud Netflix Eureka** - Service discovery
- **Spring Web MVC** - REST API development
- **Spring Webflux** - Reactive programming support

### Data Management
- **Spring Data JPA** - ORM and database access
- **PostgreSQL** - Primary relational database
- **Spring Data Redis** - Caching layer
- **Redis** - In-memory data store

### Event Streaming & CDC
- **Apache Kafka 4.0.0** - Message broker
- **Confluent Schema Registry 8.0.0** - Schema management
- **Debezium** - Change Data Capture
- **Apache Avro 1.12.1** - Serialization format
- **Confluent Kafka Avro Serializer 8.3.0** - Avro serialization

### Security
- **Spring Security** - Authentication and authorization
- **JWT (JJWT 0.12.6)** - Token-based authentication

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Spring Boot OpenTelemetry** - Distributed tracing
- **Micrometer** - Application metrics
- **Jaeger** - Distributed tracing backend
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization
- **Loki** - Log aggregation
- **OpenTelemetry Collector** - Telemetry collection

### Testing
- **Spring Boot Test** - Unit and integration testing
- **Spring Security Test** - Security testing
- **Reactor Test** - Reactive testing

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.8+** - Build tool
- **Docker & Docker Compose** - For running infrastructure services
- **PostgreSQL 14+** (optional if using Docker Compose)
- **Redis** (optional if using Docker Compose)
- **Kafka 4.0.0+** (optional if using Docker Compose)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd asset-management-spring-microservice
```

### 2. Start Infrastructure Services

Start all required services using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- Kafka brokers
- Schema Registry
- Debezium Connect
- Kafka UI (http://localhost:9095)
- OpenTelemetry Collector
- Jaeger (http://localhost:16686)
- Prometheus (http://localhost:9090)
- Grafana (http://localhost:3000)
- Loki (http://localhost:3100)

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Microservices

Start each microservice in order:

#### Service Registry (Eureka)
```bash
cd eureka
mvn spring-boot:run
```

#### API Gateway
```bash
cd gateway
mvn spring-boot:run
```

#### User Service
```bash
cd user
mvn spring-boot:run
```

#### Wallet Service
```bash
cd wallet
mvn spring-boot:run
```

#### Transaction Service
```bash
cd transaction
mvn spring-boot:run
```

### 5. Verify Services are Running

Check Eureka Dashboard: http://localhost:8761

All services should be registered and showing as UP.

## 📡 Service Communication

### API Gateway Routing
The API Gateway routes requests to microservices:
- `/user/**` → User Service
- `/wallet/**` → Wallet Service
- `/transaction/**` → Transaction Service

### Event-Driven Architecture
Services communicate asynchronously through Kafka topics:
- **wallet-outbox** - Wallet events (CDC topic)
- Other event topics for wallet and transaction events

### Change Data Capture (CDC)
Debezium captures database changes and streams them to Kafka, enabling:
- Event sourcing
- Real-time synchronization
- Audit logging

## 🔍 Observability

### Distributed Tracing
- Access Jaeger UI: http://localhost:16686
- Traces are automatically collected via OpenTelemetry
- Each microservice sends telemetry to the OTEL Collector

### Metrics & Monitoring
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (Anonymous access enabled)
- Metrics are collected from each service's `/actuator/metrics` endpoint

### Logging
- **Loki**: http://localhost:3100
- Logs can be queried through Grafana

### Health Checks
Each service exposes health endpoints:
- `http://<service>:port/actuator/health`
- `http://<service>:port/actuator/health/liveness`
- `http://<service>:port/actuator/health/readiness`

## 📁 Project Structure

```
.
├── eureka/                 # Service Registry
├── gateway/                # API Gateway
├── user/                   # User Management Service
├── wallet/                 # Wallet Service
├── transaction/            # Transaction Service
├── shared-contracts/       # Shared Avro schemas and DTOs
├── shared-module/          # Shared utilities and configurations
├── debezium/              # Debezium CDC configuration
├── docker-compose.yml      # Infrastructure services
├── pom.xml                 # Parent Maven POM
├── prometheus.yml          # Prometheus configuration
├── otel-collector-config.yaml  # OpenTelemetry configuration
└── README.md              # This file
```

## 🔧 Configuration

### Service Configuration
Each microservice has its own `application.properties` file in `src/main/resources/`:
- Database connection strings
- Kafka broker addresses
- Eureka server URL
- Redis configuration

### Database Setup
PostgreSQL schemas are created automatically on service startup (JPA `spring.jpa.hibernate.ddl-auto=update`).

### Kafka Configuration
- Bootstrap servers: `kafka:29092` (internal) or `localhost:9092` (external)
- Schema Registry: `http://schema-registry:8081`

## 🧪 Testing

Run all tests:
```bash
mvn clean test
```

Run tests for a specific module:
```bash
cd <module-name>
mvn clean test
```

## 📦 Building & Deployment

### Build JAR Files
```bash
mvn clean package
```

JAR files are created in each module's `target/` directory.

### Docker Build (Optional)
Each microservice can be containerized:
```bash
cd <module-name>
docker build -t <image-name> .
```

## 🤝 Contributing

When adding new modules to the microservices:

1. Use the provided `create_new_module.sh` script:
```bash
./create_new_module.sh <module-name>
```

2. Add the module to the parent `pom.xml` modules section

3. Configure service properties in `src/main/resources/application.properties`

4. Register the service in Eureka by including spring-cloud-starter-netflix-eureka-client dependency

## 📝 API Documentation

API endpoints are available through the API Gateway:
- **Gateway URL**: http://localhost:8080
- **User Service**: http://localhost:8080/user
- **Wallet Service**: http://localhost:8080/wallet
- **Transaction Service**: http://localhost:8080/transaction

Each service implements REST endpoints documented in their respective controller classes.

## 🛡️ Security

### Authentication
- JWT-based authentication
- Token validation in API Gateway
- Role-based access control (RBAC)

### Authorization
- Spring Security for endpoint protection
- Method-level security annotations

## 📊 Monitoring Dashboards

### Kafka UI
- **URL**: http://localhost:9095
- Monitor topics, brokers, and consumer groups

### Jaeger Traces
- **URL**: http://localhost:16686
- View distributed traces across services

### Grafana Dashboards
- **URL**: http://localhost:3000
- Query Prometheus metrics and Loki logs
- Default credentials: admin/admin (if not using anonymous)

## 🐛 Troubleshooting

### Services not registering in Eureka
- Check Eureka server is running (http://localhost:8761)
- Verify `eureka.client.service-url.defaultZone` in service application.properties

### Kafka connection issues
- Ensure Docker Compose services are running: `docker-compose ps`
- Check Kafka is accessible: `docker exec kafka-server kafka-broker-api-versions.sh --bootstrap-server localhost:9092`

### Database connection errors
- Verify PostgreSQL credentials in application.properties
- Check database is created and accessible

### No traces in Jaeger
- Verify OTEL Collector is running: `docker-compose ps`
- Check service OpenTelemetry configuration

## 📄 License

Add your license information here.

## 👥 Authors

- Project Team

## 📚 References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)

## 🔗 Useful Commands

### Maven Commands
```bash
# Build without tests
mvn clean install -DskipTests

# Run specific module
cd <module-name> && mvn spring-boot:run

# Generate project info
mvn project-info-reports:dependencies
```

### Docker Compose Commands
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f <service-name>

# Restart a service
docker-compose restart <service-name>
```

### Kafka Commands
```bash
# List topics
docker exec kafka-server kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create a topic
docker exec kafka-server kafka-topics.sh --create --bootstrap-server localhost:9092 --topic <topic-name> --partitions 4 --replication-factor 1

# Consume messages
docker exec kafka-server kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic <topic-name> --from-beginning
```

## 🎯 Next Steps

1. Review service-specific README files (if available)
2. Configure environment-specific properties
3. Set up CI/CD pipeline
4. Configure production-grade security
5. Set up log aggregation and alerting
6. Deploy to your target environment
