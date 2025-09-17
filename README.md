# 🌍 GlobeX – Distributed Currency Conversion Platform

### **Project Overview**

**GlobeX** is a production-grade currency conversion platform designed to handle real-time exchange rate requests with high reliability and performance. While its core function is simple, the project's primary goal is to demonstrate mastery of complex backend engineering principles, including distributed systems, asynchronous workflows, and cloud-native architecture.

---

### **Core Features & Architectural Principles**

-   **Microservices Architecture:** The system is composed of four distinct, independently deployable services—Conversion, Aggregator, Worker, and History—each with a clear, single responsibility.
-   **Low-Latency Caching:** Utilizes **Redis (AWS ElastiCache)** as a hot cache to serve currency conversion requests with sub-millisecond latency.
-   **Asynchronous Processing:** Leverages **AWS SQS** for a resilient, asynchronous background refresh mechanism, decoupling the public API from rate fetching and persistence logic.
-   **Data Aggregation & Resilience:** Aggregates exchange rates from multiple external providers to ensure data accuracy and reliability. Implements **retries** and **circuit breakers** to gracefully handle external API failures.
-   **Durable Persistence:** Stores an authoritative history of all fetched rates in **Postgres (AWS RDS)**, providing a durable source of truth and a fallback for cache misses.
-   **Infrastructure-as-Code:** The entire cloud infrastructure (ECS, RDS, SQS, ElastiCache) is defined and provisioned using **Terraform**, ensuring the environment is reproducible and scalable.

---

### **Tech Stack**

| Category | Technology |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3 |
| **Persistence** | Spring Data JPA, Hibernate, PostgreSQL (AWS RDS) |
| **Caching** | Redis (AWS ElastiCache) |
| **Messaging** | AWS SQS |
| **Cloud Infrastructure** | AWS ECS Fargate, ALB, Secrets Manager |
| **Infra as Code** | Terraform |
| **Observability** | Spring Actuator, Micrometer |

---
