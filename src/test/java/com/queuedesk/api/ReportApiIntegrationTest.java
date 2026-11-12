package com.queuedesk.api;

import com.queuedesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Overdue" means older than 24 hours, but a new ticket is 0 hours old. Waiting a day is not an
 * option in a test, so this test replaces the Clock bean with one that is two days ahead.
 * Every ticket then looks two days old to the report: that's why the Clock is a bean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiIntegrationTest {

    // A nested @TestConfiguration is picked up automatically. @Primary: when two Clock beans
    // exist, this one is injected.
    @TestConfiguration
    static class TwoDaysLater {
        @Bean
        @Primary
        Clock twoDaysLaterClock() {
            return Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(2));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository repository;

    @BeforeEach
    void startWithNoTickets() {
        repository.deleteAll();
    }

    @Test
    void aCriticalOpenTicketBecomesOverdue() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type": "BUG", "title": "Payments down", "requester": "Asha", "priority": "CRITICAL",
                         "severity": "BLOCKER", "stepsToReproduce": "Try to pay"}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/eod"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payments down (OPEN) - 48 hours old")));

        // The native SQL query in TicketRepository agrees with the Java report.
        mockMvc.perform(get("/api/analytics"))
                .andExpect(jsonPath("$.overdueCriticalFromDatabase").value(1));
    }
}
