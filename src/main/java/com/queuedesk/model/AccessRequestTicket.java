package com.queuedesk.model;

/**
 * Milestone 3: a ticket asking for access to a system.
 * It IS a Ticket and it CAN be escalated.
 */
public class AccessRequestTicket extends Ticket implements Escalatable {

    private String systemName;
    private String accessLevel;
    private int escalationLevel = 0;

    // Only for Jackson when loading tickets.json (see the note in Ticket).
    private AccessRequestTicket() {
        super();
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
    public void validate() {
        super.validate();
        if (isBlank(systemName)) {
            throw new IllegalArgumentException("systemName is missing");
        }
        if (isBlank(accessLevel)) {
            throw new IllegalArgumentException("accessLevel is missing");
        }
        if (escalationLevel < 0) {
            throw new IllegalArgumentException("escalationLevel cannot be negative");
        }
    }

    @Override
    public String getDetails() {
        return super.getDetails()
                + formatLine("System", systemName)
                + formatLine("Access level", accessLevel)
                + formatLine("Escalation", escalationBadge());
    }
}
