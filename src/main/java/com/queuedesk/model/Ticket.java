package com.queuedesk.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
 *
 * Milestone 7 (JSON): the annotations below tell Jackson how to save and load tickets.
 *  - @JsonTypeInfo / @JsonSubTypes write a "type" property ("bug", "feature" or "access"),
 *    so loading creates the real subclass instead of failing on the abstract Ticket.
 *  - @JsonAutoDetect makes Jackson read and write the fields directly, not the getters,
 *    so getDetails(), getType() and the other getters are not saved as extra properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BugTicket.class, name = "bug"),
        @JsonSubTypes.Type(value = FeatureRequestTicket.class, name = "feature"),
        @JsonSubTypes.Type(value = AccessRequestTicket.class, name = "access")
})
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class Ticket implements Comparable<Ticket> {

    // static = one shared copy for the whole class, not one per ticket.
    private static int nextId = 1;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Instance fields = every ticket object has its own values.
    // id, title and requester have no setters, so they never change after creation.
    // They are not "final" only because Jackson must fill them in when loading tickets.json.
    private int id;
    private String title;
    private String description;
    private String requester;
    private Priority priority;
    private Status status;
    private LocalDateTime createdAt;
    private String assignee;                    // null means "not assigned yet"
    private Set<String> tags = new TreeSet<>(); // TreeSet: no duplicates, kept in A-Z order

    // Only for Jackson. It creates an empty ticket, then fills every field from tickets.json.
    // It does NOT take a new id: the ticket already has one saved in the file.
    protected Ticket() {
    }

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

    // How many tickets have ever been made. IDs start at 1 and go up by one,
    // so the next id minus one is the number already handed out.
    public static int totalCreated() {
        return nextId - 1;
    }

    // After loading tickets.json, new tickets must continue after the highest saved id,
    // otherwise the next new ticket would get id 1 again and clash with a loaded one.
    public static void continueIdsAfter(int highestUsedId) {
        if (highestUsedId >= nextId) {
            nextId = highestUsedId + 1;
        }
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

    // ----- setters: only description, priority and status (Milestone 2) -----
    // There is no setId(), setTitle() or setCreatedAt(): those never change after creation.

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

    // ----- Milestone 4: a small setter for assignee and an adder for tags -----

    public void setAssignee(String assignee) {
        this.assignee = isBlank(assignee) ? null : assignee.trim();
    }

    public void addTag(String tag) {
        if (isBlank(tag)) {
            throw new IllegalArgumentException("Tag cannot be empty");
        }
        tags.add(tag.trim().toLowerCase());
    }

    /**
     * Checks a ticket that was loaded from tickets.json.
     * Loading does not run the normal constructor, so its checks have to be repeated here.
     * Subclasses override this, call super.validate(), and check their own fields.
     */
    public void validate() {
        if (id < 1) {
            throw new IllegalArgumentException("id must be 1 or more");
        }
        if (isBlank(title)) {
            throw new IllegalArgumentException("title is missing");
        }
        if (isBlank(requester)) {
            throw new IllegalArgumentException("requester is missing");
        }
        if (priority == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("priority, status and createdAt are required");
        }
        if (createdAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("createdAt cannot be in the future");
        }
        if (description == null) {
            description = "";
        }
        if (isBlank(assignee)) {
            assignee = null;
        }
        // Jackson loads the tags into a plain HashSet; put them back into a sorted TreeSet.
        Set<String> loadedTags = tags == null ? Set.of() : tags;
        tags = new TreeSet<>();
        for (String tag : loadedTags) {
            addTag(tag);
        }
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
