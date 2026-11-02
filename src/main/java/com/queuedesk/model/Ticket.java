package com.queuedesk.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Milestone 2: the base class for every ticket.
 *
 * It is abstract because a "plain" ticket does not exist in QueueDesk:
 * every real ticket is a BugTicket, FeatureRequestTicket or AccessRequestTicket.
 */
public abstract class Ticket implements Comparable<Ticket> {

    // static = one shared copy for the whole class, not one per ticket.
    private static int nextId = 1;
    private static int totalCreated = 0;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Instance fields = every ticket object has its own values.
    private final int id;
    private final String title;
    private String description;
    private final String requester;
    private Priority priority;
    private Status status;
    private LocalDateTime createdAt;
    private String assignee;                          // null means "not assigned yet"
    private final Set<String> tags = new TreeSet<>(); // TreeSet: no duplicates, kept in A-Z order

    public Ticket(String title, String description, String requester, Priority priority) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (isBlank(requester)) {
            throw new IllegalArgumentException("Requester cannot be empty");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority is required");
        }

        this.id = nextId;
        nextId++;
        totalCreated++;

        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.requester = requester.trim();
        this.priority = priority;
        this.status = Status.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    // A shorter constructor: no description and MEDIUM priority.
    // this(...) calls the constructor above, so the checks are written only once.
    public Ticket(String title, String requester) {
        this(title, "", requester, Priority.MEDIUM);
    }

    // Every subclass must say how many hours it needs and what type it is.
    public abstract double estimateEffortHours();

    public abstract String getType();

    public long ageInHours() {
        return Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    public static int getTotalCreated() {
        return totalCreated;
    }

    // ----- getters -----

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequester() {
        return requester;
    }

    public Priority getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAssignee() {
        return assignee;
    }

    // Returns a read-only view, so other classes cannot add tags without addTag().
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    // ----- setters: only for fields that are allowed to change -----
    // There is no setId() or setTitle(): those never change after creation.

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void setPriority(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        this.priority = priority;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.status = status;
    }

    public void setAssignee(String assignee) {
        this.assignee = isBlank(assignee) ? null : assignee.trim();
    }

    public void addTag(String tag) {
        if (isBlank(tag)) {
            throw new IllegalArgumentException("Tag cannot be empty");
        }
        tags.add(tag.trim().toLowerCase());
    }

    // Only used when loading a ticket from a file that already has a creation time.
    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null || createdAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Created time cannot be empty or in the future");
        }
        this.createdAt = createdAt;
    }

    // Moves priority up one level: LOW -> MEDIUM -> HIGH -> CRITICAL. CRITICAL stays CRITICAL.
    protected void raisePriority() {
        if (priority != Priority.CRITICAL) {
            priority = Priority.values()[priority.ordinal() + 1];
        }
    }

    /**
     * Natural order used by the triage PriorityQueue:
     * higher priority first; if the priority is the same, the older ticket first.
     * Returns a negative number if this ticket should come first.
     */
    @Override
    public int compareTo(Ticket other) {
        // Enum compareTo says LOW < CRITICAL, so compare "other" with "this" to put CRITICAL first.
        int byPriority = other.priority.compareTo(this.priority);
        if (byPriority != 0) {
            return byPriority;
        }
        int byAge = this.createdAt.compareTo(other.createdAt);
        if (byAge != 0) {
            return byAge;
        }
        return Integer.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return "#" + id + " [" + priority + "] " + title + " (" + status + ")";
    }

    // All the details of the ticket, one per line. Subclasses add their own lines.
    public String getDetails() {
        return "Ticket #" + id + " - " + getType() + "\n"
                + formatLine("Title", title)
                + formatLine("Description", description.isEmpty() ? "(none)" : description)
                + formatLine("Requester", requester)
                + formatLine("Priority", priority)
                + formatLine("Status", status)
                + formatLine("Assigned to", assignee == null ? "Unassigned" : assignee)
                + formatLine("Tags", tags.isEmpty() ? "(none)" : String.join(", ", tags))
                + formatLine("Created at", createdAt.format(DISPLAY_FORMAT))
                + formatLine("Age", ageInHours() + " hours")
                + formatLine("Estimated effort", estimateEffortHours() + " hours");
    }

    protected static String formatLine(String label, Object value) {
        return String.format("  %-18s: %s\n", label, value);
    }

    protected static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
