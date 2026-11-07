package com.queuedesk.service;

import com.queuedesk.model.Escalatable;
import com.queuedesk.model.Ticket;

import java.util.List;

/**
 * Milestone 3: prints the triage board, one line per ticket.
 *
 * The loop only knows the base type Ticket, yet every ticket answers
 * estimateEffortHours() and toString() in its own way (polymorphism).
 */
public final class TriageBoard {

    private static final int MAX_DESCRIPTION_LENGTH = 40;

    private TriageBoard() {
        // only a static method here, so nobody needs to create an object
    }

    public static void printTriageBoard(List<Ticket> tickets) {
        System.out.println("=============== QueueDesk Triage Board ===============");
        for (Ticket ticket : tickets) {
            String assignee = ticket.getAssignee() == null ? "unassigned" : ticket.getAssignee();
            String line = ticket + " | " + ticket.getType()
                    + " | " + ticket.estimateEffortHours() + "h"
                    + " | " + assignee
                    + " | " + shorten(ticket.getDescription());

            // Only some tickets can be escalated: check the capability, then use it.
            if (ticket instanceof Escalatable e) {
                line += " | " + e.escalationBadge();
            }
            System.out.println(line);
        }
        System.out.println("Total: " + tickets.size() + " ticket(s)");
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
