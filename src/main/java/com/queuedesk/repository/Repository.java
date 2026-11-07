package com.queuedesk.repository;

import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.Ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Milestone 4: a generic store for tickets, kept in memory.
 *
 * T is a placeholder for the type stored. "T extends Ticket" (a bound) means
 * T must be Ticket or a subclass, so the code below can safely call getId() and getAssignee().
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

    // Read-only view: callers can look at the list but cannot add or remove from it.
    public List<T> all() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Milestone 6: the lookup itself. Returns an empty Optional when no ticket has this id,
     * so the caller decides what "not found" means for them.
     */
    public Optional<T> findByIdSafe(int id) {
        for (T item : items) {
            if (item.getId() == id) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Milestone 4: the same lookup, but a missing id is an error.
     * TicketNotFoundException is unchecked, so callers are not forced to catch it;
     * Main catches it once, at the top of the menu loop.
     */
    public T findById(int id) {
        return findByIdSafe(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    // Puts every ticket in a list under its assignee's name; unassigned tickets go under "Unassigned".
    // The TreeMap keeps names sorted A-Z, ignoring upper/lower case (so "asha" and "Asha" share a group).
    public Map<String, List<T>> groupByAssignee() {
        Map<String, List<T>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (T item : items) {
            String name = item.getAssignee() == null ? "Unassigned" : item.getAssignee();
            List<T> group = groups.get(name);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(name, group);
            }
            group.add(item);
        }
        return groups;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
