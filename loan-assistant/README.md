# GenAI Loan Processing Assistant

A full-stack Spring Boot application that accepts loan applications, runs them
through **configurable, auditable lending rules**, uses **Claude (Anthropic API)**
as an advisory assistant, and communicates the final decision to the customer.

> **Decision control (by design):** the AI never approves or declines a loan.
> The `AiAnalysis` record has no field that could express a decision, and the
> status of an application is set only by the `RulesEngine` — or by a loan
> officer when rules flag the file for manual review. AI is used for document
> extraction, summarization, gap/risk flagging, and for drafting the
> explanation and customer message *after* a decision already exists.

## Tech stack

Java 21 · Spring Boot 3.5 · Spring MVC · Spring Data JPA · Spring Security ·
Thymeleaf + Bootstrap 5 · MySQL 8 · Flyway · Maven · Lombok · JUnit 5 + Mockito
· Testcontainers · Docker / Docker Compose

## Architecture

```
Customer → POST /apply ─→ LoanApplicationService.submit()
                            │ status = SUBMITTED
                            ▼
                ApplicationProcessingService.process()
                  1. ClaudeAiAnalysisService (advisory only)
                     → AiAnalysisResult (summary, missing info,
                       inconsistencies, risks, extracted fields)
                  2. RulesEngine → per-rule RuleEvaluation rows (audit)
                     → all PASS = APPROVED
                     → any FAIL = REJECTED
                     → any REVIEW = PENDING_MANUAL_REVIEW
                  3a. Terminal → DecisionRecord(RULES_ENGINE)
                     → Claude drafts explanation + customer message
                     → CustomerNotification
                  3b. Review → officer queue
Officer → /officer/queue → /officer/applications/{id}
                sees AI advisory + rule audit trail
                POST decision + rationale
                  → DecisionRecord(LOAN_OFFICER, decidedBy=username)
                  → Claude drafts explanation → CustomerNotification
```

## Fair-lending guardrails

The application form and data model deliberately collect **no** protected
attributes — no race, religion, gender, disability, marital status, age or
national origin. Lending rules evaluate only declared financial data, and the
Claude system prompt explicitly forbids using or inferring such attributes.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/loandb` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `loanuser` / `loanpass` | DB credentials |
| `ANTHROPIC_API_KEY` | *(empty)* | Claude key — app falls back to a local deterministic advisory when unset |
| `ANTHROPIC_MODEL` | `claude-sonnet-4-5-20250929` | Model ID |
| `UPLOAD_DIR` | `./uploads` | Document storage |
| `SERVER_PORT` | `8080` | HTTP port |

Lending-policy thresholds are externalized in `application.yml` under
`lending.rules.*` (credit-score bands, DTI caps, income floors, amount/term
ranges, employment-history minimum). Each `rule_evaluation` row persists the
threshold applied at decision time, so policy changes never rewrite history.

## Run

### With Docker (recommended)

```bash
docker compose up --build
# open http://localhost:8080
```

### Locally with Maven

```bash
# 1. Start MySQL 8 (or reuse the db service)
docker compose up db

# 2. Run the app
mvn spring-boot:run
```

No Docker at all? Run against in-memory H2:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Demo accounts (seeded on first start)

| Username | Password | Role |
|---|---|---|
| `customer` | `customer123` | CUSTOMER |
| `officer` | `officer123` | OFFICER |
| `admin` | `admin123` | ADMIN |

## Tests

```bash
mvn test        # unit tests (rules engine, decision-control boundary, officer flow)
mvn verify      # + Testcontainers MySQL integration test (needs Docker)
```

## API surface (web)

| Route | Role | Purpose |
|---|---|---|
| `GET/POST /apply` | CUSTOMER | Submit application + documents |
| `GET /applications` | CUSTOMER | Own applications + notifications |
| `GET /applications/{id}` | CUSTOMER | Status, decision explanation, documents |
| `GET /officer/queue` | OFFICER/ADMIN | Manual-review queue |
| `GET /officer/applications/{id}` | OFFICER/ADMIN | Full file: rules audit, AI advisory, docs |
| `POST /officer/applications/{id}/decision` | OFFICER/ADMIN | Approve/reject with rationale |
