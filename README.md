# QueueDesk

A console helpdesk ticketing system for a fictional IT helpdesk, built step by step while
learning Java (Groww "Grow before you Groww", Month 1). Staff log bugs, feature requests and
access requests; QueueDesk triages them by priority and age, and someone works through the queue.

The program talks with you: it shows a menu, asks for ticket details, and prints the result.
Tickets are saved in `data/tickets.json` after every change, so they are still there next time.

```text
------------- MENU -------------
 1. Create a ticket
 2. View triage board (all tickets)
 3. Find a ticket by ID
 4. Show triage queue
 5. Serve next ticket
 6. Import tickets from a text file
 7. Write end-of-day report
 8. Run queue number simulator
 9. View tickets grouped by assignee
10. Update a ticket (status, priority, assignee, tag, escalate)
11. Ticket analytics
 0. Exit
--------------------------------
```

## Milestones

| # | Milestone | Main files |
|---|---|---|
| 1 | Queue Number Simulator: Scanner, loop, modulo, running totals | `QueueNumberSimulator` |
| 2 | Ticket base class: constructors, `this(...)`, getters, setters, `ageInHours()`, static `nextId` and `totalCreated()` | `model/Ticket`, `Priority`, `Status` |
| 3 | Subclasses and a capability: abstract `estimateEffortHours()`, `Escalatable` with a default `escalationBadge()`, `printTriageBoard` | `model/BugTicket`, `FeatureRequestTicket`, `AccessRequestTicket`, `Escalatable`, `Severity`, `service/TriageBoard` |
| 4 | Repository and triage queue: `Comparable`, `TicketComparators`, generic `Repository<T>` (`all()`, `findById`, `groupByAssignee`), two `PriorityQueue`s, generic `oldest()` | `repository/Repository`, `service/TriageService`, `service/TicketComparators` |
| 5 | Loading and reporting: custom checked/unchecked exceptions, pipe-delimited loader, end-of-day report | `service/TicketFileLoader`, `service/ReportGenerator`, `exception/*` |
| 6 | TicketAnalytics: Streams (`groupingBy`, `mapToDouble`, `reduce`, `flatMap`) and `Optional` (`findByIdSafe`) | `service/TicketAnalytics`, `repository/Repository` |
| 7 | tickets.json: Jackson with subtypes, one-time migration from the text file | `service/TicketJsonStore`, `service/TicketMigration` |

## Project structure

```text
QueueDesk/
├── pom.xml                      Maven build file (Java 17, Jackson for JSON)
├── data/tickets.txt             old text format: migrated into tickets.json on the first run
└── src/main/java/com/queuedesk/
    ├── Main.java                the menu: asks questions, calls the other classes
    ├── QueueNumberSimulator.java Milestone 1 (also runs on its own)
    ├── model/                   what a ticket IS
    ├── repository/              where tickets are kept in memory
    ├── service/                 what we DO with tickets (triage, analytics, files, JSON, report)
    ├── exception/               our own exception types
    └── util/                    InputHelper: safe keyboard input
```

## Requirements

- JDK 17 (`java -version` and `javac -version` should both say 17)
- Maven 3.9+, or the Maven bundled with IntelliJ IDEA. The first build downloads Jackson, so it needs internet once.

## How to run

**IntelliJ IDEA:** open the project folder, wait for Maven to load, open
`src/main/java/com/queuedesk/Main.java`, and click the green ▶ next to `main`.
Type your answers in the Run window at the bottom.

**Terminal**, from the project folder:

```powershell
mvn compile exec:java
```

Run only the Milestone 1 simulator:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.queuedesk.QueueNumberSimulator"
```

## Where tickets are stored

```text
first run:   data/tickets.txt  --(read once)-->  data/tickets.json
every run:   data/tickets.json  <-- loaded at start, saved after every change -->  QueueDesk
```

- `data/tickets.json` is created on the first run and is not committed to Git.
- To start again with the sample tickets, close QueueDesk and delete `data/tickets.json`.
- To start with no tickets, close QueueDesk and put `[]` in `data/tickets.json`.
- If `data/tickets.json` is broken, QueueDesk stops without changing it and says what is wrong.
- Edit `data/tickets.json` only while QueueDesk is closed; it is overwritten after every change.

## Text file format (first run and option 6)

One ticket per line, exactly 7 fields separated by `|`. Lines starting with `#` are ignored.

```text
bug|title|description|requester|PRIORITY|SEVERITY|steps to reproduce
feature|title|description|requester|PRIORITY|votes|business justification
access|title|description|requester|PRIORITY|system name|access level
```

If any line is wrong, nothing is imported, and the message says which line to fix.
Option 6 skips tickets that are already in QueueDesk (same type, title and requester),
so importing the same file twice does not create duplicates.

## Rules used

- Effort estimate: bug 2 / 4 / 8 hours (MINOR / MAJOR / BLOCKER), feature 8 hours, access 1 hour.
- Triage order: higher priority first, then the older ticket first.
- Options 4 and 5 can also use a second queue: by assignee (A-Z, unassigned last), then highest priority.
- Escalating a bug or access ticket raises its escalation level and its priority by one step.
- Overdue: CRITICAL, still OPEN or IN_PROGRESS, created more than 24 hours ago.
- Simulator: ticket numbers divisible by 5 go to the VIP lane; numbers divisible by 10 also get a priority recheck.
- Ticket IDs continue after the highest saved ID, so they are never reused.

## Not included yet

- JUnit 5 / Mockito tests.
- QueueDesk is a single-user, single-threaded console program; it is not made for several users at once.
