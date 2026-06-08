# WhatsApp Fintech Bot MVP (Meta Cloud API + Node.js)

## Overview

This project is a Proof of Concept (POC) / Minimum Viable Product (MVP) for a two-way WhatsApp business bot using the Meta WhatsApp Cloud API.

The solution demonstrates:

* Receiving WhatsApp messages from real devices
* Processing inbound webhook events
* Sending responses back to users
* Running a webhook-based Node.js backend
* Integrating with Meta Graph API
* Real-device testing via WhatsApp

This MVP intentionally keeps the scope minimal to validate the core messaging architecture before introducing advanced fintech capabilities such as:

* Authentication/PIN verification
* Transaction workflows
* Loan services
* Balance inquiries
* Human handoff
* AI/NLP
* Session management
* Databases
* Event streaming

---

# Architecture

## High-Level Architecture (POC MVP)

```text
+-------------------+
| WhatsApp Mobile   |
| (Real Device)     |
+---------+---------+
          |
          v
+-------------------+
| Meta WhatsApp     |
| Cloud API         |
+---------+---------+
          |
          | Webhook Event
          v
+-------------------+
| ngrok Tunnel      |
| (HTTPS Exposure)  |
+---------+---------+
          |
          v
+-------------------+
| Node.js Backend   |
| Express API       |
+---------+---------+
          |
          | REST API Call
          v
+-------------------+
| Meta Graph API    |
| Send Message API  |
+-------------------+
```

---

# End-to-End Data Flow

## Incoming Message Flow

### Step 1

User sends a WhatsApp message from a real mobile device.

Example:

```text
Hi
```

---

### Step 2

The message reaches Meta WhatsApp Cloud API infrastructure.

Meta processes:

* user identity
* WhatsApp business routing
* webhook subscriptions

---

### Step 3

Meta sends a webhook POST request to the configured backend endpoint.

Example:

```http
POST /webhook
```

Payload contains:

* sender number
* message content
* metadata
* timestamps

---

### Step 4

The Node.js backend:

* validates payload
* extracts the message
* processes business logic
* generates a response

Example:

```text
Hello 👋 Welcome to Fintech MVP Bot
```

---

### Step 5

Backend sends outbound message request to Meta Graph API.

Example:

```http
POST https://graph.facebook.com/v20.0/{PHONE_NUMBER_ID}/messages
```

---

### Step 6

Meta delivers the response back to the user’s WhatsApp application.

---

# Future Production Architecture

As the solution evolves beyond MVP, the architecture can transition into a more production-grade fintech messaging platform.

## Target Production Architecture

```text
+----------------------+
| WhatsApp Users       |
+----------+-----------+
           |
           v
+----------------------+
| Meta WhatsApp API    |
+----------+-----------+
           |
           v
+----------------------+
| API Gateway / WAF    |
+----------+-----------+
           |
           v
+----------------------+
| Kubernetes / GKE     |
| Bot Services         |
+----------+-----------+
           |
    +------+------+
    |             |
    v             v
+--------+   +-------------+
| Redis  |   | PostgreSQL  |
+--------+   +-------------+
    |
    v
+----------------------+
| Kafka / Event Bus    |
+----------+-----------+
           |
           v
+----------------------+
| Core Banking APIs    |
| Loan APIs            |
| Payment Services     |
+----------------------+
```

---

# Tech Stack

## Current MVP Stack

| Component          | Technology              |
| ------------------ | ----------------------- |
| Backend Runtime    | Node.js                 |
| Framework          | Express.js              |
| Messaging Platform | Meta WhatsApp Cloud API |
| Local Tunnel       | ngrok                   |
| HTTP Client        | Axios                   |
| Environment Config | dotenv                  |

---

# Project Structure

```text
whatsapp-meta-bot/
│
├── src/
│   ├── app.js
│   ├── server.js
│   ├── routes/
│   │   └── webhook.js
│   ├── services/
│   │   └── whatsappService.js
│   └── utils/
│       └── logger.js        # structured logger (info/warn/error + timestamps)
│
├── .env                     # local secrets (gitignored, not committed)
├── .env.example             # template of required env vars (safe to commit)
├── package.json
├── Dockerfile
└── README.md
```

