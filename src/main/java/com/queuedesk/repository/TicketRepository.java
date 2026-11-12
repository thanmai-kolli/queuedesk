package com.queuedesk.repository;

import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Talks to the "tickets" table. There is no class that implements this interface:
 * Spring Data JPA writes the implementation when the app starts.
 *
 * JpaRepository<Ticket, Long> already gives: save, saveAll, findById (returns Optional),
 * findAll (also with paging and sorting), count, deleteById, existsById ...
 * It replaces Month 1's generic Repository<T extends Ticket>, which kept tickets in a List.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ----- Derived query methods: Spring builds the SQL from the method NAME -----

    // SELECT ... FROM tickets WHERE status = ?
    List<Ticket> findByStatus(Status status);

    // The same with paging: WHERE status = ? ... LIMIT ? OFFSET ?
    Page<Ticket> findByStatus(Status status, Pageable pageable);

    // WHERE UPPER(assignee) = UPPER(?)
    Page<Ticket> findByAssigneeIgnoreCase(String assignee, Pageable pageable);

    // WHERE status = ? AND UPPER(assignee) = UPPER(?)
    Page<Ticket> findByStatusAndAssigneeIgnoreCase(Status status, String assignee, Pageable pageable);

    // ----- @Query with JPQL: queries on Java classes and fields, not on tables -----

    // type(t) is the real subclass of the ticket. Used to refuse duplicate tickets.
    @Query("""
            select count(t) from Ticket t
            where type(t) = :type
              and lower(t.title) = lower(:title)
              and lower(t.requester) = lower(:requester)
            """)
    long countSameTicket(@Param("type") Class<? extends Ticket> type,
                         @Param("title") String title,
                         @Param("requester") String requester);

    // GROUP BY in the database: one row per status. StatusCount below is a "projection":
    // Spring fills it from the "as status" and "as total" names.
    @Query("select t.status as status, count(t) as total from Ticket t group by t.status")
    List<StatusCount> countByStatus();

    /**
     * Safe "take this ticket" for concurrent requests. It changes the ticket only if it is
     * still OPEN, in ONE update statement. If two requests try the same ticket at the same
     * moment, MySQL lets one of them update it; the other gets 0 rows back.
     * The version is increased so that anyone holding an older copy gets a conflict.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Ticket t
               set t.status = :inProgress,
                   t.assignee = coalesce(t.assignee, :assignee),
                   t.version = t.version + 1
             where t.id = :id and t.status = :open
            """)
    int startWorkIfOpen(@Param("id") Long id,
                        @Param("assignee") String assignee,
                        @Param("open") Status open,
                        @Param("inProgress") Status inProgress);

    // ----- @Query with native SQL: real MySQL SQL, written against the table and columns -----

    @Query(value = """
            SELECT COUNT(*) FROM tickets
            WHERE priority = 'CRITICAL'
              AND status IN ('OPEN', 'IN_PROGRESS')
              AND created_at < :cutoff
            """, nativeQuery = true)
    long countOverdueCritical(@Param("cutoff") LocalDateTime cutoff);

    interface StatusCount {
        Status getStatus();

        long getTotal();
    }
}
