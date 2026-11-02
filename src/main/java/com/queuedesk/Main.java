package com.queuedesk;

import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.AccessRequestTicket;
import com.queuedesk.model.BugTicket;
import com.queuedesk.model.Escalatable;
import com.queuedesk.model.FeatureRequestTicket;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.Ticket;
import com.queuedesk.repository.Repository;
import com.queuedesk.service.ReportGenerator;
import com.queuedesk.service.TicketComparators;
import com.queuedesk.service.TicketFileLoader;
import com.queuedesk.service.TriageService;
import com.queuedesk.util.InputHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * QueueDesk: the helpdesk ticketing system.
 * Shows a menu, asks the user what to do, and calls the right class for each option.
 * Tickets are kept in memory while the program runs.
 */
public class Main {

    private static final String DEFAULT_TICKET_FILE = "data/tickets.txt";
    private static final String DEFAULT_REPORT_FILE = "data/eod-report.txt";

    private final Scanner scanner;
    private final InputHelper input;
    private final Repository<Ticket> repository = new Repository<>();
    private final TriageService triageService = new TriageService();
    private final TicketFileLoader fileLoader = new TicketFileLoader();
    private final ReportGenerator reportGenerator = new ReportGenerator();

    public Main(Scanner scanner) {
        this.scanner = scanner;
        this.input = new InputHelper(scanner);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new Main(scanner).start();
    }

    private void start() {
        System.out.println("=========================================");
        System.out.println("  Welcome to QueueDesk - Helpdesk System");
        System.out.println("=========================================");

        boolean running = true;
        try {
            while (running) {
                printMenu();
                int choice = input.readInt("Choose an option (0-9): ", 0, 9);
                System.out.println();
                try {
                    switch (choice) {
                        case 1 -> createTicket();
                        case 2 -> viewAllTickets();
                        case 3 -> findTicketById();
                        case 4 -> showTriageQueue();
                        case 5 -> serveNextTicket();
                        case 6 -> loadTicketsFromFile();
                        case 7 -> writeEndOfDayReport();
                        case 8 -> QueueNumberSimulator.run(scanner);
                        case 9 -> viewByAssignee();
                        case 0 -> running = false;
                        default -> System.out.println("Unknown option.");
                    }
                } catch (TicketNotFoundException e) {
                    System.out.println("Sorry: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not do that: " + e.getMessage());
                }
                System.out.println();
            }
        } catch (NoSuchElementException e) {
            // Happens if the input stream is closed (for example Ctrl+D or Ctrl+Z).
            System.out.println();
            System.out.println("No more input.");
        }
        System.out.println("Goodbye! Tickets created while QueueDesk was running: " + Ticket.getTotalCreated());
    }

    private void printMenu() {
        System.out.println("------------- MENU -------------");
        System.out.println(" 1. Create a ticket");
        System.out.println(" 2. View all tickets");
        System.out.println(" 3. Find a ticket by ID");
        System.out.println(" 4. Show triage queue (most urgent first)");
        System.out.println(" 5. Serve next ticket");
        System.out.println(" 6. Load tickets from a file");
        System.out.println(" 7. Write end-of-day report");
        System.out.println(" 8. Run queue number simulator");
        System.out.println(" 9. View tickets grouped by assignee");
        System.out.println(" 0. Exit");
        System.out.println("--------------------------------");
    }

