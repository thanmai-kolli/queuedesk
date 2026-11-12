package com.queuedesk.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The base class for every ticket, stored in the MySQL table "tickets".
 *
 * Milestone 2: fields, two constructors, getters, a few setters, ageInHours().
 * Month 2 (JPA): the annotations tell Hibernate how this class maps to a table.
 *
 *  @Entity            this class is stored in the database
 *  @Inheritance       all three ticket types share ONE table (SINGLE_TABLE)
 *  @DiscriminatorColumn  the column "ticket_type" says which subclass a row is (BUG, FEATURE, ACCESS)
 *  @UniqueConstraint  the database refuses two tickets with the same type, title and requester
 */
@Entity
@Table(name = "tickets",
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_type_title_requester",
                columnNames = {"ticket_type", "title", "requester"}))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ticket_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Ticket implements Comparable<Ticket> {

    // MySQL creates the id (AUTO_INCREMENT), so two requests can never get the same id.
    // This replaces Month 1's "private static int nextId".
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String requester;

    // EnumType.STRING stores "HIGH", not a number, so the table is readable.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String assignee;   // null means "not assigned yet"

    // Tags live in a second table, ticket_tags (ticket_id, tag).
    // EAGER: tags are loaded together with the ticket, so they can be read after the
    // database transaction has finished.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ticket_tags", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new HashSet<>();

    /**
     * Optimistic locking. Hibernate adds 1 every time the row is saved, and every UPDATE says
     * "... WHERE id = ? AND version = ?". If someone else saved the ticket in the meantime, the
     * version no longer matches, nothing is updated, and Hibernate throws an exception
     * instead of silently overwriting their change.
     */
    @Version
    private Long version;

    // JPA needs a constructor without parameters. Hibernate uses it when it reads a row,
    // then fills in the fields. protected: our own code uses the constructors below.
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

    // Every subclass says how many hours it needs, what type it is, and its own extra fields.
    public abstract double estimateEffortHours();

    public abstract TicketType getTicketType();

    public abstract String getType();

    public abstract Map<String, Object> typeDetails();

    public long ageInHours(LocalDateTime now) {
        return Duration.between(createdAt, now).toHours();
    }

    public long ageInHours() {
        return ageInHours(LocalDateTime.now());
    }

    // ----- getters -----

    public Long getId() {
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

    public Long getVersion() {
        return version;
    }

    // A sorted, read-only copy: other classes must use addTag() to change tags.
    public Set<String> getTags() {
        return Collections.unmodifiableSet(new TreeSet<>(tags));
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

    // Moves priority up one level: LOW -> MEDIUM -> HIGH -> CRITICAL. CRITICAL stays CRITICAL.
    protected void raisePriority() {
        if (priority != Priority.CRITICAL) {
            priority = Priority.values()[priority.ordinal() + 1];
        }
    }

    /**
     * Natural order used by the triage PriorityQueue:
     * higher priority first; if the priority is the same, the older ticket first; then the lower id.
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
        if (this.id == null || other.id == null) {
            return 0;   // not saved yet, so no id to compare
        }
        return Long.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return "#" + id + " [" + priority + "] " + title + " (" + status + ")";
    }

    protected static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
