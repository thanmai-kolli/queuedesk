package com.queuedesk;

import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.AccessRequestTicket;
import com.queuedesk.model.BugTicket;
import com.queuedesk.model.Escalatable;
import com.queuedesk.model.FeatureRequestTicket;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.repository.Repository;
import com.queuedesk.service.ReportGenerator;
import com.queuedesk.service.TicketAnalytics;
import com.queuedesk.service.TicketComparators;
import com.queuedesk.service.TicketFileLoader;
import com.queuedesk.service.TicketJsonStore;
import com.queuedesk.service.TicketMigration;
import com.queuedesk.service.TriageBoard;
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
 * Tickets are saved in data/tickets.json after every change, so they are still there next time.
 */
public class Main {

    private static final Path TICKETS_JSON = Path.of("data", "tickets.json");
    private static final Path OLD_TICKETS_TXT = Path.of("data", "tickets.txt");
    private static final String DEFAULT_REPORT_FILE = "data/eod-report.txt";

    private final Scanner scanner;
    private final InputHelper input;
    private final Repository<Ticket> repository = new Repository<>();
    private final TriageService triageService = new TriageService();
    private final TicketFileLoader fileLoader = new TicketFileLoader();
    private final TicketJsonStore jsonStore = new TicketJsonStore();
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

        if (!loadSavedTickets()) {
            return;   // tickets.json is broken: stop instead of overwriting it
        }

