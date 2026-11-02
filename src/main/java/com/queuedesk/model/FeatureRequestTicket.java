package com.queuedesk.model;

/**
 * Milestone 3: a ticket asking for something new.
 * It is a Ticket but it is NOT Escalatable: feature requests wait for planning.
 */
public class FeatureRequestTicket extends Ticket {

    private final int votes;
    private final String businessJustification;

    public FeatureRequestTicket(String title, String description, String requester, Priority priority,
                                int votes, String businessJustification) {
        super(title, description, requester, priority);
        if (votes < 0) {
            throw new IllegalArgumentException("Votes cannot be negative");
        }
        if (isBlank(businessJustification)) {
            throw new IllegalArgumentException("Business justification is required for a feature request");
        }
        this.votes = votes;
        this.businessJustification = businessJustification.trim();
    }

    public int getVotes() {
        return votes;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    @Override
    public double estimateEffortHours() {
        return 8.0;
    }

    @Override
    public String getType() {
        return "Feature request";
    }

    @Override
    public String getDetails() {
        return super.getDetails()
                + formatLine("Votes", votes)
                + formatLine("Justification", businessJustification);
    }
}
