package com.queuedesk.service;

import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Milestone 4: decides which OPEN ticket should be worked on next.
 *
 * A PriorityQueue always keeps the "smallest" element at the head. It uses
 * Ticket.compareTo, where "smallest" means most urgent, so poll() gives the next ticket.
 */
public class TriageService {

    // The queue is rebuilt every time, so it always reflects the latest priorities and statuses.
    private PriorityQueue<Ticket> buildQueue(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = new PriorityQueue<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == Status.OPEN) {
                queue.add(ticket);
            }
        }
        return queue;
    }

    // Printing a PriorityQueue directly does NOT show sorted order. Polling one by one does.
    public List<Ticket> getWaitingInOrder(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = buildQueue(tickets);
        List<Ticket> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    // Takes the most urgent OPEN ticket and marks it IN_PROGRESS.
    // Returns null when no ticket is waiting.
    public Ticket serveNext(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = buildQueue(tickets);
        Ticket next = queue.poll();
        if (next != null) {
            next.setStatus(Status.IN_PROGRESS);
        }
        return next;
    }
}
