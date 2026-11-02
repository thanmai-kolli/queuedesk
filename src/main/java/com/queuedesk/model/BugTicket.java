package com.queuedesk.model;

/**
 * Milestone 3: a ticket for something that is broken.
 * It IS a Ticket (extends) and it CAN be escalated (implements Escalatable).
 */
public class BugTicket extends Ticket implements Escalatable {

    private final Severity severity;
    private final String stepsToReproduce;
    private int escalationLevel = 0;

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
    public String getType() {
        return "Bug";
    }

    @Override
    public void escalate() {
        escalationLevel++;
        raisePriority();
    }

    @Override
    public int getEscalationLevel() {
        return escalationLevel;
    }

    @Override
    public String getDetails() {
        return super.getDetails()
                + formatLine("Severity", severity)
                + formatLine("Steps to reproduce", stepsToReproduce)
                + formatLine("Escalation", escalationLevel == 0 ? "Not escalated" : "Level " + escalationLevel);
    }
}
