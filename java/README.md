# WhatsApp Fintech Bot MVP — Java / Spring Boot

Java (Spring Boot 3, Java 21) port of the original ASP.NET Core POC. It is a
**behavioural parity** rewrite: same endpoints, same env vars, same port, same
HMAC scheme, same reply logic — so the existing Meta webhook + ngrok setup keeps
working unchanged.

> The original .NET project still lives in [`../src`](../src). This Java backend
> runs independently and is intended to replace it once validated.

---

## Prerequisites

Choose **one** path:

- **Docker only** (no local Java needed): Docker Desktop.
- **Local build**: **JDK 21** and **Maven 3.9+**.
  - This machine currently has only a Java 8 JRE — install a JDK 21
    (e.g. Eclipse Temurin 21) and Maven to build locally.

---

## Configuration

Copy the template and fill in your Meta values (identical keys to the .NET app):

```bash
cp .env.example .env
```

| Variable | Required | Notes |
| --- | --- | --- |
| `PORT` | no (default 3000) | Same port as the .NET MVP |
| `WHATSAPP_TOKEN` | **yes** | Meta access token |
| `PHONE_NUMBER_ID` | **yes** | Meta sender id |
| `VERIFY_TOKEN` | **yes** | Webhook verification token |
| `APP_SECRET` | required outside `dev` | Enables HMAC webhook verification |
| `GRAPH_API_BASE_URL` / `GRAPH_API_VERSION` | no | Defaults `https://graph.facebook.com` / `v20.0` |
| `RATE_LIMIT_PERMIT` / `RATE_LIMIT_WINDOW_SECONDS` | no | Defaults `300` / `60` |

Real environment variables take precedence over `.env` (parity with DotNetEnv).

### Dev vs production profile

The .NET app keys security off `ASPNETCORE_ENVIRONMENT=Development`. Here the
equivalent is the Spring **`dev` profile**:

- **Local demo:** run with `SPRING_PROFILES_ACTIVE=dev`. A blank `APP_SECRET` is
  allowed (signature verification skipped) and the `/debug` endpoint is enabled.
- **Any deployed env:** do **not** set the `dev` profile. `APP_SECRET` becomes
  mandatory (the app fails fast at startup) and `/debug` is not registered. Set
  `SPRING_PROFILES_ACTIVE=prod` to also emit structured JSON logs.

---

## Build & run

### Local (Maven + JDK 21)

```bash
# from the java/ folder
mvn test                 # run unit + integration tests
mvn spring-boot:run      # run locally (set SPRING_PROFILES_ACTIVE=dev for demos)

# or build a runnable jar
mvn -DskipTests package
java -jar target/whatsapp-meta-bot-0.1.0.jar
```

On Windows PowerShell, set the profile with:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; mvn spring-boot:run
```

> No Maven wrapper is committed. To generate one (optional):
> `mvn -N wrapper:wrapper`, then use `./mvnw` / `mvnw.cmd`.

### Docker (no local Java required)

```bash
# from the java/ folder
docker build -t whatsapp-meta-bot-java .
docker run --rm -p 3000:3000 --env-file .env \
  -e SPRING_PROFILES_ACTIVE=dev whatsapp-meta-bot-java
```

Then point ngrok at port 3000 exactly as before.

---

## Endpoints (unchanged contract)

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/webhook` | Meta verification handshake (echoes `hub.challenge`) |
| POST | `/webhook` | Inbound messages (HMAC-verified, fast 200 ack) |
| GET | `/` | Liveness banner |
| GET | `/healthz`, `/readyz` | Health probes |
| POST | `/debug` | Raw payload dump (**dev profile only**) |

---

## .NET → Java mapping

| .NET / ASP.NET Core | Java / Spring Boot |
| --- | --- |
| `Program.cs` | `WhatsAppMetaBotApplication` + `config/*` |
| `WebhookController` | `controller/WebhookController` |
| `WhatsAppService` | `service/WhatsAppServiceImpl` |
| `ReplyGenerator`, `PiiMasker` | `service/ReplyGenerator`, `service/PiiMasker` |
| `WhatsAppOptions` + `IOptions` + validation | `config/WhatsAppProperties` (`@ConfigurationProperties` + `@Validated`) |
| `WebhookSignatureMiddleware` | `security/WebhookSignatureFilter` (+ cached-body wrapper) |
| `WebhookSignature` (HMAC) | `security/WebhookSignature` |
| `OutboundQueue` (Channels) | `service/InMemoryOutboundQueue` (`LinkedBlockingQueue`) |
| `OutboundMessageWorker` (`BackgroundService`) | `service/OutboundMessageWorker` (daemon thread) |
| `IMemoryCache` idempotency | Caffeine cache (`AppConfig`) |
| `HttpClient` + `AddStandardResilienceHandler` | `RestClient` + Resilience4j (`AppConfig`) |
| `AddRateLimiter` (fixed window) | `web/RateLimitFilter` |
| `AddHealthChecks` | `controller/HealthController` |
| `UseExceptionHandler` / ProblemDetails | `web/GlobalExceptionHandler` + `ProblemDetail` |
| xUnit tests | JUnit 5 (`src/test/java`) |

---

## Tests

```bash
mvn test
```

- `ReplyGeneratorTest`, `PiiMaskerTest`, `WebhookSignatureTest` — ported 1:1 from
  the xUnit suite (same inputs/expected outputs = parity harness).
- `WebhookIntegrationTest` — boots the app and exercises the verify handshake,
  the 403 path, the fast 200 ack and the health banner.