        boolean running = true;
        try {
            while (running) {
                printMenu();
                int choice = input.readInt("Choose an option (0-11): ", 0, 11);
                System.out.println();
                try {
                    switch (choice) {
                        case 1 -> {
                            createTicket();
                            saveTickets();
                        }
                        case 2 -> viewTriageBoard();
                        case 3 -> findTicketById();
                        case 4 -> showTriageQueue();
                        case 5 -> {
                            serveNextTicket();
                            saveTickets();
                        }
                        case 6 -> {
                            importTicketsFromTextFile();
                            saveTickets();
                        }
                        case 7 -> writeEndOfDayReport();
                        case 8 -> QueueNumberSimulator.run(scanner);
                        case 9 -> viewByAssignee();
                        case 10 -> {
                            updateTicket();
                            saveTickets();
                        }
                        case 11 -> showAnalytics();
                        case 0 -> running = false;
                        default -> System.out.println("Unknown option.");
                    }
                } catch (TicketNotFoundException e) {
                    // The one place where a missing ticket is caught: a clean message, not a stack trace.
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
        saveTickets();
        System.out.println("Goodbye! Tickets are saved in " + TICKETS_JSON
                + ". Tickets ever created: " + Ticket.totalCreated());
    }

    private void printMenu() {
        System.out.println("------------- MENU -------------");
        System.out.println(" 1. Create a ticket");
        System.out.println(" 2. View triage board (all tickets)");
        System.out.println(" 3. Find a ticket by ID");
        System.out.println(" 4. Show triage queue");
        System.out.println(" 5. Serve next ticket");
        System.out.println(" 6. Import tickets from a text file");
        System.out.println(" 7. Write end-of-day report");
        System.out.println(" 8. Run queue number simulator");
        System.out.println(" 9. View tickets grouped by assignee");
        System.out.println("10. Update a ticket (status, priority, assignee, tag, escalate)");
        System.out.println("11. Ticket analytics");
        System.out.println(" 0. Exit");
        System.out.println("--------------------------------");
    }

    // ---------- Startup: load tickets.json, or move tickets.txt into it once ----------
    private boolean loadSavedTickets() {
        try {
            List<Ticket> tickets;
            if (Files.exists(TICKETS_JSON)) {
                tickets = jsonStore.load(TICKETS_JSON);
                System.out.println("Loaded " + tickets.size() + " ticket(s) from " + TICKETS_JSON + ".");
            } else if (Files.exists(OLD_TICKETS_TXT)) {
                tickets = new TicketMigration(fileLoader, jsonStore).migrate(OLD_TICKETS_TXT, TICKETS_JSON);
                System.out.println("First run: moved " + tickets.size() + " ticket(s) from " + OLD_TICKETS_TXT
                        + " into " + TICKETS_JSON + ". From now on tickets are saved as JSON.");
            } else {
                tickets = List.of();
                System.out.println("No saved tickets yet. New tickets will be saved in " + TICKETS_JSON + ".");
            }
            for (Ticket ticket : tickets) {
                repository.add(ticket);
            }
            return true;
        } catch (InvalidTicketDataException | IOException e) {
            System.out.println("Could not load the saved tickets, so QueueDesk will stop without changing anything.");
            System.out.println("  " + e.getMessage());
            System.out.println("Fix the file, or move it somewhere else to start again.");
            return false;
        }
    }

    // Called after every menu option that changes tickets.
    private void saveTickets() {
        try {
            jsonStore.save(repository.all(), TICKETS_JSON);
        } catch (InvalidTicketDataException e) {
            System.out.println("Warning: " + e.getMessage());
            System.out.println("Your changes are still in memory; QueueDesk will try again after the next change.");
        }
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

    // ---------- 2. View triage board ----------
    private void viewTriageBoard() {
        if (repository.isEmpty()) {
            System.out.println("No tickets yet. Create one (option 1) or import a text file (option 6).");
            return;
        }
        System.out.println("Sort the tickets by:");
        System.out.println("  1. ID (the order they were created)");
        System.out.println("  2. Priority - most urgent first");
        System.out.println("  3. Assignee, then priority");
        System.out.println("  4. Status, then oldest first");
        int sortChoice = input.readInt("Choose 1-4: ", 1, 4);

        // Copy the list, so sorting does not change the order inside the repository.
        List<Ticket> tickets = new ArrayList<>(repository.all());
        if (sortChoice == 2) {
            Collections.sort(tickets);                       // uses Ticket.compareTo (Comparable)
        } else if (sortChoice == 3) {
            tickets.sort(TicketComparators.BY_ASSIGNEE_THEN_PRIORITY); // uses a Comparator
        } else if (sortChoice == 4) {
            tickets.sort(TicketComparators.BY_STATUS_THEN_AGE);
        }

        System.out.println();
        TriageBoard.printTriageBoard(tickets);
    }

    // ---------- 3. Find a ticket by ID ----------
    // Not finding a ticket is normal here, so it uses findByIdSafe (an Optional):
    // map turns the ticket into its details, ifPresentOrElse prints them or a message.
    private void findTicketById() {
        int id = input.readInt("Ticket ID: ", 1, Integer.MAX_VALUE);
        repository.findByIdSafe(id)
                .map(Ticket::getDetails)
                .ifPresentOrElse(
                        System.out::print,
                        () -> System.out.println("No ticket with ID " + id + ". Option 2 lists all tickets."));
    }

    // ---------- 4. Show triage queue ----------
    private void showTriageQueue() {
        boolean byAssignee = chooseQueueOrder();
        List<Ticket> waiting = triageService.getWaitingInOrder(repository.all(), byAssignee);
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
        Ticket next = triageService.serveNext(repository.all(), byAssignee);
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

    // ---------- 6. Import tickets from a text file ----------
    // Tickets already in QueueDesk (same type, title and requester) are skipped,
    // so importing the same file twice does not create duplicates.
    private void importTicketsFromTextFile() {
        String fileName = input.readOptional("Text file to import (press Enter for " + OLD_TICKETS_TXT + "): ");
        Path path = fileName.isEmpty() ? OLD_TICKETS_TXT : Path.of(fileName);

        try {
            List<Ticket> loaded = fileLoader.load(path);
            int added = 0;
            int skipped = 0;
            for (Ticket ticket : loaded) {
                if (isAlreadyStored(ticket)) {
                    skipped++;
                } else {
                    repository.add(ticket);
                    System.out.println("  added " + ticket);
                    added++;
                }
            }
            System.out.println("Imported " + added + " new ticket(s) from " + path + ". Skipped "
                    + skipped + " already in QueueDesk.");
        } catch (NoSuchFileException e) {
            System.out.println("File not found: " + path.toAbsolutePath());
        } catch (InvalidTicketDataException e) {
            System.out.println("The file has a mistake, so no tickets were imported.");
            System.out.println("  " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not read the file: " + e.getMessage());
        }
    }

    private boolean isAlreadyStored(Ticket newTicket) {
        for (Ticket existing : repository.all()) {
            if (existing.getType().equals(newTicket.getType())
                    && existing.getTitle().equalsIgnoreCase(newTicket.getTitle())
                    && existing.getRequester().equalsIgnoreCase(newTicket.getRequester())) {
                return true;
            }
        }
        return false;
    }

    // ---------- 7. Write end-of-day report ----------
    private void writeEndOfDayReport() {
        String report = reportGenerator.generate(repository.all());
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
            System.out.println("No tickets yet. Create one (option 1) or import a text file (option 6).");
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

    // ---------- 10. Update a ticket ----------
    // Here the ticket must exist, so findById is used: a wrong ID throws TicketNotFoundException,
    // which start() catches once and turns into "Sorry: No ticket found with ID ...".
    private void updateTicket() {
        int id = input.readInt("Ticket ID: ", 1, Integer.MAX_VALUE);
        Ticket ticket = repository.findById(id);
        System.out.println("Updating " + ticket);
        System.out.println("What do you want to change?");
        System.out.println("  1. Status");
        System.out.println("  2. Priority");
        System.out.println("  3. Description");
        System.out.println("  4. Assignee");
        System.out.println("  5. Add a tag");
        System.out.println("  6. Escalate");
        int choice = input.readInt("Choose 1-6: ", 1, 6);

        switch (choice) {
            case 1 -> ticket.setStatus(input.readChoice("New status:", Status.values()));
            case 2 -> ticket.setPriority(input.readChoice("New priority:", Priority.values()));
            case 3 -> ticket.setDescription(input.readOptional("New description (press Enter to clear it): "));
            case 4 -> ticket.setAssignee(input.readOptional("Assign to (press Enter to unassign): "));
            case 5 -> ticket.addTag(input.readRequired("Tag: "));
            default -> {
                // Only some tickets can be escalated, so check the capability first.
                if (ticket instanceof Escalatable escalatable) {
                    escalatable.escalate();
                } else {
                    System.out.println(ticket.getType() + " tickets cannot be escalated. Nothing changed.");
                    return;
                }
            }
        }
        System.out.println();
        System.out.println("Updated:");
        System.out.print(ticket.getDetails());
    }

    // ---------- 11. Ticket analytics (Milestone 6: Streams and Optional) ----------
    private void showAnalytics() {
        List<Ticket> tickets = repository.all();
        System.out.println("=============== Ticket Analytics ===============");

        // The map only has priorities that appear, so getOrDefault fills in the zeros.
        Map<Priority, Long> byPriority = TicketAnalytics.countByPriority(tickets);
        System.out.println("Tickets per priority:");
        for (Priority priority : Priority.values()) {
            System.out.println("  " + priority + ": " + byPriority.getOrDefault(priority, 0L));
        }

        Map<String, List<Ticket>> byAssignee = TicketAnalytics.groupByAssignee(tickets);
        System.out.println("Tickets per assignee:");
        if (byAssignee.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Map.Entry<String, List<Ticket>> entry : byAssignee.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size());
        }

        System.out.println(String.format("Average effort: %.2f hours", TicketAnalytics.averageEffortHours(tickets)));
        System.out.println("Total effort (loop):   " + TicketAnalytics.totalEffortHoursLoop(tickets) + " hours");
        System.out.println("Total effort (stream): " + TicketAnalytics.totalEffortHours(tickets) + " hours");
        System.out.println("Oldest open ticket: "
                + TicketAnalytics.oldestOpenTicket(tickets).map(Ticket::toString).orElse("none"));
        String tags = TicketAnalytics.tagLine(tickets);
        System.out.println("All tags: " + (tags.isEmpty() ? "(none)" : tags));
    }

    // Used by options 4 and 5: which of the two priority queues to use.
    private boolean chooseQueueOrder() {
        System.out.println("Queue order:");
        System.out.println("  1. Most urgent first (natural order: priority, then oldest)");
        System.out.println("  2. By assignee, then most urgent (TicketComparators)");
        return input.readInt("Choose 1-2: ", 1, 2) == 2;
    }
}
