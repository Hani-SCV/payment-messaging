# Payment Messaging

결제 시스템을 기반으로 **Transactional Outbox Pattern과 Apache Kafka를 활용한 비동기 메시징 아키텍처**를 학습하는 프로젝트입니다.

결제 처리 과정에서 발생하는 이벤트를 Outbox 테이블에 원자적으로 저장하고, 별도의 Publisher가 Kafka로 발행한 뒤 Consumer가 이를 수신하여 후속 작업을 처리하는 구조를 구현합니다.

## Architecture

```text
                   Payment API
                       |
                       v
                 MySQL Transaction
                  /           \
             Payment         Outbox
                                |
                                v
                       Outbox Publisher
                                |
                                v
                          Kafka Broker
                                |
                                v
                       Payment Consumer
                                |
                                v
                       Event Processing
```

각 애플리케이션은 독립적으로 실행 및 배포할 수 있도록 분리하며, Gradle Multi-Project 구조로 하나의 저장소에서 관리합니다.

## Modules

| Module | Responsibility |
|---|---|
| `payment-api` | 결제 요청 처리 및 Payment, Outbox 저장 |
| `outbox-publisher` | Outbox 이벤트 조회 및 Kafka 발행 |
| `payment-consumer` | Kafka 이벤트 수신 및 후속 처리 |

### Payment API

결제 요청을 처리하고 Payment와 Outbox 이벤트를 하나의 DB 트랜잭션으로 저장합니다.

결제 데이터와 이벤트 데이터의 원자성을 보장하여, 결제 처리는 완료됐지만 이벤트가 발행되지 않는 문제를 방지하는 것이 목적입니다.

### Outbox Publisher

Outbox 테이블에서 아직 발행되지 않은 이벤트를 조회하고 Kafka로 발행합니다.

이벤트 발행 성공 및 실패에 따른 재시도와 발행 상태 관리 등을 구현할 예정입니다.

### Payment Consumer

Kafka에서 결제 이벤트를 수신하고 후속 작업을 처리합니다.

중복 메시지 수신에 대비한 멱등성 처리와 메시지 처리 실패에 대한 대응을 구현할 예정입니다.

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Build | Gradle 9.8.2 |
| Database | MySQL 8.4 |
| ORM | Spring Data JPA |
| Messaging | Apache Kafka 4.4.0 |
| Kafka Mode | KRaft |
| Mapping | MapStruct |
| Container | Docker Compose |
| Test | JUnit 5, Spring Boot Test |

## Infrastructure

Docker Compose를 통해 MySQL과 Kafka를 실행합니다.

| Service | Version | Port |
|---|---|---|
| MySQL | 8.4 | 53306 |
| Kafka | 4.4.0 | 9094 |

Kafka는 ZooKeeper 없이 KRaft 모드로 구성했으며, 초기 학습 환경에서는 단일 Broker를 사용합니다.

### Kafka Topic

| Topic | Partitions | Replication Factor |
|---|---:|---:|
| `payment-events` | 3 | 1 |

## Getting Started

### Requirements

- Java 21
- Docker
- Docker Compose

### 1. Infrastructure 실행

```bash
docker compose up -d
```

### 2. Gradle Build

```bash
./gradlew clean build
```

### 3. Application 실행

각 모듈은 독립적으로 실행할 수 있습니다.

```bash
# Payment API
./gradlew :payment-api:bootRun

# Outbox Publisher
./gradlew :outbox-publisher:bootRun

# Payment Consumer
./gradlew :payment-consumer:bootRun
```

## Learning Goals

- Transactional Outbox Pattern
- Kafka Producer / Consumer
- 비동기 이벤트 기반 아키텍처
- DB Transaction과 메시지 발행 간의 원자성 문제
- At-least-once Delivery와 멱등성
- 메시지 재시도 및 장애 처리
- Kafka Partition과 Consumer Group
- 서비스 간 책임 분리 및 독립적인 확장

## Related Documents

기존 결제 콜백 과제의 요구사항과 테스트 데이터는 다음 문서에서 확인할 수 있습니다.

- [Assignment](docs/ASSIGNMENT.md)
- [Incident](docs/INCIDENT_2025_11.md)
- [Payload Provenance](docs/PAYLOAD_PROVENANCE.md)
- [Requirements Memo](docs/REQUIREMENTS_MEMO.md)
