# 🌍 Vaxly – Distributed Currency Conversion Microservices Platform

Vaxly is a **scalable, distributed currency conversion microservices platform** designed to handle real-time exchange rate requests with high reliability, low latency, and fault tolerance. While the core function is simple, the project demonstrates advanced backend engineering principles, including **distributed systems**, **asynchronous workflows**, **event-driven architecture**, and **cloud-native design**.

---

## 🚀 Project Overview

Vaxly consists of multiple microservices interacting via **REST APIs, Redis caching, SQS queues, and a relational database**, achieving high performance, scalability, and reliability.  

### Key Services

1. **Conversion Service (Client API)**  
   - Handles client requests for currency conversion (e.g., USD → EUR).  
   - Implements a **cache-first strategy**:  
     1. Checks **Redis cache**.  
     2. Falls back to **Historical Service**.  
     3. If missing, queries **Aggregator Service** for live rates.  
   - Enqueues new or frequently requested currency pairs to **SQS** for asynchronous processing.  

2. **Historical Service**  
   - Stores historical currency rates in a **database**.  
   - Provides APIs to retrieve and update rates.  
   - Secured with **Role-Based Access Control (RBAC)**.  

3. **Scheduler Service**  
   - Periodically queries Redis for top N requested currency pairs.  
   - Checks **in-flight flags** and `last_refresh` timestamps to avoid duplicate work.  
   - Enqueues eligible jobs to **SQS** and sets TTL flags to prevent stale processing.  

4. **Worker Service**  
   - Consumes jobs from **SQS**.  
   - Fetches rates from **Aggregator Service**.  
   - Updates **Redis** and **Historical Service**.  
   - Clears `in-flight` flags and updates `last_refresh` timestamps.  

5. **Aggregator Service**  
   - Retrieves live rates from multiple **third-party providers**.  
   - Provides API endpoints for other services to query live data.  

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3 |
| **Persistence** | PostgreSQL (AWS RDS), Spring Data JPA, Hibernate |
| **Caching** | Redis (AWS ElastiCache) |
| **Messaging** | AWS SQS (FIFO & Standard) |
| **Infrastructure** | AWS ECS Fargate, ALB, Secrets Manager |
| **Infrastructure as Code** | Terraform |
| **Observability** | Spring Actuator, Micrometer, Prometheus |
| **Security** | Spring Security (RBAC) |

---

## 🔑 Architectural Patterns

- **Cache-Aside / Lazy Loading**: Conversion Service checks Redis before querying other services.  
- **Event-Driven Architecture**: Scheduler → SQS → Worker enables asynchronous, decoupled processing.  
- **Distributed System Resilience**: TTLs and in-flight flags prevent duplicate updates; failed jobs are retried.  
- **Role-Based Access Control (RBAC)**: Secure endpoints in Historical Service.  
- **Priority Queueing**: High-priority (top N) vs low-priority pairs in SQS.  

---

## ⚡ Workflow

1. Client requests a currency conversion (e.g., USD → EUR).  
2. Conversion Service checks **Redis**:  
   - **Hit** → returns cached rate.  
   - **Miss** → queries Historical Service → if unavailable, queries Aggregator Service.  
3. New or frequently requested pairs are **enqueued to SQS**.  
4. Scheduler Service periodically queries Redis for top N pairs and enqueues eligible jobs to **SQS**.  
5. Worker Service processes SQS jobs:  
   - Fetches live rates from Aggregator Service.  
   - Updates Redis and Historical Service.  
   - Clears `in-flight` flags and updates timestamps.  
6. Aggregator Service ensures **reliable, accurate live rates** from multiple external providers.  

---

## 🗺 Architecture Diagram

```text
                 ┌───────────────────────┐
                 │      Client API       │
                 │  (Conversion Service) │
                 └─────────┬─────────────┘
                           │ Request: USD/EUR
                           ▼
                   ┌─────────────┐
                   │   Redis     │
                   └─────┬───────┘
            Hit        │ Miss
            │          ▼
            │     ┌─────────────┐
            │     │ History DB  │
            │     └─────┬───────┘
            │ Hit         │ Miss
            │             ▼
            │      ┌───────────────┐
            │      │ Aggregator    │
            │      │ Service       │
            │      └─────┬─────────┘
            │            │ Fetch live 3rd-party rate
            │            ▼
Return value│    ┌─────────────┐
 to client  │    │   Rate      │
            │    └─────┬──────┘
            │ Enqueue new pair to SQS
            │
            ▼
      ┌─────────────────────────┐
      │         SQS Queue        │
      │ High-priority: top N     │
      │ Low-priority: others     │
      └─────┬───────────────────┘
            │
            ▼
      ┌─────────────┐
      │   Worker    │
      │  Service    │
      └─────┬───────┘
   1. Set "in-flight" flag in Redis (TTL)
   2. Fetch rate from Aggregator
   3. Update Redis + History DB
   4. Clear "in-flight" flag
   5. Update "last_refresh" timestamp in Redis
            ▲
            │
┌───────────┴──────────────┐
│   Scheduler Service       │
│   (every N seconds)       │
│   1. Query Redis for top N|
│      most requested pairs │
│   2. Check last_refresh & │
│      in-flight flags      │
│   3. Enqueue eligible pairs│
│      to SQS (high/low)   │
└───────────────────────────┘