---

# Prerequisites

Before running the project, ensure you have the following:

## 1. Meta Developer Account

Required to create and manage the WhatsApp Cloud API application.

Create account:
https://developers.facebook.com/

---

## 2. Meta Business Account

Required for WhatsApp Business integration.

Create business account:
https://business.facebook.com/

---

## 3. WhatsApp Product Enabled

Inside your Meta Developer App:

```text
My Apps
→ Your App
→ Add Product
→ WhatsApp
```

---

## 4. Node.js Installed

Recommended:

* Node.js v20+

Download:
https://nodejs.org/

---

## 5. ngrok Installed

Required to expose local webhook endpoint publicly over HTTPS.

Download:
https://ngrok.com/

---

# Meta Cloud API Setup

## Step 1 — Create Meta App

Go to:

```text
https://developers.facebook.com/
```

Create:

* Business App

---

## Step 2 — Add WhatsApp Product

Inside the app dashboard:

```text
Add Product → WhatsApp
```

---

## Step 3 — Open API Setup

Navigate to:

```text
WhatsApp → API Setup
```

You will obtain:

* Temporary Access Token
* Phone Number ID
* WhatsApp Business Account ID
* Test Phone Number

---

## Step 4 — Add Recipient Number

Inside:

```text
WhatsApp → API Setup
```

Add your real phone number for testing.

Meta will send OTP verification.

---

# Environment Variables

Copy the provided template and fill in your own values:

```bash
cp .env.example .env
```

The `.env` file holds your secrets and must **never** be committed. The
`.env.example` file documents the required keys and is safe to commit.

```env
PORT=3000

WHATSAPP_TOKEN=your_meta_access_token
PHONE_NUMBER_ID=your_phone_number_id

VERIFY_TOKEN=your_custom_verify_token
```

---

# Environment Variable Details

## WHATSAPP_TOKEN

Generated by Meta.

Used for:

* authenticating Graph API requests
* sending WhatsApp messages

Obtained from:

```text
WhatsApp → API Setup
```

---

## PHONE_NUMBER_ID

Generated by Meta.

Represents the WhatsApp sender identity attached to the business account.

Obtained from:

```text
WhatsApp → API Setup
```

---

## VERIFY_TOKEN

Custom token created by the developer.

Used during webhook verification.

Example:

```env
VERIFY_TOKEN=my_secure_token_123
```

The same value MUST also be configured inside Meta webhook settings.

---

# Installation

## Install Dependencies

```bash
npm install
```

---

# Running the Application

## Start Server

```bash
npm start
```

Expected output:

```text
🚀 WhatsApp bot running on port 3000
```

---

# Expose Localhost Using ngrok

## Start ngrok

```bash
ngrok http 3000
```

Example generated URL:

```text
https://abc123.ngrok-free.app
```

---

# Webhook Configuration

## Configure Webhook in Meta

Navigate to:

```text
WhatsApp → Configuration
```

---

## Callback URL

```text
https://abc123.ngrok-free.app/webhook
```

---

## Verify Token

Must match:

```env
VERIFY_TOKEN=my_secure_token_123
```

---

## Subscribe to Fields

Enable:

* messages
* message_status

---

# Testing the MVP

## Step 1

Ensure:

* Node server is running
* ngrok is running
* webhook verified successfully

---

## Step 2

Send message from your real WhatsApp device.

Example:

```text
Hi
```

---

## Step 3

Observe backend logs:

```text
📩 Incoming from 2547XXXXXXXX: Hi
📤 Sent to 2547XXXXXXXX
```

---

## Step 4

Receive WhatsApp response on mobile device.

Example:

```text
Hello 👋 Welcome to Fintech MVP Bot
```

---

# Troubleshooting

## Webhook Verification Fails

