package com.queuedesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Milestone 3: a ticket asking for something new.
 * It is a Ticket but it is NOT Escalatable: feature requests wait for planning.
 * Stored in the "tickets" table with ticket_type = 'FEATURE'.
 */
@Entity
@DiscriminatorValue("FEATURE")
public class FeatureRequestTicket extends Ticket {

    @Column(name = "votes")
    private int votes;

    @Column(name = "business_justification", length = 2000)
    private String businessJustification;

    // For JPA only (see Ticket).
    protected FeatureRequestTicket() {
    }

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
    public TicketType getTicketType() {
        return TicketType.FEATURE;
    }

    @Override
    public String getType() {
        return "Feature request";
    }

    @Override
    public Map<String, Object> typeDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("votes", votes);
        details.put("businessJustification", businessJustification);
        return details;
    }
}