    // ---------- 1. Create a ticket ----------
    private void createTicket() {
        System.out.println("--- Create a new ticket ---");
        System.out.println("What kind of ticket is it?");
        System.out.println("  1. Bug - something is broken");
        System.out.println("  2. Feature request - something new is needed");
        System.out.println("  3. Access request - someone needs access to a system");
        int type = input.readInt("Choose 1-3: ", 1, 3);

        String title = input.readRequired("Title: ");
        String description = input.readOptional("Description (optional, press Enter to skip): ");
        String requester = input.readRequired("Requester name: ");
        Priority priority = input.readChoice("Priority:", Priority.values());

        Ticket ticket;
        if (type == 1) {
            Severity severity = input.readChoice("Severity:", Severity.values());
            String steps = input.readRequired("Steps to reproduce: ");
            ticket = new BugTicket(title, description, requester, priority, severity, steps);
        } else if (type == 2) {
            int votes = input.readInt("Votes (how many people want this, 0 or more): ", 0, Integer.MAX_VALUE);
            String justification = input.readRequired("Business justification (why is it needed?): ");
            ticket = new FeatureRequestTicket(title, description, requester, priority, votes, justification);
        } else {
            String systemName = input.readRequired("System name: ");
            String accessLevel = input.readRequired("Access level (for example READ, WRITE, ADMIN): ");
            ticket = new AccessRequestTicket(title, description, requester, priority, systemName, accessLevel);
        }

        ticket.setAssignee(input.readOptional("Assign to (optional, press Enter to leave unassigned): "));

        String tagText = input.readOptional("Tags separated by commas (optional): ");
        for (String tag : tagText.split(",")) {
            if (!tag.isBlank()) {
                ticket.addTag(tag);
            }
        }

        repository.add(ticket);
        System.out.println();
        System.out.println("Ticket created successfully!");
        System.out.print(ticket.getDetails());
    }

    // ---------- 2. View all tickets ----------
    private void viewAllTickets() {
        if (repository.isEmpty()) {
            System.out.println("No tickets yet. Create one (option 1) or load a file (option 6).");
            return;
        }
        System.out.println("Sort the tickets by:");
        System.out.println("  1. ID (the order they were created)");
        System.out.println("  2. Priority - most urgent first");
        System.out.println("  3. Assignee, then priority");
        System.out.println("  4. Status, then oldest first");
        int sortChoice = input.readInt("Choose 1-4: ", 1, 4);

        // Copy the list, so sorting does not change the order inside the repository.
        List<Ticket> tickets = new ArrayList<>(repository.findAll());
        if (sortChoice == 2) {
            Collections.sort(tickets);                       // uses Ticket.compareTo (Comparable)
        } else if (sortChoice == 3) {
            tickets.sort(TicketComparators.BY_ASSIGNEE_THEN_PRIORITY); // uses a Comparator
        } else if (sortChoice == 4) {
            tickets.sort(TicketComparators.BY_STATUS_THEN_AGE);
        }

        System.out.println();
        System.out.println(String.format("%-4s %-16s %-9s %-12s %-12s %-7s %s",
                "ID", "TYPE", "PRIORITY", "STATUS", "ASSIGNEE", "EFFORT", "TITLE"));
        for (Ticket ticket : tickets) {
            String title = ticket.getTitle();
            if (ticket instanceof Escalatable escalatable && escalatable.isEscalated()) {
                title += "  [ESCALATED L" + escalatable.getEscalationLevel() + "]";
            }
            System.out.println(String.format("%-4d %-16s %-9s %-12s %-12s %-7s %s",
                    ticket.getId(),
                    ticket.getType(),
                    ticket.getPriority(),
                    ticket.getStatus(),
                    ticket.getAssignee() == null ? "-" : ticket.getAssignee(),
                    ticket.estimateEffortHours() + "h",
                    title));
        }
        System.out.println("Total: " + tickets.size() + " tickets");
    }

    // ---------- 3. Find a ticket by ID ----------
    private void findTicketById() {
        int id = input.readInt("Ticket ID: ", 1, Integer.MAX_VALUE);
        Ticket ticket = repository.findById(id); // throws TicketNotFoundException if the ID is unknown
        System.out.print(ticket.getDetails());

        // Only some tickets can be escalated, so check the capability first.
        if (ticket instanceof Escalatable escalatable) {
            if (input.readYesNo("Escalate this ticket? (y/n): ")) {
                escalatable.escalate();
                System.out.println("Escalated. Level is now " + escalatable.getEscalationLevel()
                        + " and priority is now " + ticket.getPriority() + ".");
            }
        } else {
            System.out.println("(" + ticket.getType() + " tickets cannot be escalated.)");
        }
    }

