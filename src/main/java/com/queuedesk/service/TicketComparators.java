package com.queuedesk.service;

import com.queuedesk.model.Ticket;

import java.util.Comparator;

/**
 * Milestone 4: extra ways to sort tickets.
 *
 * Ticket.compareTo gives the ONE natural order (priority, then age).
 * A Comparator is a separate object that gives ANOTHER order,
 * without changing the Ticket class.
 */
public final class TicketComparators {

    private TicketComparators() {
        // only constants here, so nobody needs to create an object
    }

    // Assignee A-Z (unassigned tickets last), then the most urgent first.
    public static final Comparator<Ticket> BY_ASSIGNEE_THEN_PRIORITY =
            Comparator.comparing(Ticket::getAssignee, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(Ticket::getPriority, Comparator.reverseOrder());

    // Status in workflow order (OPEN first), then the oldest first.
    public static final Comparator<Ticket> BY_STATUS_THEN_AGE =
            Comparator.comparing(Ticket::getStatus)
                    .thenComparing(Ticket::getCreatedAt);
}
