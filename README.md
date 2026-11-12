# QueueDesk

A helpdesk ticketing **REST API** for a fictional IT helpdesk, built while learning Java and
Spring Boot (Groww "Grow before you Groww", Months 1 and 2). Staff log bugs, feature requests and
access requests; QueueDesk triages them by priority and age, and someone works through the queue.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Web · Spring Data JPA (Hibernate) · Bean Validation ·
MySQL 8 · Swagger UI (springdoc) · JUnit 5, Mockito, AssertJ, MockMvc.

The console version (Month 1) is kept under the Git tag `console-version`.

## Architecture

```text
HTTP request (Swagger UI, Postman, curl)
   │
controller/   TicketController, TriageController, ReportController, SimulatorController
   │            reads the request, @Valid checks the fields, returns JSON
validation/   TicketValidator           business rules before anything is saved
   │
service/      TicketService, TriageService, ReportGenerator, TicketAnalytics, ...
   │            business logic, @Transactional
repository/   TicketRepository (Spring Data JPA)
   │
MySQL         tables tickets, ticket_tags
```

Cross-cutting: `exception/GlobalExceptionHandler` turns exceptions into HTTP errors;
`aop/ServiceTimingAspect` logs how long each service call takes.

## Endpoints

| Method | Path | What it does |
|---|---|---|
| POST | `/api/tickets` | Create a ticket → 201 |
| GET | `/api/tickets?status=&assignee=&page=&size=&sort=` | List tickets, one page at a time |
| GET | `/api/tickets/{id}` | One ticket → 200 or 404 |
| PATCH | `/api/tickets/{id}` | Change status, priority, description, assignee, tags (needs `version`) |
| POST | `/api/tickets/{id}/escalate?version=` | Escalate a bug or access ticket |
| GET | `/api/tickets/by-assignee` | Tickets grouped by assignee |
| POST | `/api/tickets/import` | Upload a text file of tickets (multipart field `file`) |
| GET | `/api/triage?order=PRIORITY\|ASSIGNEE` | OPEN tickets in the order they would be served |
| POST | `/api/triage/serve-next?order=&assignee=` | Take the next ticket → 200, or 204 if none |
| GET | `/api/triage/board` | Triage board as plain text |
| GET | `/api/reports/eod` | End-of-day report as plain text |
| POST | `/api/reports/eod/files` | Save the report to `data/reports/` |
| GET | `/api/analytics` | Streams analytics + database counts |
| GET | `/api/simulator?start=&count=` | Month 1's queue number simulator |

Try them all at **http://localhost:8080/swagger-ui.html** while the app runs.

## Validation and errors

1. **Field checks** (`@NotBlank`, `@Size`, ... on the request records, `@Valid` in the controller).
2. **Business rules** in `TicketValidator`: type-specific fields, no fields from another type,
   no duplicate ticket, a CLOSED ticket can't change, an escalated ticket's priority can't be lowered,
   only BUG and ACCESS tickets can be escalated.
3. **Database constraints**: NOT NULL columns and a unique key on (ticket type, title, requester).

Errors use Spring's Problem Details JSON: `400` invalid request, `404` not found, `409` conflict.

## Concurrency

- **IDs:** MySQL `AUTO_INCREMENT`, never duplicated.
- **Lost updates:** `@Version` (optimistic locking). Updates must send the `version` they read;
  a change based on an old version gets `409 Conflict` instead of overwriting someone else's change.
- **Serve next:** a single `UPDATE ... WHERE id = ? AND status = 'OPEN'`, so two requests can never
  take the same ticket.
- **Duplicates at the same moment:** the unique key; the second insert gets `409`.
- **Imports:** one transaction, all or nothing. **Reports:** read in one read-only transaction.
- Proven by `ConcurrencyIntegrationTest`, which runs requests on several threads at once.

## Requirements

- JDK 17
- Maven 3.9+ (or the one bundled with IntelliJ IDEA). The first build downloads the libraries.
- MySQL 8 running on `localhost:3306`

## Set up the database connection

The app reads three environment variables (see `src/main/resources/application.yml`):

| Variable | Default | Meaning |
|---|---|---|
| `QUEUEDESK_DB_URL` | `jdbc:mysql://localhost:3306/queuedesk?createDatabaseIfNotExist=true` | Where MySQL is |
| `QUEUEDESK_DB_USERNAME` | `root` | MySQL user |
| `QUEUEDESK_DB_PASSWORD` | *(empty)* | MySQL password: **never commit it** |

The `queuedesk` database and its tables are created automatically on the first start.

**IntelliJ:** Run → Edit Configurations → QueueDeskApplication → Environment variables:
`QUEUEDESK_DB_USERNAME=root;QUEUEDESK_DB_PASSWORD=yourPassword`

**PowerShell (current window only):**

```powershell
$env:QUEUEDESK_DB_USERNAME = 'root'
$env:QUEUEDESK_DB_PASSWORD = 'yourPassword'
```

## Run

```powershell
mvn spring-boot:run
```

Or in IntelliJ, run `QueueDeskApplication`. On the first start, the sample tickets in
`data/tickets.txt` are imported into the empty database.

## Test

```powershell
mvn test
```

The integration tests need MySQL too. They use a separate database, `queuedesk_test`, which is
created and emptied automatically; your real tickets are never touched.

| Test | What it checks |
|---|---|
| `TicketValidatorTest` | Each business rule, with Mockito instead of a database |
| `TicketApiIntegrationTest` | The HTTP API: status codes, validation errors, paging, optimistic locking |
| `ReportApiIntegrationTest` | The overdue report, using a Clock set two days ahead |
| `ConcurrencyIntegrationTest` | Many threads at once: no ticket served twice, no lost update |

## Text file format (import)

One ticket per line, exactly 7 fields separated by `|`. Lines starting with `#` are ignored.

```text
bug|title|description|requester|PRIORITY|SEVERITY|steps to reproduce
feature|title|description|requester|PRIORITY|votes|business justification
access|title|description|requester|PRIORITY|system name|access level
```

If any line is wrong, nothing is imported and the error names the line. Tickets that already
exist (same type, title and requester) are skipped.

## Rules used

- Effort estimate: bug 2 / 4 / 8 hours (MINOR / MAJOR / BLOCKER), feature 8 hours, access 1 hour.
- Triage order: higher priority first, then the older ticket first. A second queue orders by assignee.
- Escalating a bug or access ticket raises its escalation level and its priority by one step.
- Overdue: CRITICAL, still OPEN or IN_PROGRESS, created more than 24 hours ago.
- Simulator: ticket numbers divisible by 5 go to the VIP lane; numbers divisible by 10 also get a priority recheck.

## Not included yet

- Database migration scripts (Flyway); tables are created by Hibernate (`ddl-auto: update`).
- Authentication.
