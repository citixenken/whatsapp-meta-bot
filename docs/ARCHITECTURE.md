# Production System Architecture — WhatsApp Banking Bot

> **Audience:** Engineering, Platform, and Security teams of a banking partner
> operating WhatsApp messaging across a customer base in the **millions**.
>
> This document describes the **target production architecture**. The current
> repository is an MVP/POC (a single ASP.NET Core service: webhook verify,
> inbound handling, and outbound send). The diagrams below show how that core
> evolves into a horizontally scalable, secure, regulated-grade platform.

---

## 1. Design Goals & Constraints

| Concern | Target |
| --- | --- |
| **Scale** | Millions of customers; bursty inbound + large outbound campaigns |
| **Throughput** | Bounded by Meta Cloud API messaging tier (e.g. 80 → 1,000 msg/s per number; multiple numbers via WABA) |
| **Latency** | Acknowledge Meta webhook in < 5s (Meta retries otherwise); async processing for everything else |
| **Availability** | Multi-AZ, active-active stateless services; target 99.95%+ |
| **Security** | Zero secrets in code; HMAC verification; mTLS to core banking; PII encryption |
| **Compliance** | Audit trail, data residency, consent management, opt-out handling |
| **Reliability** | At-least-once delivery, idempotency, retries with backoff, DLQ |

> **Key architectural principle:** the public webhook path does the *minimum*
> (verify signature → enqueue → 200 OK). All business logic, core-banking
> calls, and outbound sends happen **asynchronously** behind queues.

---

## 2. High-Level Production Architecture

```mermaid
flowchart TB
    subgraph Customers["📱 Customer Estate (millions)"]
        U1["WhatsApp Users"]
    end

    subgraph Meta["Meta WhatsApp Cloud API"]
        MAPI["Cloud API / Graph API"]
        WABA["WhatsApp Business Account<br/>(multiple phone numbers)"]
    end

    subgraph Edge["Edge / Perimeter"]
        DNS["DNS + Global LB"]
        WAF["WAF + DDoS Protection"]
        APIGW["API Gateway<br/>(authN, rate limit, routing)"]
    end

    subgraph Cluster["Kubernetes Cluster (Multi-AZ)"]
        direction TB
        subgraph Ingress["Inbound Path (sync, fast)"]
            WH["Webhook Service<br/>(ASP.NET Core)<br/>HMAC verify → enqueue → 200"]
        end

        subgraph Workers["Async Processing (workers)"]
            MP["Message Processor<br/>(intent, routing, replies)"]
            OB["Outbound Sender<br/>(rate-limited to Meta)"]
            CMP["Campaign Engine<br/>(bulk/template broadcasts)"]
        end

        subgraph Integration["Integration Layer"]
            BANK["Core Banking Adapter<br/>(mTLS, circuit breaker)"]
            AUTH["Auth / PIN / OTP Service"]
            NLP["NLP / AI Service<br/>(intent + handoff)"]
        end
    end

    subgraph Messaging["Event Backbone"]
        Q["Message Queue / Kafka<br/>(inbound, outbound, DLQ)"]
    end

    subgraph Data["Stateful Services"]
        REDIS["Redis<br/>(session, idempotency,<br/>rate-limit counters)"]
        PG[("PostgreSQL<br/>users, audit, history")]
        VAULT["Secrets Vault<br/>(tokens, keys)"]
        BLOB["Object Storage<br/>(media, campaign files)"]
    end

    subgraph CoreBank["🏦 Bank Systems (existing)"]
        CBS["Core Banking System"]
        LOAN["Loan / Lending APIs"]
        PAY["Payments / Transfers"]
        CRM["CRM / Contact Center"]
    end

    subgraph Obs["Observability & Ops"]
        LOG["Centralized Logging"]
        MET["Metrics (Prometheus/Grafana)"]
        TRACE["Tracing (OpenTelemetry)"]
        ALERT["Alerting / On-call"]
    end

    U1 <--> MAPI
    MAPI --- WABA
    MAPI -->|"Webhook POST (inbound)"| DNS
    DNS --> WAF --> APIGW --> WH

    WH -->|"signature OK"| Q
    WH -.->|"verify token"| VAULT

    Q --> MP
    MP --> REDIS
    MP --> BANK
    MP --> AUTH
    MP --> NLP
    MP -->|"reply"| Q
    Q --> OB
    CMP -->|"templated messages"| Q
    OB -->|"Send Message API<br/>(rate limited)"| MAPI
    OB -.->|"token"| VAULT

    BANK <-->|mTLS| CBS
    BANK <--> LOAN
    BANK <--> PAY
    AUTH <--> CRM

    MP --> PG
    OB --> PG
    CMP --> PG
    CMP --> BLOB
    MP --> BLOB

    WH -.-> LOG & MET & TRACE
    MP -.-> LOG & MET & TRACE
    OB -.-> LOG & MET & TRACE
    MET --> ALERT
```