Check:

* ngrok is active
* webhook URL is HTTPS
* VERIFY_TOKEN matches exactly
* server is running

---

## Access Denied (#131005)

Usually caused by:

* expired Meta token
* invalid permissions
* incorrect PHONE_NUMBER_ID

Fix:

* generate new temporary token
* restart Node.js server

---

## No Incoming Messages

Check:

* webhook subscription includes "messages"
* recipient number added and verified
* ngrok tunnel active

---

# Security Notes

This MVP is intentionally lightweight and not production hardened.

Before production rollout, implement:

* long-lived access tokens
* HTTPS termination
* HMAC webhook validation
* rate limiting
* audit logging
* secret management
* Redis session storage
* authentication/PIN flows
* observability/monitoring

---

# Recommended Next Enhancements

## Phase 2

* Redis session state
* interactive WhatsApp buttons
* PIN verification
* mock banking APIs

---

## Phase 3

* PostgreSQL
* Kafka
* Kubernetes/GKE
* Prometheus/Grafana
* CI/CD pipelines
* production-grade observability

---

# Production-Grade Enhancements (Roadmap)

The MVP is intentionally minimal. The items below are **deferred** and tracked
here so the team can harden the service toward production without losing
context. They are grouped by concern.

## Security & Compliance

* **HMAC webhook signature validation** — verify Meta's `X-Hub-Signature-256`
  header against the App Secret on every inbound `POST /webhook` to reject
  spoofed payloads.
* **Secret management** — move `WHATSAPP_TOKEN`, `VERIFY_TOKEN`, etc. out of
  `.env` into a managed vault (GCP Secret Manager, AWS Secrets Manager,
  HashiCorp Vault).
* **Long-lived / system-user access tokens** — replace the temporary 24h Meta
  token with a permanent System User token; add automated rotation.
* **Input validation & sanitization** — schema-validate inbound payloads
  (e.g. `zod`) before processing.
* **Rate limiting & abuse protection** — per-sender throttling and a global
  limiter (`express-rate-limit`) behind an API gateway / WAF.

## Reliability & Architecture

* **Async processing** — acknowledge Meta immediately and push messages onto a
  queue (Redis/BullMQ or Kafka) for worker-based handling and retries.
* **Idempotency** — de-duplicate redelivered webhook events using the message
  `id` and a short-lived Redis key.
* **Session/state management** — Redis-backed conversation state to support
  multi-step flows (PIN entry, transactions).
* **Persistence** — PostgreSQL for users, audit trail, and message history.
* **Graceful shutdown** — handle `SIGTERM`/`SIGINT` to drain in-flight work.

## Code Quality & Tooling

* **Structured logging** — replace the lightweight logger with `pino`/`winston`
  (JSON logs, correlation IDs, log levels via env). *(MVP now uses a small
  structured logger in `src/utils/logger.js` as a stepping stone.)*
* **Centralized config module** — validate and fail-fast on missing env vars at
  startup instead of discovering them at request time.
* **Centralized error-handling middleware** in Express.
* **TypeScript** migration for type safety.
* **Testing** — unit tests (Jest/Vitest) for `generateReply` and the webhook
  handler, plus integration tests with mocked Meta APIs.
* **Linting/formatting** — ESLint + Prettier, enforced in CI.

## Observability

* **Health/readiness probes** — dedicated `/healthz` and `/readyz` endpoints.
* **Metrics** — Prometheus counters/histograms (messages in/out, send latency,
  error rates) with Grafana dashboards.
* **Tracing** — OpenTelemetry spans across webhook → worker → Graph API.
* **Alerting** — on send failures, token expiry, and webhook verification
  failures.

## Delivery

* **CI/CD** — automated build, test, scan, and deploy pipelines.
* **Containerization hardening** — non-root user, multi-stage Docker build,
  pinned base image digests, `npm ci` with a committed lockfile.
* **Orchestration** — Kubernetes/GKE with horizontal pod autoscaling.

---

# License

MIT
