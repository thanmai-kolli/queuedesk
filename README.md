# QueueDesk

A console helpdesk ticketing system, built step by step while learning Java
fundamentals (Phase 1: OOP, Collections, Generics, Exceptions).

The program talks with you: it shows a menu, asks for ticket details, and
prints the result.

```text
------------- MENU -------------
 1. Create a ticket
 2. View all tickets
 3. Find a ticket by ID
 4. Show triage queue (most urgent first)
 5. Serve next ticket
 6. Load tickets from a file
 7. Write end-of-day report
 8. Run queue number simulator
 0. Exit
--------------------------------
```

## Milestones

| # | Milestone | Main files |
|---|---|---|
| 1 | Queue Number Simulator: Scanner, loop, modulo, running totals | `QueueNumberSimulator` |
| 2 | Ticket base class: constructors, fields, `ageInHours()`, static counters | `model/Ticket`, `Priority`, `Status` |
| 3 | Subclasses and capabilities | `model/BugTicket`, `FeatureRequestTicket`, `AccessRequestTicket`, `Escalatable`, `Severity` |
| 4 | Repository and triage queue: `PriorityQueue`, `Comparable`, comparators, generic `Repository<T>` | `repository/Repository`, `service/TriageService`, `service/TicketComparators` |
| 5 | File loading, custom checked/unchecked exceptions, end-of-day report | `service/TicketFileLoader`, `service/ReportGenerator`, `exception/*` |

## Project structure

```text
QueueDesk/
├── pom.xml                      Maven build file (Java 17, no external libraries)
├── data/tickets.txt             sample tickets for option 6
└── src/main/java/com/queuedesk/
    ├── Main.java                the menu: asks questions, calls the other classes
    ├── QueueNumberSimulator.java Milestone 1 (also runs on its own)
    ├── model/                   what a ticket IS
    ├── repository/              where tickets are stored
    ├── service/                 what we DO with tickets (triage, files, report)
    ├── exception/               our own exception types
    └── util/                    InputHelper: safe keyboard input
```

## Requirements

- JDK 17 (`java -version` and `javac -version` should both say 17)
- Maven 3.9+, or the Maven bundled with IntelliJ IDEA

## How to run

**IntelliJ IDEA:** open the `QueueDesk` folder, wait for Maven to load, open
`src/main/java/com/queuedesk/Main.java`, and click the green ▶ next to `main`.
Type your answers in the Run window at the bottom.

**Terminal**, from the `QueueDesk` folder:

```powershell
mvn compile exec:java
```

Run only the Milestone 1 simulator:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.queuedesk.QueueNumberSimulator"
```

Build a jar and run it:

```powershell
mvn package
java -jar target/queuedesk-1.0-SNAPSHOT.jar
```

## Ticket file format (option 6)

One ticket per line, fields separated by `|`. Lines starting with `#` are ignored.

```text
bug|title|description|requester|PRIORITY|SEVERITY|steps to reproduce
feature|title|description|requester|PRIORITY|votes|business justification
access|title|description|requester|PRIORITY|system name|access level
```

An optional 8th field sets the creation time, for example `2026-10-30T09:00`.
If any line is wrong, nothing is loaded, and the message says which line to fix.

## Rules used

- Effort estimate: bug 2 / 4 / 8 hours (MINOR / MAJOR / BLOCKER), feature 8 hours, access 1 hour.
- Triage order: higher priority first, then the older ticket first.
- Escalating a bug or access ticket raises its escalation level and its priority by one step.
- Overdue: CRITICAL, still OPEN or IN_PROGRESS, created more than 24 hours ago.
- Simulator: ticket numbers divisible by 5 go to the VIP lane; numbers divisible by 10 also get a priority recheck.

## Not included yet

Tickets live in memory only; they are gone when the program exits.
JSON storage, Streams, and JUnit tests are later topics in the month and will be
added as later milestones.