---

## 3. Inbound Message Flow (Customer → Bank)

The hot path is intentionally thin so Meta always gets a fast `200 OK`.

```mermaid
sequenceDiagram
    autonumber
    participant C as Customer (WhatsApp)
    participant M as Meta Cloud API
    participant W as Webhook Service
    participant V as Vault
    participant Q as Queue (Kafka)
    participant P as Message Processor
    participant R as Redis (session/idemp.)
    participant B as Core Banking Adapter
    participant O as Outbound Sender

    C->>M: Send message ("Balance")
    M->>W: POST /webhook (X-Hub-Signature-256)
    W->>V: Fetch App Secret (cached)
    W->>W: Verify HMAC signature
    alt signature invalid
        W-->>M: 403 Forbidden
    else valid
        W->>R: Check message id (idempotency)
        W->>Q: Publish inbound event
        W-->>M: 200 OK (<5s)
    end

    Q->>P: Consume inbound event
    P->>R: Load/refresh conversation state
    P->>B: Authenticated balance request (mTLS)
    B-->>P: Balance result
    P->>Q: Publish outbound reply
    Q->>O: Consume outbound event
    O->>M: POST /{phone_number_id}/messages (rate limited)
    M->>C: Deliver reply
    O->>R: Update delivery state
```

---

## 4. Outbound Broadcast / Campaign Flow (Bank → Millions)

Bulk messaging to millions must respect Meta template rules, per-number
throughput tiers, opt-out/consent, and back-pressure.

```mermaid
flowchart LR
    subgraph Trigger["Campaign Trigger"]
        OPS["Ops / Marketing Console"]
        SCHED["Scheduler / Batch Job"]
        EVT["Event-driven<br/>(e.g. salary credit alert)"]
    end

    subgraph Engine["Campaign Engine"]
        SEG["Audience Segmentation<br/>+ Consent / Opt-out filter"]
        TPL["Approved Template Selector"]
        THR["Throttler / Token Bucket<br/>(per phone number tier)"]
    end

    subgraph Fanout["Fan-out"]
        Q2["Outbound Queue<br/>(partitioned)"]
        OBW["Outbound Sender Workers<br/>(autoscaled)"]
    end

    OPS --> SEG
    SCHED --> SEG
    EVT --> SEG
    SEG --> TPL --> THR --> Q2
    Q2 --> OBW
    OBW -->|"rate-limited sends"| META["Meta Cloud API<br/>(multiple WABA numbers)"]
    META -->|"delivery + read status<br/>webhooks"| STATUS["Status Consumer"]
    STATUS --> DB[("Campaign Metrics<br/>delivered / read / failed")]
    OBW -->|"failures"| DLQ["Dead Letter Queue<br/>→ retry w/ backoff"]
```

**Scaling levers for millions of recipients:**

- **Multiple phone numbers** under one or more WABAs to multiply aggregate throughput.
- **Queue partitioning** by recipient hash for parallel, ordered-per-user delivery.
- **Token-bucket throttling** per number to stay within Meta's messaging tier (auto-upgrades as quality rating allows).
- **Horizontal autoscaling** of sender workers driven by queue depth.
- **Back-pressure + DLQ** so transient Meta/network failures retry without data loss.
- **Consent & opt-out enforcement** applied *before* fan-out (regulatory requirement).

---

## 5. Deployment Topology (Multi-AZ / DR)

```mermaid
flowchart TB
    subgraph Region["Primary Region"]
        GLB["Global Load Balancer"]
        subgraph AZ1["Availability Zone A"]
            N1["Webhook pods"]
            P1["Processor pods"]
            O1["Sender pods"]
        end
        subgraph AZ2["Availability Zone B"]
            N2["Webhook pods"]
            P2["Processor pods"]
            O2["Sender pods"]
        end
        KQ["Kafka (3+ brokers, replicated)"]
        RDS["Redis (HA, replica)"]
        DBP[("PostgreSQL Primary")]
        DBR[("PostgreSQL Replica")]
    end

    subgraph DR["DR Region (warm standby)"]
        DBDR[("PostgreSQL<br/>async replication")]
        KQDR["Kafka MirrorMaker"]
    end

    GLB --> N1 & N2
    N1 & N2 --> KQ
    KQ --> P1 & P2
    P1 & P2 --> O1 & O2
    P1 & P2 --> RDS
    P1 & P2 --> DBP
    DBP --> DBR
    DBP -.->|async| DBDR
    KQ -.->|mirror| KQDR
```

