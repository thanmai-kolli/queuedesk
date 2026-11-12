package com.queuedesk.service;

import com.queuedesk.model.Escalatable;
import com.queuedesk.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Milestone 3: the triage board, one line per ticket (GET /api/triage/board).
 *
 * The loop only knows the base type Ticket, yet every ticket answers
 * estimateEffortHours() and toString() in its own way (polymorphism).
 *
 * In the console version this method printed to the screen. A web server has no screen,
 * so it now builds the text and the controller sends it back.
 */
public final class TriageBoard {

    private static final int MAX_DESCRIPTION_LENGTH = 40;

    private TriageBoard() {
        // only static methods here, so nobody needs to create an object
    }

    public static String renderTriageBoard(List<Ticket> tickets, LocalDateTime now) {
        StringBuilder board = new StringBuilder();
        board.append("=============== QueueDesk Triage Board ===============\n");
        for (Ticket ticket : tickets) {
            String assignee = ticket.getAssignee() == null ? "unassigned" : ticket.getAssignee();
            board.append(ticket)
                    .append(" | ").append(ticket.getType())
                    .append(" | ").append(ticket.estimateEffortHours()).append("h")
                    .append(" | ").append(assignee)
                    .append(" | ").append(ticket.ageInHours(now)).append("h old")
                    .append(" | ").append(shorten(ticket.getDescription()));

            // Only some tickets can be escalated: check the capability, then use it.
            if (ticket instanceof Escalatable e) {
                board.append(" | ").append(e.escalationBadge());
            }
            board.append("\n");
        }
        board.append("Total: ").append(tickets.size()).append(" ticket(s)\n");
        return board.toString();
    }

    // Long descriptions are cut so each ticket stays on one line.
    // substring is only called when the text is longer than the limit, so it can't go out of range.
    static String shorten(String description) {
        String text = description == null ? "" : description.trim();
        if (text.isEmpty()) {
            return "(no description)";
        }
        if (text.length() <= MAX_DESCRIPTION_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }
}
