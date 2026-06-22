# DocuSign Backend Architecture Engine

A clean, backend infrastructure clone of the digital signature platform DocuSign.

This engine orchestrates multi-party document signing lifecycles, implementing stateless authentication, sequential and parallel signing workflow state machines, event-driven audit logging, and transactional notification dispatching. Built using clean Domain-Driven Design (DDD) principles and defensive software engineering patterns, this project demonstrates how to structure real-world business logic, enforce data isolation, and handle asynchronous state transitions cleanly in a Spring Boot ecosystem.

## 📋 Table of Contents
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Domain Model](#-domain-model)
- [Project Structure](#-project-structure)
- [Security & Defenses](#-security--defenses)
- [Signing Workflows](#-signing-workflows)
- [Audit & Notification System](#-audit--notification-system)
- [Controller & API Design](#-controller--api-design)
- [Service Layer Design](#-service-layer-design)
- [Production Gap Analysis & Planned Upgrades](#-production-gap-analysis--planned-upgrades)
- [Getting Started](#-getting-started)

---

## 🛠️ Tech Stack

| Technology | Purpose | Selection Rationale |
| :--- | :--- | :--- |
| **Java 17** | Core Language | Long-Term Support (LTS) release offering modern language semantics and robust type safety. |
| **Spring Boot 4.0.x** | Application Framework | Production-ready runtime environment offering rapid dependency injection and standard auto-configuration engines. |
| **Spring Security + JWT** | Stateless Authentication | Provides stateless `Bearer` token validation, decoupling server memory from active sessions to allow seamless horizontal scaling. |
| **Spring Data JPA** | Object-Relational Mapping | Abstracts the persistence layer with clean repository interfaces and automated transaction tracking. |
| **PostgreSQL** | Relational Database | Battle-tested engine utilized to enforce ACID properties and strict referential integrity across document states. |
| **Lombok** | Boilerplate Reduction | Cleans up domain models by auto-generating getters, setters, and builders at compilation time. |
| **Spring Events** | Event-Driven Architecture | Decouples secondary concerns (Audit, Notification) from the core transactional signing pipeline. |

---

## 🏗️ System Architecture

The application is structured around a clean, decoupled layered architecture to isolate concerns and enforce predictability:


```

[ HTTP Controller Layer ]  --> Receives HTTP data, extracts protocol metadata, delegates instantly.
│
[ Service Core Layer ]     --> Orchestrates transactional business rules and process flows.
│
[ Repository Access Layer] --> Abstracts SQL operations via Spring Data JPA.
│
[ Domain Core Layer ]      --> Rich entities, enums, lifecycle state machine boundaries.
│
[ Async Event Layer ]      --> Listens to non-blocking system broadcasts asynchronously.

```

The system employs an **Event-Driven Architecture (EDA)** for auxiliary operations. Core workflow services broadcast transactional changes via Spring's `ApplicationEventPublisher`. Decoupled listeners catch these signals asynchronously to stream audit logs and notifications, ensuring the main application thread remains highly performant and free of network I/O blockages.

---

## 🗂️ Domain Model

### User
The primary actor. Implements Spring Security's `UserDetails` natively to streamline credential evaluation. A single `User` record gracefully maps to both a Sender and a Signer relationship simultaneously, mirroring the real-world operational requirement where a single account needs to dispatch contracts while signing incoming requests.

### Document
A pure storage tracking unit. Manages metadata regarding uploaded file states. To enforce strict decoupling, a `Document` entity only retains awareness of its original uploader; it contains no logic regarding who needs to sign it. 
* **Lifecycle States:** `DRAFT` ➔ `PENDING` ➔ `COMPLETED`

### SignatureRequest
The central aggregate root orchestrating the contract lifecycle. Links a specific `Document` to an array of assigned signers and tracks global completion markers.
* **Lifecycle States:** `PENDING` ➔ `COMPLETED` / `DECLINED`

### Signer
Represents a specific person's distinct participation slot inside an active workflow sequence. It encapsulates metadata specific to that unique interaction—such as `signingOrder`, `status`, and `signedAt`—preventing individual state pollution on the universal `User` profile.

### SigningProcess
Captures the forensic, immutable evidence required for legal non-repudiation. Records the exact global UTC timestamp and the client network IP address at the definitive millisecond of digital signature execution.

### AuditLog
An append-only, chronologically ascending data ledger tracking historical system events. Records are structured as immutable fragments, allowing legal/forensic investigators to trace the unbroken lifecycle of any given document.

### Notification
Transactional alerts distributed to users when state changes occur. Managed in a separate table to trace read/unread conditions per recipient account.

---

## 🔒 Security & Defenses

### 1. Method-Level SpEL Security Filters (BOLA Defense)
To prevent **Broken Object-Level Authorization (BOLA / IDOR)** vulnerabilities, the system intercepts traffic at the Controller threshold using Spring Security's `@PreAuthorize` annotation paired with a custom evaluation bean:
```java
@PreAuthorize("@documentSecurityEvaluator.isParticipant(#requestId, principal.username)")

```

This intercepts requests before invoking service logic, ensuring the authenticated principal is explicitly listed as either the original sender or an active, designated signer on the requested document UUID.

### 2. Temporal Normalization (`Instant` UTC Timeline)

To ensure absolute consistency across distributed server nodes or multi-region cloud clusters, the application bypasses regional machine wall clocks (`LocalDateTime`). All transactional and audit fields are bound directly to **`java.time.Instant`**. This stores records relative to a global UTC epoch anchor point, ensuring legal audit validation can withstand timestamp discrepancies.

### 3. Idempotent State Machine Controls

The business tier features protective logic traps to protect against race conditions, duplicate form submissions, or malicious API endpoint spamming:

```java
if (signer.getStatus() == SignerStatus.DECLINED) {
    throw new IllegalStateException("Business Logic Violation: This request has already been declined.");
}

```

If an actor multi-clicks or automates a request execution, the state machine hits this threshold, triggers a clean fast-fail block, and avoids redundant database flushes, duplicate audit streams, or system resource exhaustion.

### 4. Database-Level Structural Integrity

To prevent logical sequence corruption at the hardware layer, the persistence layer enforces a composite unique constraint on the `Signer` schema mapping:

```java
@Table(name = "signers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"signature_request_id", "signing_order"})
})

```

This blocks invalid configuration requests from ever creating overlapping signing slots within a sequential workflow, providing a physical safeguard beneath the application logic layer.

---

## 🔄 Signing Workflows

### PARALLEL Workflow

All designated signers receive the document broadcast simultaneously and can sign independently without sequence conditions. Perfect for corporate resolutions or shareholder approval cycles.

### SEQUENTIAL Workflow

Enforces a strict cascading hierarchy dictated by the `signingOrder` integer property. For instance, Signer #2 cannot execute their signature block until Signer #1 has successfully authorized the document. Attempting to sign prematurely triggers a validation exception: `"Waiting for previous signer to sign first."`

The sequencing boundaries are validated within `SignerWorkflowService.validateSigningOrder()` so that order checking lives natively as a core workflow domain contract rather than a peripheral data access check.

---

## 📡 Audit & Notification System

Both components execute on top of an decoupled asynchronous pipeline fed by a single `AuditEvent`.

```
[ Core Service Event ] ➔ Broadcasts AuditEvent
                             │
                             ├──► [ AuditLogListener ] ──► Compiles Forensics to Database
                             └──► [ NotificationListener ] ──► Dispatches Recipient Alert

```

Because these processors execute inside an independent async thread context, database execution latency or external notifications will never block or degrade the performance of the core signing transaction thread.

---

## 📦 Project Structure

```
src/main/java/com/docusign/docusign/
├── config/                  # Security filter configuration, JWT handlers, encryption beans
├── controller/              # REST controllers (strict input parsing, metadata mapping)
├── domain/                  # Rich database entities, status enums, JPA lifecycles
├── dto/                     # Decoupled Request/Response data transfer blueprints
│   ├── request/
│   └── response/
├── event/                   # AuditEvent payload models and publisher wrappers
├── repository/              # Spring Data JPA data persistence contracts
└── service/                 # Transactional business orchestration pipelines

```

---

## 📡 API Endpoints

### 🔐 Authentication (`/api/auth`)

* `POST /api/auth/register` - Registers an account profile. *(Public)*
* `POST /api/auth/login` - Authenticates credentials; yields JWT authorization header. *(Public)*

### 📄 Documents (`/api/documents`)

* `POST /api/documents/upload` - Uploads raw binary files via `multipart/form-data`.
* `GET /api/documents/{id}` - Retrieves a specific metadata file record by ID.
* `GET /api/documents?page=0&size=10&sort=createdAt,desc` - Lists paginated file assets uploaded by the authenticated user.

### ✍️ Signature Requests (`/api/signature-requests`)

* `POST /api/signature-requests` - Configures and creates a new multi-party signing workflow.
* `GET /api/signature-requests/{id}` - Retrieves a signature request tracking object with full signer matrices.
* `GET /api/signature-requests` - Displays history of all workflows initiated by the caller.

### 🖊️ Signer Interface (`/api/signer-workflow`)

* `GET /api/signer-workflow/pending` - Returns a custom list of pending signature actions assigned to the logged-in user.
* `POST /api/signer-workflow/requests/{requestId}/signers/{signerId}/decline` - Executes defensive decline logic to reject a signing contract.

### ✅ Signing Engine (`/api/signing`)

* `POST /api/signing/{signerId}/sign` - Records the forensic metadata of signature confirmation (IP, time-anchor).

### 📋 Audits & Alerts

* `GET /api/audit/{signatureRequestId}` - Retrieves complete historical immutable ledger trails.
* `GET /api/notifications` - Fetches comprehensive client notifications.

---

## ⚠️ Production Gap Analysis & Planned Upgrades

While the backend engine features a robust architectural core, the following optimizations are tracked for high-scale readiness:

### 🔴 Core Infrastructure Priorities

* **Storage Abstraction:** File handling currently targets the local host server filesystem. A migration strategy is slated to replace the local stream processor with AWS S3 or equivalent cloud object storage, swapping internal local path outputs with short-lived pre-signed download URLs.
* **Distributed Audit Splitting:** Transition `AuditLogService` into a fully isolated process line running out-of-band to prevent downstream relational logging locks from impacting execution paths during peak core execution intervals.

### 🔒 Expanded Authentication Controls

* **Rate-Limiting (Brute-Force Prevention):** Implement an API rate-limiting tier using a token bucket filter approach (e.g., Spring Cloud Gateway RateLimiter or Bucket4j) to prevent automated login credential stuffing.
* **Strict Email Verification Gates:** Activate the latent `isVerified` boolean attribute on user profiles, blocking the login filter from issuing active JWT tokens until an out-of-band email loop completes verification.

### 💾 Data Integrity Controls

* **Application Duplication Guardrails:** Add a pre-save check (`userRepository.existsByEmail()`) into the registration service pipeline to intercept email collisions and translate SQL state violations into uniform client messages.
* **Empty Assignment Constraints:** Introduce collection validation handlers (`@NotEmpty`) to reject incoming `SignatureRequest` payloads containing empty signer blocks, preventing dead-state workflows.

### 🖥️ API Contract Enhancements

* **Token Lifecycle Transparency:** Expand `AuthResponse` payloads to declare an explicit token expiration timestamp in milliseconds (`expiresIn`), allowing upstream client applications to predictably manage token refresh schedules without decoding JWT claims manually.
* **Notification Deep-Linking:** Enhance `NotificationResponse` attributes to expose the explicit `signatureRequestId` property, enabling direct client routing to actionable document views upon interaction with an alert banner.

---

## 🚀 Getting Started

### Prerequisites

* Java 17+
* Maven 3.8+
* PostgreSQL 14+

### Installation & Run Steps

1. **Clone the Source Engine:**

```bash
   git clone [https://github.com/AkshayTeja3/docusign-clone.git](https://github.com/AkshayTeja3/docusign-clone.git)
   cd docusign-clone

```

2. **Provision the Relational Engine:**

```sql
   CREATE DATABASE docusign;

```

3. **Configure the Properties Stack (`src/main/resources/application.properties`):**

```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/docusign
   spring.datasource.username=your_postgres_username
   spring.datasource.password=your_postgres_password
   spring.jpa.hibernate.ddl-auto=update
   
   jwt.secret=your_base64_encoded_hmac_sha256_secret_key_string
   jwt.expiration=86400000

```

4. **Compile and Execute:**

```bash
   ./mvnw spring-boot:run

```

---

*Developed as a clean, defensive digital signature backend infrastructure project.*