---

## 6. Security & Compliance Architecture

```mermaid
flowchart TB
    subgraph Perimeter["Perimeter Controls"]
        WAF2["WAF / DDoS"]
        MTLS_IN["TLS termination"]
        HMAC["HMAC X-Hub-Signature-256<br/>verification"]
    end

    subgraph Identity["Secrets & Identity"]
        VAULT2["Secrets Vault<br/>(WHATSAPP_TOKEN, App Secret,<br/>DB creds)"]
        ROT["Automated token rotation<br/>(System User tokens)"]
        WID["Workload Identity<br/>(no static creds in pods)"]
    end

    subgraph Boundary["Bank Integration Boundary"]
        MTLS_OUT["mTLS to Core Banking"]
        CB["Circuit Breaker + Timeouts"]
        IPALLOW["IP allow-listing"]
    end

    subgraph DataSec["Data Protection"]
        ENC["Encryption at rest (PII)"]
        TLSALL["TLS in transit (all hops)"]
        MASK["Log masking / PII redaction"]
        AUDIT["Immutable audit trail"]
        CONSENT["Consent & opt-out registry"]
    end

    Internet["Internet"] --> WAF2 --> MTLS_IN --> HMAC --> App["Webhook Service"]
    App --> VAULT2
    VAULT2 --> ROT
    App --> WID
    App --> MTLS_OUT --> CB --> IPALLOW --> Bank["Core Banking"]
    App --> ENC & TLSALL & MASK & AUDIT & CONSENT
```

**Banking-grade safeguards:**

- **HMAC signature validation** on every inbound webhook (reject spoofed payloads).
- **Secrets in a vault**, never in `.env`/images; **workload identity** for pod auth.
- **mTLS + IP allow-listing + circuit breakers** on all core-banking calls.
- **PII encryption** at rest, **log redaction**, and an **immutable audit trail** for every customer interaction.
- **Consent / opt-out registry** enforced before any outbound message.
- **Long-lived System User tokens** with automated rotation (no 24h temp tokens).

---

## 7. Mapping: MVP (this repo) → Production

| MVP Component (current) | Production Evolution |
| --- | --- |
| `WebhookController` (verify + handle inline) | Thin **Webhook Service**: HMAC verify → enqueue → 200 |
| `WhatsAppService.HandleIncomingMessage` | **Message Processor** workers consuming from Kafka |
| `WhatsAppService.GenerateReply` (if/else) | **NLP/Intent service** + rules engine + human handoff |
| `WhatsAppService.SendWhatsAppMessage` | **Outbound Sender** workers with token-bucket rate limiting |
| `.env` config | **Secrets Vault** + validated config + rotation |
| In-memory / none | **Redis** (session, idempotency) + **PostgreSQL** (audit, history) |
| Single container | **Kubernetes**, multi-AZ, autoscaled, with DR region |
| `ILogger` console | **Structured logs + metrics + tracing + alerting** |
| Direct synchronous send | **Kafka-backed async** with retries + DLQ |
| n/a | **Campaign Engine** for bulk/template broadcasts to millions |

---

## 8. Throughput Planning Notes

- Meta enforces **per-phone-number messaging limits** tied to quality rating
  (commonly 1K → 10K → 100K → unlimited business-initiated conversations/24h,
  and an API call rate of ~80 msg/s, upgradable). Plan capacity by
  **(numbers × per-number rate)** and partition queues accordingly.
- For a campaign to **N million** recipients, estimate wall-clock time as
  `N / (numbers × msg_per_sec)` and add headroom for retries/back-pressure.
- Keep **business-initiated** (template) vs **user-initiated** (session)
  messaging separate — they have different rules, costs, and rate envelopes.
```mermaid
flowchart LR
    A["1 number ~ 80 msg/s"] --> B["10 numbers ~ 800 msg/s"]
    B --> C["1M msgs ≈ ~21 min<br/>(at 800 msg/s, ideal)"]
    C --> D["+ retries / quality / back-pressure<br/>→ plan generous headroom"]
```
