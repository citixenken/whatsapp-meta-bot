# Production Readiness Tracker — WhatsApp Meta Bot

> **Purpose:** Track the gaps between the current **MVP/POC** and a
> production-grade WhatsApp banking bot. This file is the single source of
> truth for hardening work. Update the **Status** column as items progress.
>
> **Scope reviewed:** `src/` (Program, WebhookController, WhatsAppService,
> Models), `Dockerfile`, `appsettings*.json`, `.env.example`.
>
> Last reviewed: 2026-06-22

## How to use this document

- **Status legend:** `❌ Not started` · `🟡 In progress` · `✅ Done` · `➖ N/A`
- **Priority legend:** `P0` = blocker for any production traffic · `P1` =
  required before general availability · `P2` = hardening / scale / polish.
- Keep item IDs stable (e.g. `SEC-01`) so they can be referenced in tickets/PRs.

## Summary

| Category | P0 | P1 | P2 | Total |
| --- | --- | --- | --- | --- |
| Test data / placeholders | 5 | 3 | 2 | 10 |
| Security & compliance | 4 | 3 | 1 | 8 |
| Reliability & architecture | 2 | 5 | 1 | 8 |
| Observability | 0 | 3 | 2 | 5 |
| Config & code quality | 1 | 4 | 1 | 6 |
| Delivery & ops | 0 | 2 | 4 | 6 |
| **Total** | **12** | **20** | **11** | **43** |

---

## A. Test Data, Mocks & Placeholders (replace before production)

These are the spots where the MVP ships **stub / test / placeholder** behavior
instead of real functionality. Each must be replaced with a real implementation
or removed before go-live.

