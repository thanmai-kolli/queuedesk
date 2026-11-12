package com.queuedesk.model;

/**
 * The three kinds of ticket. Used in requests ("type": "BUG") and as the value of the
 * ticket_type column, which tells Hibernate which Java class each row belongs to.
 */
public enum TicketType {
    BUG,
    FEATURE,
    ACCESS;

    // Which entity class stores this type. Used by the duplicate-ticket query.
    public Class<? extends Ticket> entityClass() {
        return switch (this) {
            case BUG -> BugTicket.class;
            case FEATURE -> FeatureRequestTicket.class;
            case ACCESS -> AccessRequestTicket.class;
        };
    }
}
