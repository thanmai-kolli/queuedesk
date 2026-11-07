package com.queuedesk.model;

/**
 * A capability: "this kind of ticket can be escalated".
 *
 * Not every ticket can be escalated (a feature request cannot), so this is an
 * interface that only some ticket classes implement, instead of a method in Ticket.
 */
public interface Escalatable {

    void escalate();

    boolean isEscalated();

    int escalationLevel();

    // A default method has a body, so classes that implement the interface get it for free.
    default String escalationBadge() {
        return isEscalated() ? "ESCALATED (level " + escalationLevel() + ")" : "not escalated";
    }
}
