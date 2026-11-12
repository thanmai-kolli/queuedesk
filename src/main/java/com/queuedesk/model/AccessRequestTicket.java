package com.queuedesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Milestone 3: a ticket asking for access to a system.
 * It IS a Ticket and it CAN be escalated.
 * Stored in the "tickets" table with ticket_type = 'ACCESS'.
 */
@Entity
@DiscriminatorValue("ACCESS")
public class AccessRequestTicket extends Ticket implements Escalatable {

    @Column(name = "system_name", length = 100)
    private String systemName;

    @Column(name = "access_level", length = 50)
    private String accessLevel;

    // The same column is used by BugTicket.
    @Column(name = "escalation_level")
    private int escalationLevel = 0;

    // For JPA only (see Ticket).
    protected AccessRequestTicket() {
    }

    public AccessRequestTicket(String title, String description, String requester, Priority priority,
                               String systemName, String accessLevel) {
        super(title, description, requester, priority);
        if (isBlank(systemName)) {
            throw new IllegalArgumentException("System name is required for an access request");
        }
        if (isBlank(accessLevel)) {
            throw new IllegalArgumentException("Access level is required for an access request");
        }
        this.systemName = systemName.trim();
        this.accessLevel = accessLevel.trim().toUpperCase();
    }

    public String getSystemName() {
        return systemName;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    @Override
    public double estimateEffortHours() {
        return 1.0;
    }

    @Override
    public TicketType getTicketType() {
        return TicketType.ACCESS;
    }

    @Override
    public String getType() {
        return "Access request";
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
        details.put("systemName", systemName);
        details.put("accessLevel", accessLevel);
        details.put("escalationLevel", escalationLevel);
        details.put("escalation", escalationBadge());
        return details;
    }
}