    // ---------- 4. Show triage queue ----------
    private void showTriageQueue() {
        boolean byAssignee = chooseQueueOrder();
        List<Ticket> waiting = triageService.getWaitingInOrder(repository.findAll(), byAssignee);
        if (waiting.isEmpty()) {
            System.out.println("No OPEN tickets are waiting.");
            return;
        }
        System.out.println("Triage queue - " + waiting.size() + " OPEN tickets, "
                + (byAssignee ? "by assignee, then most urgent:" : "most urgent first:"));
        int position = 1;
        for (Ticket ticket : waiting) {
            String assignee = ticket.getAssignee() == null ? "unassigned" : ticket.getAssignee();
            System.out.println("  " + position + ". " + ticket + " - " + assignee
                    + ", waiting " + ticket.ageInHours() + "h");
            position++;
        }
    }

    // ---------- 5. Serve next ticket ----------
    private void serveNextTicket() {
        boolean byAssignee = chooseQueueOrder();
        Ticket next = triageService.serveNext(repository.findAll(), byAssignee);
        if (next == null) {
            System.out.println("No OPEN tickets are waiting. Nothing to serve.");
            return;
        }
        System.out.println("Now serving: " + next);
        if (next.getAssignee() == null) {
            next.setAssignee(input.readOptional("Who will work on it? (optional, press Enter to skip): "));
        }
        System.out.println("Ticket #" + next.getId() + " is now IN_PROGRESS"
                + (next.getAssignee() == null ? "." : " with " + next.getAssignee() + "."));
    }

    // ---------- 6. Load tickets from a file ----------
    private void loadTicketsFromFile() {
        String fileName = input.readOptional("File to load (press Enter for " + DEFAULT_TICKET_FILE + "): ");
        if (fileName.isEmpty()) {
            fileName = DEFAULT_TICKET_FILE;
        }
        Path path = Path.of(fileName);

        try {
            List<Ticket> loaded = fileLoader.load(path);
            for (Ticket ticket : loaded) {
                repository.add(ticket);
            }
            System.out.println("Loaded " + loaded.size() + " tickets from " + fileName + ":");
            for (Ticket ticket : loaded) {
                System.out.println("  " + ticket);
            }
        } catch (NoSuchFileException e) {
            System.out.println("File not found: " + path.toAbsolutePath());
        } catch (InvalidTicketDataException e) {
            System.out.println("The file has a mistake, so no tickets were loaded.");
            System.out.println("  " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not read the file: " + e.getMessage());
        }
    }

    // ---------- 7. Write end-of-day report ----------
    private void writeEndOfDayReport() {
        String report = reportGenerator.generate(repository.findAll());
        System.out.print(report);

        String fileName = input.readOptional("Save report to (press Enter for " + DEFAULT_REPORT_FILE + "): ");
        if (fileName.isEmpty()) {
            fileName = DEFAULT_REPORT_FILE;
        }
        Path path = Path.of(fileName);

        if (Files.exists(path) && !input.readYesNo(fileName + " already exists. Replace it? (y/n): ")) {
            System.out.println("Report not saved.");
            return;
        }
        try {
            reportGenerator.writeToFile(report, path);
            System.out.println("Report saved to " + path.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not save the report: " + e.getMessage());
        }
    }

    // ---------- 9. View tickets grouped by assignee ----------
    private void viewByAssignee() {
        if (repository.isEmpty()) {
            System.out.println("No tickets yet. Create one (option 1) or load a file (option 6).");
            return;
        }
        Map<String, List<Ticket>> groups = repository.groupByAssignee();
        for (Map.Entry<String, List<Ticket>> entry : groups.entrySet()) {
            List<Ticket> tickets = entry.getValue();
            Ticket oldest = TriageService.oldest(tickets);   // the generic method
            System.out.println(entry.getKey() + " - " + tickets.size() + " ticket(s), oldest is #" + oldest.getId() + ":");
            for (Ticket ticket : tickets) {
                System.out.println("    " + ticket);
            }
        }
    }

    // Used by options 4 and 5: which of the two priority queues to use.
    private boolean chooseQueueOrder() {
        System.out.println("Queue order:");
        System.out.println("  1. Most urgent first (natural order: priority, then oldest)");
        System.out.println("  2. By assignee, then most urgent (TicketComparators)");
        return input.readInt("Choose 1-2: ", 1, 2) == 2;
    }
}
