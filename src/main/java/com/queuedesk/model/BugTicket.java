package com.queuedesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Milestone 3: a ticket for something that is broken.
 * It IS a Ticket (extends) and it CAN be escalated (implements Escalatable).
 * Stored in the "tickets" table with ticket_type = 'BUG'.
 */
@Entity
@DiscriminatorValue("BUG")
public class BugTicket extends Ticket implements Escalatable {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Column(name = "steps_to_reproduce", length = 2000)
    private String stepsToReproduce;

    // The same column is used by AccessRequestTicket; feature-request rows leave it empty.
    @Column(name = "escalation_level")
    private int escalationLevel = 0;

    // For JPA only (see Ticket).
    protected BugTicket() {
    }

    public BugTicket(String title, String description, String requester, Priority priority,
                     Severity severity, String stepsToReproduce) {
        super(title, description, requester, priority); // builds the Ticket part first
        if (severity == null) {
            throw new IllegalArgumentException("Severity is required for a bug");
        }
        if (isBlank(stepsToReproduce)) {
            throw new IllegalArgumentException("Steps to reproduce are required for a bug");
        }
        this.severity = severity;
        this.stepsToReproduce = stepsToReproduce.trim();
    }

    // Short version for quick bugs: MEDIUM priority and MINOR severity.
    public BugTicket(String title, String requester) {
        this(title, "", requester, Priority.MEDIUM, Severity.MINOR, "Not provided yet");
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    @Override
    public double estimateEffortHours() {
        return switch (severity) {
            case MINOR -> 2.0;
            case MAJOR -> 4.0;
            case BLOCKER -> 8.0;
        };
    }

    @Override
    public TicketType getTicketType() {
        return TicketType.BUG;
    }

    @Override
    public String getType() {
        return "Bug";
    }

    @Override
    public void escalate() {
        escalationLevel++;
        raisePriority();
    }

    @Override
    public boolean isEscalated() {
        return escalationLevel > 0;
    }

    @Override
    public int escalationLevel() {
        return escalationLevel;
    }

    @Override
    public Map<String, Object> typeDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("severity", severity);
        details.put("stepsToReproduce", stepsToReproduce);
        details.put("escalationLevel", escalationLevel);
        details.put("escalation", escalationBadge());
        return details;
    }
}