| ID | What is "test data" today | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| TD-01 | Hardcoded canned replies (`"Hello 👋 Welcome to Fintech MVP Bot"`, `"(MVP mode)"`) via if/else keyword matching | [`WhatsAppService.GenerateReply`](../src/services/WhatsAppService.cs#L95-L115) | Replace with real intent routing (NLP/rules engine) + dialog flows; remove "MVP mode" copy | P1 | ❌ |
| TD-02 | **Balance** is a stub: `"Your balance feature is coming soon 🚧"` | [WhatsAppService.cs#L104-L107](../src/services/WhatsAppService.cs#L104-L107) | Integrate Core Banking balance API (authenticated, mTLS) | P1 | ❌ |
| TD-03 | **Loan** is a stub: `"Loan services will be available in next phase 📊"` | [WhatsAppService.cs#L109-L112](../src/services/WhatsAppService.cs#L109-L112) | Integrate Loan/Lending API | P2 | ❌ |
| TD-04 | Only text messages handled; everything else gets `"Only text messages are supported in this MVP."` | [WhatsAppService.cs#L69-L75](../src/services/WhatsAppService.cs#L69-L75) | Decide supported types (interactive buttons, media, location) and handle/ignore deliberately | P2 | ❌ |
| TD-05 | **Temporary 24h Meta access token** used as `WHATSAPP_TOKEN` (from API Setup screen) | [`.env.example`](../.env.example), [WhatsAppService.cs#L121](../src/services/WhatsAppService.cs#L121) | Replace with a permanent **System User** token + automated rotation, sourced from a vault | P0 | ❌ |
| TD-06 | **Test *sender* number** — the Meta-provided sandbox "From" number on the API Setup screen (its `PHONE_NUMBER_ID` is what the app sends with). It is bound to test mode, has no approved display name, and is not a real business line. | Meta config / `PHONE_NUMBER_ID` | Register & verify a real **business** phone number on the production WABA (display-name approval + Business Verification), then switch the app to **Live mode** | P0 | ❌ |
| TD-07 | `/debug` endpoint dumps full raw webhook payloads (a Meta-testing aid) | [`Program.cs` /debug](../src/Program.cs#L29-L53) | Remove (or gate behind auth + non-prod env); never log raw PII payloads in prod | P0 | ✅ |
| TD-08 | **ngrok tunnel + port 3000 + plain HTTP** dev exposure | [Program.cs#L17-L18](../src/Program.cs#L17-L18), `README` | Replace with managed HTTPS ingress / API gateway behind a real domain + TLS | P0 | ❌ |
| TD-09 | `.env` file as the config/secret source | [Program.cs#L7](../src/Program.cs#L7), `.env.example` | Move secrets to a managed vault; `.env` only for local dev | P1 | ❌ |
| TD-10 | **Test *recipient* allow-list** — in test mode the bot can only message up to **5** phone numbers manually added & OTP-verified in API Setup (see README "Add Recipient Number"). Real customers cannot be reached. | Meta API Setup (recipient list) | Go **Live** (verified business number + Business Verification) so any **opted-in** customer can be messaged; drop the 5-number allow-list; use **approved message templates** for business-initiated sends within the number's messaging tier | P0 | ❌ |

---

## B. Security & Compliance

| ID | Issue | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| SEC-01 | **No HMAC signature verification** on inbound webhook — any caller can POST fake messages and trigger (paid) outbound sends | [`WebhookController.Receive`](../src/Controllers/WebhookController.cs#L48-L62) | Verify Meta `X-Hub-Signature-256` against the App Secret on every `POST /webhook`; reject mismatches with 403 | P0 | ✅ |
| SEC-02 | **Secrets in plaintext** (`.env` / env vars), no rotation | [Program.cs#L7](../src/Program.cs#L7) | Managed secrets vault (GCP Secret Manager / AWS Secrets Manager / HashiCorp Vault) + workload identity | P0 | ❌ |
| SEC-03 | **PII logged in cleartext** — full sender number + message body | [WhatsAppService.cs#L79](../src/services/WhatsAppService.cs#L79) | Mask/redact PII in logs; structured logging with redaction policy (banking compliance) | P0 | ✅ |
| SEC-04 | **No rate limiting / abuse protection** — outbound sends cost money and can be weaponized | app-wide | Per-sender throttling + global limiter (ASP.NET Core rate limiting) behind WAF/API gateway | P0 | ❌ |
| SEC-05 | **Verify-token check is not constant-time** and **fails open if `VERIFY_TOKEN` is unset** (`null == null`) | [WhatsAppService.cs#L30-L36](../src/services/WhatsAppService.cs#L30-L36) | Fail-fast if token unconfigured; use fixed-time comparison | P1 | ✅ |
| SEC-06 | **No inbound payload validation/sanitization** — model bound directly from body | [WebhookController.cs#L49](../src/Controllers/WebhookController.cs#L49) | Schema validation (FluentValidation/DataAnnotations) + size limits before processing | P1 | ❌ |
| SEC-07 | **No HTTPS/HSTS enforcement** in-app; relies entirely on the tunnel/proxy | [Program.cs#L18](../src/Program.cs#L18) | Enforce HTTPS redirection/HSTS at edge; treat app as behind TLS-terminating proxy explicitly | P1 | ❌ |
| SEC-08 | **No consent / opt-out registry** (regulatory requirement for outbound) | n/a | Consent + opt-out enforcement before any business-initiated message | P2 | ❌ |

---

## C. Reliability & Architecture

| ID | Issue | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| REL-01 | **Fire-and-forget sends** (`_ = SafeSendAsync(...)`) — unobserved tasks, lost on shutdown, no retry | [WhatsAppService.cs#L71-L73](../src/services/WhatsAppService.cs#L71-L73), [#L84](../src/services/WhatsAppService.cs#L84) | Acknowledge Meta immediately, enqueue to a queue (Redis/Kafka) and process via a worker (`BackgroundService`) | P0 | ✅ |
| REL-02 | **No idempotency** — Meta retries webhooks → duplicate replies | [WhatsAppService.HandleIncomingMessageAsync](../src/services/WhatsAppService.cs#L42-L90) | De-duplicate on message `id` using a short-lived Redis key | P0 | ✅ |
| REL-03 | **HttpClient has no resilience** — default 100s timeout, no retry/backoff/circuit breaker | [Program.cs#L21](../src/Program.cs#L21), [WhatsAppService.SendWhatsAppMessageAsync](../src/services/WhatsAppService.cs#L118) | Add Polly: timeout, retry w/ backoff, circuit breaker; pass `CancellationToken` | P1 | ✅ |
| REL-04 | **No retry / DLQ for failed sends** — a failed Meta send is only logged | [WhatsAppService.cs#L139-L145](../src/services/WhatsAppService.cs#L139-L145) | Retry with backoff; dead-letter queue for poison messages | P1 | ❌ |
| REL-05 | **No persistence** — no users, audit trail, or message history | n/a | PostgreSQL for users/audit/history; immutable audit log for every interaction | P1 | ❌ |
| REL-06 | **No session/state management** — cannot do multi-step flows (PIN, transactions) | n/a | Redis-backed conversation state | P1 | ❌ |
| REL-07 | **No graceful shutdown** — in-flight fire-and-forget work is killed on SIGTERM | [Program.cs](../src/Program.cs) | Handle `IHostApplicationLifetime` / drain workers before exit | P1 | ✅ |
| REL-08 | **Hardcoded Graph API version** `v20.0` will age | [WhatsAppService.cs#L123](../src/services/WhatsAppService.cs#L123) | Make Graph API base URL + version configurable | P2 | ✅ |

---

## D. Observability

| ID | Issue | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| OBS-01 | **No real health/readiness probes** — `/` just returns a string | [Program.cs#L26](../src/Program.cs#L26) | Add `/healthz` (liveness) + `/readyz` (readiness incl. dependency checks) | P1 | ✅ |
| OBS-02 | **Console logging only, unstructured**, no correlation IDs | app-wide (`ILogger`) | Structured JSON logging (Serilog), correlation/trace IDs, configurable levels | P1 | 🟡 |
| OBS-03 | **No alerting** on send failures, token expiry, or webhook verification failures | n/a | Alert rules wired to on-call | P1 | ❌ |
| OBS-04 | **No metrics** (messages in/out, send latency, error rates) | n/a | Prometheus counters/histograms + Grafana dashboards | P2 | ❌ |
| OBS-05 | **No distributed tracing** across webhook → worker → Graph API | n/a | OpenTelemetry spans | P2 | ❌ |

---

## E. Config & Code Quality

| ID | Issue | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| CFG-01 | **No fail-fast config validation** — missing `PHONE_NUMBER_ID`/`WHATSAPP_TOKEN` surface as a malformed URL / 401 at request time | [WhatsAppService.cs#L120-L123](../src/services/WhatsAppService.cs#L120-L123) | Validate required settings at startup via `IOptions` validation; fail fast | P0 | ✅ |
| CFG-02 | **No automated tests** (unit or integration) | repo | xUnit/NUnit for `GenerateReply` + webhook handler; integration tests with mocked Meta API | P1 | ✅ |
| CFG-03 | **Controller swallows all exceptions and always returns 200** — errors are invisible downstream | [WebhookController.cs#L51-L58](../src/Controllers/WebhookController.cs#L51-L58) | Keep fast 200 to Meta, but route failures to queue/DLQ + centralized exception middleware | P1 | 🟡 |
| CFG-04 | **`message_status` events subscribed but silently ignored** (only `messages` handled) | [WhatsAppService.cs#L52-L57](../src/services/WhatsAppService.cs#L52-L57) | Consume delivery/read/failed statuses to track delivery + quality rating | P1 | ✅ |
| CFG-05 | **Misleading `async`** — `HandleIncomingMessageAsync` only `await Task.CompletedTask`; no true async work | [WhatsAppService.cs#L89](../src/services/WhatsAppService.cs#L89) | Make processing genuinely async via the worker/queue path (see REL-01) | P1 | ✅ |
| CFG-06 | **Emoji in log messages** can break some log aggregators/encodings | app-wide | Use plain ASCII log messages; move semantics to structured fields | P2 | ✅ |

---

## F. Delivery & Ops

| ID | Issue | Where | Required change | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| DEL-01 | **Container runs as root** | [`Dockerfile`](../Dockerfile) | Add and switch to a non-root user | P1 | ✅ |
| DEL-02 | **No CI/CD pipeline** (build, test, scan, deploy) | repo | Add pipeline with build + test + image/dependency scanning + deploy gates | P1 | 🟡 |
| DEL-03 | **Base image pinned by mutable tag** (`:10.0`) | [Dockerfile#L2](../Dockerfile#L2), [#L11](../Dockerfile#L11) | Pin by digest for reproducible/secure builds | P2 | ❌ |
| DEL-04 | **No committed lockfile** (`packages.lock.json`) for deterministic restore | [`WhatsAppMetaBot.csproj`](../src/WhatsAppMetaBot.csproj) | Enable `RestorePackagesWithLockFile` and commit the lock | P2 | ✅ |
| DEL-05 | **No `.dockerignore`** — local `bin/`/`obj/` get copied into the build context | repo root | Add `.dockerignore` excluding `bin/`, `obj/`, `.env`, `.git` | P2 | ✅ |
| DEL-06 | **No orchestration/autoscaling** definition | n/a | Kubernetes/GKE manifests with HPA, multi-AZ, DR (per `docs/ARCHITECTURE.md`) | P2 | ❌ |

---

## G. Can these be done while staying in MVP/POC mode?

Yes — most hardening items are **pure code or single-container config** and do
**not** require leaving POC scope (no Kafka / Redis / PostgreSQL / Kubernetes /
vault / WAF). They are grouped below by the infrastructure (if any) they need.

### ✅ Fully MVP-safe — pure code / single-container (do now)

`SEC-01` HMAC verify · `SEC-03` PII log redaction · `SEC-04` built-in rate
limiter · `SEC-05` constant-time + fail-fast token · `SEC-06` payload
validation · `REL-03` HttpClient timeout/retry/circuit-breaker (Polly) ·
`REL-08` configurable Graph API version · `OBS-01` `/healthz` + `/readyz`
(`AddHealthChecks`) · `OBS-02` structured JSON logs + correlation id ·
`CFG-01` fail-fast config validation (`ValidateOnStart`) · `CFG-02` xUnit
tests · `CFG-03` exception-handling middleware · `CFG-04` handle
`message_status` · `CFG-06` ASCII logs · `DEL-01` non-root image · `DEL-03`
pin base image digest · `DEL-04` lockfile · `DEL-05` `.dockerignore` ·
`DEL-02` lightweight CI (build + test).

### 🟡 MVP-safe with an in-process substitute (single-instance only)

These reproduce the production design without external infra — fine for POC,
but state lives in memory (lost on restart, not shared across instances):

* `REL-01` async processing → `System.Threading.Channels` + a `BackgroundService`
  (instead of Kafka/Redis). Also fixes `CFG-05` (real async) and gives
  `REL-07` graceful shutdown for free.
* `REL-02` idempotency → `IMemoryCache` keyed on message `id` with a short TTL
  (instead of Redis).
* `REL-06` session/state → `IMemoryCache` (only if multi-step flows are added).

### ❌ Needs real infrastructure — defer beyond MVP

`SEC-02` secrets vault · `SEC-07` edge HTTPS/HSTS (ngrok already provides TLS in
POC) · `SEC-08` consent registry · `REL-04` DLQ · `REL-05` PostgreSQL ·
`OBS-03` alerting · `OBS-04` / `OBS-05` metrics & tracing backends · `DEL-06`
Kubernetes. *(Local stand-ins exist — e.g. .NET user-secrets for `SEC-02`,
SQLite for `REL-05` — but adopting them starts to blur the POC boundary.)*

**Recommended first batch (high value, low risk, stays POC):** `SEC-01`,
`CFG-01`, `SEC-03`, `REL-03`, `OBS-01`, `REL-01` (+ `REL-07` / `CFG-05`),
`SEC-05`, and the Docker trio `DEL-01` / `DEL-03` / `DEL-05`.

---

## H. Implemented in the MVP-safe iteration (changelog)

The following were delivered without leaving POC scope (no new external infra).
All changes build clean, 16 unit tests pass, and the app was smoke-tested
(health probes, webhook verification, and HMAC accept/reject paths).

**Done (✅):**

* `SEC-01` HMAC verification via `WebhookSignatureMiddleware` — **opt-in**: only
  enforced when `APP_SECRET` is set, so existing POC setups keep working.
* `SEC-03` PII redaction (`PiiMasker`) — phone numbers masked, message bodies no
  longer logged.
* `SEC-05` constant-time verify-token comparison (`CryptographicOperations.FixedTimeEquals`).
* `REL-01` / `REL-07` / `CFG-05` in-process outbound pipeline
  (`IOutboundQueue` + `OutboundMessageWorker` `BackgroundService`) replacing
  fire-and-forget; drains on shutdown.
* `REL-02` idempotency via `IMemoryCache` (10-min TTL on message id).
* `REL-03` HttpClient resilience via `AddStandardResilienceHandler()`
  (timeout + retry + circuit breaker) and a `CancellationToken` on sends.
* `REL-08` configurable Graph API base URL + version (`WhatsAppOptions`).
* `OBS-01` `/healthz` + `/readyz` health endpoints.
* `CFG-01` fail-fast options validation (`ValidateDataAnnotations().ValidateOnStart()`).
* `CFG-02` xUnit test project (`ReplyGenerator`, `WebhookSignature`, `PiiMasker`).
* `CFG-04` `message_status` callbacks are now consumed/logged.
* `CFG-06` ASCII log messages (emoji kept only in user-facing replies).
* `TD-07` `/debug` endpoint restricted to the Development environment.
* `DEL-01` container runs as the non-root `app` user.
* `DEL-04` `packages.lock.json` committed + Docker `--locked-mode` restore.
* `DEL-05` `.dockerignore` added.

**Partial (🟡) — remainder needs infra:**

* `OBS-02` structured **JSON logging** enabled outside Development; correlation
  IDs / Serilog sink still pending.
* `CFG-03` centralized exception middleware (`UseExceptionHandler` + ProblemDetails)
  added; routing failed sends to a **DLQ** needs a broker.
* `DEL-02` CI workflow runs **build + test**; image/dependency scanning + deploy
  gates still to add.

**Notes / deviations:**

* `REL-01`/`REL-02` use **in-process** substitutes (Channels / `IMemoryCache`).
  These are single-instance and non-durable by design — swap for Kafka/Redis at
  scale (tracked under section G and the production architecture).
* New config keys: optional `APP_SECRET`, `GRAPH_API_VERSION`, `GRAPH_API_BASE_URL`
  (see `.env.example`). Existing `VERIFY_TOKEN` / `WHATSAPP_TOKEN` /
  `PHONE_NUMBER_ID` are now **required at startup** (clearer failure than before).

**Deliberately deferred (would risk MVP/POC operations or need infra):**
`SEC-04` rate limiting (could drop Meta webhook deliveries without a gateway),
`SEC-06` heavy schema validation, `DEL-03` digest pinning (needs resolved digests),
plus all `❌` infra items in section G.

---

## Suggested sequencing

1. **P0 security & correctness first:** SEC-01 (HMAC), SEC-02/TD-05 (token+vault),
   SEC-03 (PII), SEC-04 (rate limit), TD-07 (`/debug`), CFG-01 (config validation),
   REL-01/REL-02 (queue + idempotency), TD-06/TD-10 (real business number + Live
   mode, no recipient allow-list), TD-08 (HTTPS ingress).
2. **P1 to reach GA:** real business logic (TD-01..TD-03), persistence/session
   (REL-05/06), resilience (REL-03/04), observability (OBS-01..03), tests (CFG-02),
   non-root container (DEL-01), CI/CD (DEL-02).
3. **P2 hardening & scale:** tracing/metrics, image pinning, lockfile,
   `.dockerignore`, orchestration, consent registry.

> See [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) for the target production
> architecture these items move toward.
