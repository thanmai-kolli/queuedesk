package com.queuedesk.service;

import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Milestone 4: decides which OPEN ticket should be worked on next.
 *
 * A PriorityQueue always keeps the "smallest" element at the head, and poll() removes it.
 * There are two queues, so the same tickets can be served in two different orders:
 *  - natural order: Ticket.compareTo (most urgent first, then oldest)
 *  - assignee order: TicketComparators.BY_ASSIGNEE_THEN_PRIORITY
 */
public class TriageService {

    // Queue 1: no comparator given, so it uses Ticket.compareTo (Comparable).
    private PriorityQueue<Ticket> buildPriorityQueue(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = new PriorityQueue<>();
        addOpenTickets(queue, tickets);
        return queue;
    }

    // Queue 2: a Comparator is passed in, so the queue uses that order instead.
    private PriorityQueue<Ticket> buildAssigneeQueue(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = new PriorityQueue<>(TicketComparators.BY_ASSIGNEE_THEN_PRIORITY);
        addOpenTickets(queue, tickets);
        return queue;
    }

    // The queues are rebuilt every time, so they always reflect the latest priorities and statuses.
    private void addOpenTickets(PriorityQueue<Ticket> queue, List<Ticket> tickets) {
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == Status.OPEN) {
                queue.offer(ticket);
            }
        }
    }

    // Printing a PriorityQueue directly does NOT show sorted order. Polling one by one does.
    public List<Ticket> getWaitingInOrder(List<Ticket> tickets, boolean byAssignee) {
        PriorityQueue<Ticket> queue = byAssignee ? buildAssigneeQueue(tickets) : buildPriorityQueue(tickets);
        List<Ticket> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    // Takes the head of the chosen queue and marks it IN_PROGRESS.
    // Returns null when no ticket is waiting.
    public Ticket serveNext(List<Ticket> tickets, boolean byAssignee) {
        PriorityQueue<Ticket> queue = byAssignee ? buildAssigneeQueue(tickets) : buildPriorityQueue(tickets);
        Ticket next = queue.poll();
        if (next != null) {
            next.setStatus(Status.IN_PROGRESS);
        }
        return next;
    }

    /**
     * A generic method: works for a List<Ticket>, a List<BugTicket>, or any other ticket subclass,
     * and returns the same type it was given, with no cast needed.
     * If two tickets have the same creation time, the first one in the list wins.
     */
    public static <T extends Ticket> T oldest(List<T> tickets) {
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Cannot find the oldest ticket in an empty list");
        }
        T oldest = tickets.get(0);
        for (T ticket : tickets) {
            if (ticket.getCreatedAt().isBefore(oldest.getCreatedAt())) {
                oldest = ticket;
            }
        }
        return oldest;
    }
}
