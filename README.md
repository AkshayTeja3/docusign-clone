# Docusign Backend Clone

**A student-built backend clone of the document signing platform DocuSign.**

This system handles the core responsibilities of a real document signing platform — user authentication, document uploading, signature request creation, sequential and parallel signing workflows, audit logging, and real-time notifications. The goal behind building this was to demonstrate solid backend architecture skills and showcase hands-on experience with real-world technologies while understanding every design decision behind the system.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Domain Model](#-domain-model)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Endpoints](#-api-endpoints)
- [Authentication Flow](#-authentication-flow)
- [Signing Workflows](#-signing-workflows)
- [Audit & Notification System](#-audit--notification-system)
- [Controller Design](#-controller-design)
- [Service Layer Design](#-service-layer-design)
- [Security Design](#-security-design)
- [Known Limitations & Planned Improvements](#-known-limitations--planned-improvements)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 4.0.4 | Backend framework |
| Spring Security + JWT (jjwt 0.11.5) | Stateless authentication |
| Spring Data JPA | Database ORM |
| PostgreSQL | Relational database |
| Lombok | Reduces boilerplate code |
| Spring Events | Decoupled audit and notification system |

These technologies were chosen because they reflect real-world enterprise backend stacks. JWT was specifically chosen over traditional sessions because it is stateless and scales horizontally — the server does not need to store or look up session data for every request, making it ideal for REST APIs where clients and servers communicate independently. PostgreSQL is a battle-tested relational database that handles the referential integrity requirements of a document signing system, where relationships between users, documents, requests, and signers must always be consistent.

---

## 🏗️ System Architecture

The system is built around a clean layered architecture:

```
Controller Layer  →  receives HTTP requests, delegates to services, nothing more
Service Layer     →  all business logic, validation, orchestration
Repository Layer  →  database access via Spring Data JPA
Domain Layer      →  entity definitions, enums, state machine
Event Layer       →  decoupled audit logging and notifications
Config Layer      →  JWT, security filter chain, authentication wiring
```

The audit and notification system is intentionally **event-driven** — services publish events rather than calling audit or notification code directly. This decouples concerns so that adding a new notification type never requires touching the service that triggered the action. Services communicate what happened; the event system decides what to do about it.

---

## 🗂️ Domain Model

### `User`
The central actor in the system. A user can act as both a **sender** (someone who sends documents for signing) and a **signer** (someone who is asked to sign) simultaneously — because in the real world the same person may be sending documents to others while also being assigned as a signer on a different document. Keeping a single `User` entity with a `role` field reflects this flexibility without forcing artificial separation.

The `User` entity implements Spring Security's `UserDetails` directly so Spring can use it during authentication without a separate wrapper class. The `isVerified` flag is designed for a future identity verification step — every new user starts as unverified and would need to confirm their email before gaining full access. This is not yet enforced.

### `Document`
A pure storage record — it tracks what was uploaded, by whom, and its current status. The document only knows who uploaded it. It does not know who will sign it — that responsibility belongs to `SignatureRequest`. This separation keeps the domain clean: a document can exist without ever being sent, and the document itself is not responsible for coordinating the signing workflow.

**Document lifecycle:** `DRAFT` (just uploaded) → `PENDING` (signature request created) → `COMPLETED` (all signers acted).

### `SignatureRequest`
The coordination layer between a document and its signers. When a sender wants a document signed they create a `SignatureRequest` that links the document to a list of signers and defines the signing workflow type. The request starts as `PENDING` and moves to `COMPLETED` when all signers sign, or `DECLINED` when any signer declines.

### `Signer`
Represents one specific person's participation in one specific signing workflow. It carries `signingOrder`, `status`, and `signedAt` — none of which belong on `User` (a user exists independently) or `SignatureRequest` (the request doesn't track per-person state). `Signer` is the right entity to model "this specific person's role and progress in this specific request."

### `SigningProcess`
Records the forensic detail of the actual signing act — the precise timestamp and IP address at the moment of signing. Where `Signer` tracks status, `SigningProcess` captures the evidence. This separation matters for legal non-repudiation: if a signature is disputed, the signing process record provides proof of when and from where the signature was submitted.

### `AuditLog`
An immutable history of everything that happened in the system. Audit logs are never deleted — they are a legal and forensic record that cannot be rewritten. Stored in chronological ascending order so any investigator can reconstruct the exact sequence of events from start to finish.

### `Notification`
In-system messages sent to users when relevant events occur. Notifications are stored in the database and marked read/unread per user. They are generated automatically from the same audit events that power the audit log — when a signing event fires, both an audit record and a notification are created from the same event, keeping the two systems in sync without duplicating the triggering logic.

---

## 🔄 Signing Workflows

### PARALLEL Signing
All signers receive the document simultaneously and can sign in any order. This suits scenarios where signers are of equal authority — for example board members or shareholders approving a resolution. No signing order enforcement applies.

### SEQUENTIAL Signing
Signers must act in a strict order defined by `signingOrder`. This models real-world hierarchical flows — for example a birth certificate being verified by a hospital official first, then counter-signed by a government authority. Signer #2 cannot sign until Signer #1 has completed. If Signer #2 attempts to sign before Signer #1, the system throws: `"Waiting for previous signer to sign first"`.

The order enforcement lives in `SignerWorkflowService.validateSigningOrder()` — designed as a shared service method rather than private logic inside `SigningProcessService` because signing order validation is a business rule belonging to the workflow domain, not an implementation detail of the signing act itself.

---

## 📡 Audit & Notification System

Both the audit log and notification systems listen to the same `AuditEvent` via Spring's `@EventListener`. When any significant action occurs — document sent, signer viewed, signer signed, signer declined, request completed — a single event is published and both services react independently.

**Why event-driven over direct service calls:**
Services publish what happened and move on. The audit and notification concerns are completely decoupled — a new notification type can be added in `NotificationService` without touching the service that triggered the action. This also supports distributed architectures where the signing service and the audit service don't need to be in the same process.

**Audit actions and their notification behaviour:**

| Action | Audit Logged | Notification Sent | Recipient |
|---|---|---|---|
| `DOCUMENT_SENT` | ✅ | ✅ | Signer |
| `SIGNER_VIEWED` | ✅ | ❌ | — |
| `SIGNER_SIGNED` | ✅ | ✅ | Sender |
| `SIGNER_DECLINED` | ✅ | ✅ | Sender |
| `REQUEST_COMPLETED` | ✅ | ✅ | Sender |

`SIGNER_VIEWED` is tracked in the audit log but generates no notification — the sender doesn't need to be notified every time someone opens their pending list. However recording the view event serves a legal purpose: it proves the signer was aware of the document, making "I never saw it" harder to argue in a dispute.

One audit event fires **per signer** on `DOCUMENT_SENT` — not one per request. This gives the sender granular visibility into exactly who was notified and when, rather than a single vague "document sent" entry. In sequential flows this is especially important as each signer's notification is a distinct event in the workflow timeline.

---

## 🚪 Controller Design

Controllers in this system follow a single strict rule: **receive the request, delegate to the service, return the response.** No business logic, no validation logic, no direct repository calls live in any controller. Every controller method is deliberately kept to three lines — get the input, call the service, wrap in `ResponseEntity`. This is a conscious architectural choice: the controller's only job is to be the HTTP entry point, not to decide what happens.

### `AuthController` — `/api/auth`
The only public controller in the system. Both `/register` and `/login` are excluded from JWT authentication in `SecurityConfig` via `.requestMatchers("/api/auth/**").permitAll()`. Every other endpoint in the system requires a valid Bearer token. The controller takes the request DTO, passes it to `AuthService`, and returns the JWT response — it has no awareness of how authentication works internally.

**Design note:** Login and register are deliberately separate endpoint methods even though they both return `AuthResponse`. Their request shapes differ (`RegisterRequest` includes name and role, `LoginRequest` does not) and keeping them separate allows each to evolve independently — for example adding OTP to login without touching the register flow.

### `DocumentController` — `/api/documents`
Handles three operations: upload, fetch by ID, and fetch all for the logged-in user. The upload endpoint takes a `MultipartFile` via `@RequestParam` rather than a request body — this is correct for file uploads which use `multipart/form-data` encoding rather than JSON. The `@AuthenticationPrincipal User user` annotation pulls the authenticated user directly from Spring Security's context, injected automatically by the `JwtAuthFilter` on every authenticated request — the controller never touches the token directly.

**Design note:** `getDocument()` by ID has no ownership check at the controller or service level — any authenticated user who knows a document UUID can fetch it. This is a security gap documented in limitations. The controller correctly delegates the call but the service should enforce that only the uploader can retrieve their document.

### `SignatureRequestController` — `/api/signature-requests`
The sender-facing controller. Creates signature requests and retrieves them. `createSignatureRequest()` passes both the request body and the authenticated user to the service — the service uses the user to enforce document ownership before creating the request. The `getSignatureRequest()` by ID endpoint has no ownership check — any authenticated user can fetch any request by UUID, which is a known limitation.

**Design note:** There is no `DELETE` or `CANCEL` endpoint on signature requests. This is intentional — once a signature request is sent it cannot be silently deleted because the audit trail and signer notifications already exist. The correct resolution is to add a `CANCEL` status transition rather than deletion.

### `SignerWorkflowController` — `/api/signer-workflow`
The signer-facing controller — the endpoint the receiving party uses. It exposes exactly two operations: view pending requests and decline a request. Signing itself lives in a separate controller (`SigningProcessController`) because signing is a distinct act that involves forensic recording, while viewing and declining are workflow state transitions. This separation keeps each controller's responsibility clear.

**Design note:** The controller imports several repository classes directly at the top of the file — `import com.docusign.docusign.repository.*`. These are unused imports left over from development. Controllers should never import repositories directly — all data access must go through the service layer. This is a cleanup item for refactoring.

### `SigningProcessController` — `/api/signing`
The most critical controller — handles the actual signing act. It does one thing the other controllers don't: it extracts the client IP address from the `HttpServletRequest` before passing it to the service. This is the only piece of HTTP-specific data the signing process needs and it is correct for the controller to extract it — the service should not know about HTTP internals. The IP is then passed down as a plain string.

**Design note:** `request.getRemoteAddr()` returns the IP of the direct connection to the server. Behind a load balancer or reverse proxy in a real deployment this would return the proxy's IP, not the actual client IP. The `X-Forwarded-For` header should be read instead for accurate forensic logging. This is documented as a known limitation.

### `AuditLogController` — `/api/audit`
Exposes a single `GET` endpoint that returns the full audit trail for a signature request by its ID. The `@AuthenticationPrincipal User user` is injected but not passed to the service — the service fetches logs without checking if the requester is actually the sender of that request. This means any authenticated user can view the audit trail of any request by knowing its UUID. The user parameter should be passed to the service and validated there.

**Design note:** Audit logs are read-only by design — there is no `POST`, `PUT`, `PATCH`, or `DELETE` endpoint and there never should be. An audit trail that can be modified is not an audit trail. The controller correctly exposes only `GET`.

### `NotificationController` — `/api/notifications`
The most complete controller in the system in terms of HTTP verb usage. `GET /` returns all notifications, `GET /unread` returns unread only, and `PATCH /{id}/read` marks a single notification as read. The `PATCH` verb is the correct choice here — it represents a partial update to a resource (flipping `isRead` from false to true) rather than a full replacement which would be `PUT`. The controller returns `ResponseEntity<Void>` with `204 No Content` for the mark-as-read operation — correct because there is nothing meaningful to return after a state flip.

**Design note:** There is no `DELETE` endpoint on notifications. As discussed in limitations, notifications accumulate indefinitely with no way for users to clear them. A `DELETE /{id}` or `DELETE /read` (bulk clear read notifications) endpoint should be added during refactoring.

---

## 🔧 Service Layer Design

### `AuthService`
Handles registration and login. Passwords are hashed with **BCrypt** before storage — BCrypt automatically generates a unique salt per password, meaning two users with identical passwords have completely different hash values in the database. This protects against rainbow table attacks. Login errors deliberately say `"Invalid password or email"` without specifying which field failed — this prevents attackers from using the endpoint to enumerate valid email addresses in the system.

### `DocumentService`
Handles file upload to local storage and document record creation. Every uploaded file gets a UUID prefix prepended to its original filename to guarantee uniqueness on disk — without it two users uploading `contract.pdf` would collide. Documents are stored in an `uploads/` folder on the local server for the current scope.

### `SignatureRequestService`
The most critical service — it creates signature requests and orchestrates the signer setup. It validates that only the document owner can send it (`document.getUploadedBy().getId().equals(sender.getId())`) — without this check any authenticated user who knows a document UUID could send someone else's private document to arbitrary signers. This is a broken object level authorization vulnerability and the check is essential.

### `SignerWorkflowService`
Handles the signer's side of the workflow — viewing pending requests and declining. The `validateSigningOrder()` method is the enforcement point for sequential signing rules and is shared with `SigningProcessService` as a service-level method because order validation is a business rule, not an implementation detail of the signing act.

### `SigningProcessService`
Records the actual signing act. Runs through authorization, status check, order validation, signer update, process record creation, audit event publication, and completion check in sequence. The IP address is captured from the HTTP layer by the controller and passed down as a plain string — the service handles the forensic recording without needing to know about HTTP internals.

### `AuditLogService`
Listens to `AuditEvent` and persists the record synchronously. If the audit write fails it could affect the signing transaction since they share the same thread. Making it `@Async` would isolate the audit concern so signing succeeds regardless of audit write failures.

### `NotificationService`
Listens to the same `AuditEvent` and generates user-facing messages. Resolves the recipient and message dynamically based on the action type. Notifications are ordered newest-first — the opposite of audit logs which are oldest-first — because notifications are action items where recency matters, while audit logs are investigative timelines read from start to finish.

---

## 🔒 Security Design

**JWT over sessions:** JWT is stateless — the server embeds everything it needs in the token itself and never stores session data. As the user base grows, session storage becomes a bottleneck. JWT scales horizontally without session synchronization between server instances.

**Stateless session management:** `SessionCreationPolicy.STATELESS` means every request is verified independently by its JWT. No server memory is consumed by active sessions regardless of how many users are online simultaneously.

**BCrypt password hashing:** BCrypt is intentionally slow to compute and auto-generates a unique salt per password. This makes brute force attacks expensive and rainbow table attacks impossible even if the database is compromised.

**Global exception handling:** `@RestControllerAdvice` centralizes all error responses. When the same error type can occur across dozens of service methods, handling it centrally means one change propagates everywhere and guarantees a consistent JSON error structure to every client.

**Document ownership check:** Before any signature request is created the system verifies the sender uploaded the document. This prevents broken object level authorization — a class of vulnerability where one user manipulates another user's resources by guessing their IDs.

**Vague login errors:** The login endpoint returns the same error message regardless of whether the email doesn't exist or the password is wrong. This prevents attackers from using the login endpoint to discover which email addresses are registered in the system.

---

## 📁 Project Structure

```
src/
└── main/
    └── java/
        └── com/docusign/docusign/
            ├── config/
            │   ├── ApplicationConfig.java       # BCrypt, AuthenticationProvider, UserDetailsService
            │   ├── JwtAuthFilter.java           # Bearer token validation on every request
            │   ├── JwtService.java              # Token generation, validation, claims extraction
            │   └── SecurityConfig.java          # Security rules, stateless session, filter chain
            │
            ├── controller/
            │   ├── AuthController.java          # POST /register, /login (public endpoints)
            │   ├── DocumentController.java      # POST /upload, GET /{id}, GET /
            │   ├── SignatureRequestController.java  # POST /, GET /{id}, GET /
            │   ├── SignerWorkflowController.java    # GET /pending, POST /{id}/decline
            │   ├── SigningProcessController.java    # POST /{signerId}/sign
            │   ├── AuditLogController.java      # GET /{signatureRequestId}
            │   └── NotificationController.java  # GET /, GET /unread, PATCH /{id}/read
            │
            ├── domain/
            │   ├── User.java                    # Core user entity, implements UserDetails
            │   ├── Document.java                # Uploaded file record
            │   ├── SignatureRequest.java         # Signing workflow coordinator
            │   ├── Signer.java                  # Per-person signing slot
            │   ├── SigningProcess.java           # Forensic signing act record
            │   ├── AuditLog.java                # Immutable event history
            │   ├── Notification.java            # User-facing messages
            │   ├── DocumentStatus.java          # DRAFT, PENDING, COMPLETED
            │   ├── SignatureRequestStatus.java  # PENDING, COMPLETED, DECLINED
            │   ├── SignerStatus.java            # PENDING, SIGNED, DECLINED
            │   ├── SigningType.java             # PARALLEL, SEQUENTIAL
            │   └── AuditAction.java            # DOCUMENT_SENT, SIGNER_VIEWED, SIGNER_SIGNED, SIGNER_DECLINED, REQUEST_COMPLETED
            │
            ├── dto/
            │   ├── request/
            │   │   ├── RegisterRequest.java
            │   │   ├── LoginRequest.java
            │   │   ├── DocumentUploadRequest.java
            │   │   ├── SignatureRequestCreate.java
            │   │   └── SignerRequest.java
            │   └── response/
            │       ├── AuthResponse.java
            │       ├── DocumentResponse.java
            │       ├── SignatureRequestResponse.java
            │       ├── SignerResponse.java
            │       ├── AuditLogResponse.java
            │       ├── NotificationResponse.java
            │       └── SigningProcessResponse.java
            │
            ├── event/
            │   ├── AuditEvent.java              # Event data carrier
            │   └── AuditEventPublisher.java     # Wraps Spring's ApplicationEventPublisher
            │
            ├── repository/
            │   ├── UserRepository.java
            │   ├── DocumentRepository.java
            │   ├── SignatureRequestRepository.java
            │   ├── SignerRepository.java
            │   ├── SigningProcessRepository.java
            │   ├── AuditLogRepository.java
            │   └── NotificationRepository.java
            │
            ├── service/
            │   ├── AuthService.java
            │   ├── DocumentService.java
            │   ├── SignatureRequestService.java
            │   ├── SignerWorkflowService.java
            │   ├── SigningProcessService.java
            │   ├── AuditLogService.java
            │   └── NotificationService.java
            │
            └── DocusignApplication.java
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Clone the repository

```bash
git clone https://github.com/AkshayTeja3/docusign-clone.git
cd docusign-clone
```

### 2. Create the database

```sql
CREATE DATABASE docusign;
```

### 3. Configure application properties

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/docusign
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=your_base64_encoded_secret_key
jwt.expiration=86400000
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

### 5. Test the full signing flow

```
Step 1 — Register a sender     →  POST /api/auth/register  { role: "SENDER" }
Step 2 — Register a signer     →  POST /api/auth/register  { role: "SIGNER" }
Step 3 — Login as sender       →  POST /api/auth/login     (save the JWT token)
Step 4 — Upload a document     →  POST /api/documents/upload
Step 5 — Create a request      →  POST /api/signature-requests
Step 6 — Login as signer       →  POST /api/auth/login
Step 7 — View pending docs     →  GET  /api/signer-workflow/pending
Step 8 — Sign the document     →  POST /api/signing/{signerId}/sign
Step 9 — View the audit trail  →  GET  /api/audit/{signatureRequestId}
```

---

## 🔑 Environment Variables

| Property | Description |
|---|---|
| `spring.datasource.url` | PostgreSQL connection URL |
| `spring.datasource.username` | Database username |
| `spring.datasource.password` | Database password |
| `jwt.secret` | Base64 encoded HMAC-SHA256 signing key |
| `jwt.expiration` | Token expiry in milliseconds (default: 86400000 = 24 hours) |

> ⚠️ The JWT secret and database password are currently stored in `application.properties`. In production both must be moved to environment variables or a secrets manager and never committed to version control.

---

## 📡 API Endpoints

### 🔐 Auth — `/api/auth`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | ❌ |
| POST | `/api/auth/login` | Login and receive JWT token | ❌ |

### 📄 Documents — `/api/documents`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/documents/upload` | Upload a document (multipart/form-data) | ✅ |
| GET | `/api/documents/{id}` | Get document by ID | ✅ |
| GET | `/api/documents` | Get all documents for logged in user | ✅ |

### ✍️ Signature Requests — `/api/signature-requests`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/signature-requests` | Create a signature request | ✅ |
| GET | `/api/signature-requests/{id}` | Get request by ID with all signers | ✅ |
| GET | `/api/signature-requests` | Get all requests sent by logged in user | ✅ |

### 🖊️ Signer Workflow — `/api/signer-workflow`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/signer-workflow/pending` | Get all pending signing assignments | ✅ |
| POST | `/api/signer-workflow/{signerId}/decline` | Decline a signing request | ✅ |

### ✅ Signing — `/api/signing`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/signing/{signerId}/sign` | Sign a document (records IP + timestamp) | ✅ |

### 📋 Audit Log — `/api/audit`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/audit/{signatureRequestId}` | Get full audit trail for a request | ✅ |

### 🔔 Notifications — `/api/notifications`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/notifications` | Get all notifications newest first | ✅ |
| GET | `/api/notifications/unread` | Get unread notifications only | ✅ |
| PATCH | `/api/notifications/{id}/read` | Mark a notification as read | ✅ |

---

## ⚠️ Known Limitations & Planned Improvements

### 🔴 Priority Fixes
- **`createSignatureRequest()` not `@Transactional`** — if the application crashes between saving `SignatureRequest` and saving `Signer` records a broken request with no signers is left in the database. `@Transactional` should be added immediately.
- **`signedAt` incorrectly mapped** — `SignatureRequestResponse.signedAt` is mapped from `createdAt` not from the actual signing completion time. Affects displayed dates shown to users.
- **`IOException` not caught by `GlobalExceptionHandler`** — if a file write fails during upload the client receives an unformatted 500 error. Should be wrapped in a `RuntimeException` in the service or handled explicitly in the global handler.

### 🔒 Security
- **Email verification not enforced** — `isVerified` flag exists on `User` but `isEnabled()` always returns `true`. Users receive a JWT immediately on registration without verifying their email. Should gate token issuance behind email confirmation.
- **JWT secret hardcoded** — currently stored in `application.properties`. Must be moved to environment variables before any production use.
- **No brute force protection** — the login endpoint has no rate limiting or account lockout after repeated failures. An alert or temporary lockout after 5-10 failed attempts should be added.
- **Audit log access not restricted** — any authenticated user can view audit logs for any signature request by knowing its UUID. Access should be restricted to the sender of that specific request.
- **`getDocument()` has no ownership check** — any authenticated user who knows a document UUID can fetch it. The service should verify the requester is the uploader.
- **`getSignatureRequest()` has no ownership check** — same issue as above for signature requests.
- **`filePath` exposed in `DocumentResponse`** — reveals server directory structure to clients. Should be replaced with a signed download URL, especially after migrating to cloud storage.
- **Unused repository imports in `SignerWorkflowController`** — controllers should never import repositories directly. This is a cleanup item that also signals a potential future risk of bypassing the service layer.

### 💾 Data & Storage
- **Local file storage** — documents are stored on the local server filesystem. Not suitable for scale, redundancy, or horizontal deployment. Migration to AWS S3 or equivalent cloud object storage is the planned upgrade path, replacing `filePath` with a cloud URL.
- **No duplicate email check before save** — a second registration with the same email hits the database unique constraint and returns a raw error instead of a clean user-facing message. A `userRepository.existsByEmail()` check should precede the save.
- **Duplicate `signingOrder` not prevented** — two signers can be assigned the same order in a sequential request with no guard at the application or database level. A unique constraint on `(signature_request_id, signing_order)` should be added to the `Signer` table.
- **No guard against empty signers list** — a `SignatureRequest` can be created with no signers attached, leaving it permanently `PENDING` with no one to act on it. A `@NotEmpty` annotation or service-level check is needed.

### 🏗️ Design & Architecture
- **Java-level filtering instead of DB queries** — `getPendingRequests()` loads all signer records for a user then filters for `PENDING` in Java. Should use `findByUserAndStatus(user, SignerStatus.PENDING)` to push filtering to the database and avoid loading unnecessary data.
- **No pagination on any list endpoint** — all list queries return unbounded results. A sender with thousands of documents or a signer with a large assignment history would cause memory and performance issues. `Pageable` support should be added across all list queries.
- **`signingOrder` required for PARALLEL requests** — signing order is mandatory even when it is irrelevant to the workflow. Should be made optional and conditionally validated based on `signingType`.
- **One signer declining kills entire PARALLEL request** — in a parallel flow where 3 of 5 signers have already signed, a single decliner marks the whole request as `DECLINED` immediately. A `PARTIALLY_SIGNED` status and per-signer decline handling would be a fairer design for parallel workflows.
- **`AuditLogService` is synchronous** — if the audit write fails it could affect the signing transaction since they share the same thread. Making it `@Async` isolates the audit concern so signing always succeeds regardless of audit write failures.
- **`signDocument()` does too much** — the method handles authorization, status checking, order validation, signer update, process recording, audit publishing, and completion checking in one long sequence. Should be broken into three focused methods: authorization and status check, order validation and signer update, signing act recording and completion check.
- **`X-Forwarded-For` not used for IP capture** — `request.getRemoteAddr()` returns the proxy IP in real deployments behind a load balancer, not the actual client IP. The `X-Forwarded-For` header should be checked first for accurate forensic IP logging.
- **`DocumentUploadRequest` unused** — the DTO exists but the upload endpoint takes `MultipartFile` directly. Should be expanded with meaningful metadata fields like `description` or `documentType`, or removed.

### 🖥️ API & Response Design
- **`ipAddress` returned in `SigningProcessResponse`** — IP address is forensic data for the audit trail, not display data for the client. Should be removed from the response and kept exclusively in `AuditLog` and `SigningProcess` internally.
- **`senderId` missing from `SignatureRequestResponse`** — only `senderName` is returned as a plain string. The frontend cannot navigate to a sender's profile without their UUID. Both `senderId` and `senderName` should be returned.
- **`signatureRequestId` missing from `NotificationResponse`** — a user who receives "John has signed your document" has to manually navigate to find the relevant request. Adding the ID would enable direct deep-linking from notification to action.
- **`expiresIn` missing from `AuthResponse`** — the frontend has no way to know when the token expires without decoding the JWT. An `expiresIn` field in milliseconds should be returned so the frontend can schedule token refresh.
- **Hardcoded notification messages** — notification text is built as fixed English strings in `NotificationService`. A `notificationType` enum with frontend-rendered templates would support internationalisation and richer dynamic messages.
- **No notification priority or type system** — all notifications are treated equally regardless of urgency. A `type` field with values like `ACTION_REQUIRED`, `FYI`, and `SYSTEM` combined with a `priority` of `HIGH` or `NORMAL` would let the frontend render them differently.
- **No delete or archive for notifications** — notifications accumulate indefinitely with no way for users to clear them. A `DELETE /{id}` or bulk clear endpoint should be added.
- **No signing confirmation retrieval** — `SigningProcessResponse` is returned once on signing but cannot be retrieved again. A `GET /api/signing/{signerId}/confirmation` endpoint should be added so signers can retrieve their signing receipt at any time, similar to a bank transaction record.
- **No `CANCEL` endpoint for signature requests** — there is no way to cancel a request once sent. A `CANCEL` status transition via `PATCH /api/signature-requests/{id}/cancel` would be the correct addition rather than a delete, since the audit trail must be preserved.

---

> Built by Akshay Teja — a student backend project inspired by [DocuSign](https://www.docusign.com/)
