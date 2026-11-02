package com.queuedesk.repository;

import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Milestone 4: a generic store for tickets, kept in memory.
 *
 * T is a placeholder for the type stored. "T extends Ticket" (a bound) means
 * T must be Ticket or a subclass, so the code below can safely call getId() and getStatus().
 * Examples: Repository<Ticket> holds every kind; Repository<BugTicket> holds only bugs.
 */
public class Repository<T extends Ticket> {

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add an empty ticket");
        }
        items.add(item);
    }

    public T findById(int id) {
        for (T item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        throw new TicketNotFoundException(id);
    }

    // Read-only view: callers can look at the list but cannot add or remove from it.
    public List<T> findAll() {
        return Collections.unmodifiableList(items);
    }

    public List<T> findByStatus(Status status) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (item.getStatus() == status) {
                result.add(item);
            }
        }
        return result;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
