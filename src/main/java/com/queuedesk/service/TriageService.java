package com.queuedesk.service;

import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Milestone 4: decides which OPEN ticket should be worked on next.
 *
 * A PriorityQueue always keeps the "smallest" element at the head, and poll() removes it.
 * There are two queues, so the same tickets can be served in two different orders.
 */
@Service
public class TriageService {

    private final TicketRepository repository;

    public TriageService(TicketRepository repository) {
        this.repository = repository;
    }

    // Printing a PriorityQueue directly does NOT show sorted order. Polling one by one does.
    @Transactional(readOnly = true)
    public List<Ticket> waitingInOrder(QueueOrder order) {
        PriorityQueue<Ticket> queue = buildQueue(order, repository.findByStatus(Status.OPEN));
        List<Ticket> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    /**
     * Takes the next OPEN ticket and marks it IN_PROGRESS. Safe when many requests call it at once.
     *
     * The PriorityQueue picks the best candidate in Java. The database then changes it only if it
     * is STILL OPEN (startWorkIfOpen). If another request took it a moment earlier, that update
     * changes 0 rows and we try the next candidate. So one ticket is never served twice.
     */
    @Transactional
    public Optional<Ticket> serveNext(QueueOrder order, String assignee) {
        PriorityQueue<Ticket> queue = buildQueue(order, repository.findByStatus(Status.OPEN));
        String cleanAssignee = assignee == null || assignee.isBlank() ? null : assignee.trim();

        while (!queue.isEmpty()) {
            Ticket candidate = queue.poll();
            int changedRows = repository.startWorkIfOpen(candidate.getId(), cleanAssignee,
                    Status.OPEN, Status.IN_PROGRESS);
            if (changedRows == 1) {
                return repository.findById(candidate.getId());   // read it again with the new values
            }
            // 0 rows: someone else started this ticket just now. Try the next one.
        }
        return Optional.empty();
    }

    private PriorityQueue<Ticket> buildQueue(QueueOrder order, List<Ticket> openTickets) {
        // Queue 1 has no comparator, so it uses Ticket.compareTo (Comparable).
        // Queue 2 is given a Comparator, so it uses that order instead.
        PriorityQueue<Ticket> queue = order == QueueOrder.ASSIGNEE
                ? new PriorityQueue<>(TicketComparators.BY_ASSIGNEE_THEN_PRIORITY)
                : new PriorityQueue<>();
        for (Ticket ticket : openTickets) {
            queue.offer(ticket);
        }
        return queue;
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
