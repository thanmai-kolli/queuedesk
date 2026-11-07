package com.queuedesk.service;

import com.queuedesk.model.Escalatable;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Milestone 5: builds the end-of-day (EOD) summary and saves it to a file.
 * Milestone 6: the total effort and the oldest open ticket now come from TicketAnalytics.
 */
public class ReportGenerator {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public String generate(List<Ticket> tickets) {
        LocalDateTime now = LocalDateTime.now();

        // StringBuilder adds text to one buffer instead of creating a new String each time.
        StringBuilder report = new StringBuilder();
        report.append("========== QueueDesk End-of-Day Report ==========\n");
        report.append("Generated at : ").append(now.format(FORMAT)).append("\n");
        report.append("Total tickets: ").append(tickets.size()).append("\n\n");

        // Loop over every enum value so statuses with 0 tickets are shown too.
        report.append("Tickets by status:\n");
        for (Status status : Status.values()) {
            int count = 0;
            for (Ticket ticket : tickets) {
                if (ticket.getStatus() == status) {
                    count++;
                }
            }
            report.append(String.format("  %-12s %d\n", status, count));
        }

        report.append("\nTickets by priority:\n");
        for (Priority priority : Priority.values()) {
            int count = 0;
            for (Ticket ticket : tickets) {
                if (ticket.getPriority() == priority) {
                    count++;
                }
            }
            report.append(String.format("  %-12s %d\n", priority, count));
        }

        // Each subclass answers estimateEffortHours() in its own way (polymorphism).
        report.append(String.format("\nTotal estimated effort: %.1f hours\n",
                TicketAnalytics.totalEffortHours(tickets)));

        // oldestOpenTicket returns an Optional: map turns a ticket into text, orElse covers "no OPEN tickets".
        report.append("Oldest open ticket: ")
                .append(TicketAnalytics.oldestOpenTicket(tickets)
                        .map(t -> t + " - " + t.ageInHours() + " hours old")
                        .orElse("none"))
                .append("\n");

        report.append("\nEscalated tickets:\n");
        int escalatedCount = 0;
        for (Ticket ticket : tickets) {
            if (ticket instanceof Escalatable escalatable && escalatable.isEscalated()) {
                report.append("  ").append(ticket).append(" - level ")
                        .append(escalatable.escalationLevel()).append("\n");
                escalatedCount++;
            }
        }
        if (escalatedCount == 0) {
            report.append("  none\n");
        }

        report.append("\nOverdue: CRITICAL, still OPEN or IN_PROGRESS, older than 24 hours:\n");
        int overdueCount = 0;
        for (Ticket ticket : tickets) {
            if (isOverdue(ticket, now)) {
                report.append("  ").append(ticket).append(" - ").append(ticket.ageInHours()).append(" hours old\n");
                overdueCount++;
            }
        }
        if (overdueCount == 0) {
            report.append("  none\n");
        }

        report.append("=================================================\n");
        return report.toString();
    }

    private boolean isOverdue(Ticket ticket, LocalDateTime now) {
        boolean critical = ticket.getPriority() == Priority.CRITICAL;
        boolean stillActive = ticket.getStatus() == Status.OPEN || ticket.getStatus() == Status.IN_PROGRESS;
        boolean olderThanOneDay = ticket.getCreatedAt().isBefore(now.minusHours(24));
        return critical && stillActive && olderThanOneDay;
    }

    public void writeToFile(String report, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(report);
        }
    }
}
