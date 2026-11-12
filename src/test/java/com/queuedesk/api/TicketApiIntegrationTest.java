package com.queuedesk.api;

import com.jayway.jsonpath.JsonPath;
import com.queuedesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the whole app runs (controllers, validator, services, JPA) against a real
 * MySQL database, queuedesk_test (see src/test/resources/application-test.yml).
 * MockMvc sends HTTP requests to the controllers without starting a real web server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketApiIntegrationTest {

    private static final String LOGIN_BUG = """
            {
              "type": "BUG",
              "title": "Login fails",
              "description": "Clicking Login shows an error",
              "requester": "Asha",
              "priority": "HIGH",
              "severity": "MAJOR",
              "stepsToReproduce": "Open the login page and submit"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository repository;

    @BeforeEach
    void startWithNoTickets() {
        repository.deleteAll();
    }

    @Test
    void createReturns201AndTheTicketCanBeReadBack() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUG))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.estimatedEffortHours").value(4.0))
                .andExpect(jsonPath("$.details.severity").value("MAJOR"))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Login fails"));
    }

    @Test
    void missingFieldsReturn400WithAMessagePerField() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\": \"BUG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("title: is required")))
                .andExpect(jsonPath("$.errors", hasItem("requester: is required")));
    }

    @Test
    void aBugWithoutSeverityIsRejectedByTheValidator() throws Exception {
        String noSeverity = LOGIN_BUG.replace("\"severity\": \"MAJOR\",", "");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(noSeverity))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.startsWith("severity")));
    }

    @Test
    void theSameTicketTwiceReturns409() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUG))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUG))
                .andExpect(status().isConflict());
    }

    @Test
    void anUnknownTicketReturns404() throws Exception {
        mockMvc.perform(get("/api/tickets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ticket not found"));
    }

    @Test
    void anUpdateWithAnOldVersionReturns409() throws Exception {
        long id = createLoginBug();

        mockMvc.perform(patch("/api/tickets/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0, \"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Someone who still has version 0 tries to change it: refused, not silently overwritten.
        mockMvc.perform(patch("/api/tickets/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0, \"priority\": \"LOW\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aFeatureRequestCannotBeEscalated() throws Exception {
        String feature = """
                {"type": "FEATURE", "title": "Dark mode", "requester": "Mira", "priority": "LOW",
                 "votes": 3, "businessJustification": "Eyes hurt at night"}
                """;
        MvcResult created = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(feature))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post("/api/tickets/" + id + "/escalate").param("version", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ticketsComeBackOnePageAtATime() throws Exception {
        createLoginBug();
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BUG.replace("Login fails", "Search is slow"))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BUG.replace("Login fails", "Logout fails"))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void serveNextTakesTheTicketAndThenReturns204() throws Exception {
        createLoginBug();

        mockMvc.perform(post("/api/triage/serve-next").param("assignee", "Ravi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignee").value("Ravi"));

        mockMvc.perform(post("/api/triage/serve-next"))
                .andExpect(status().isNoContent());
    }

    @Test
    void theSimulatorChecksItsParameters() throws Exception {
        mockMvc.perform(get("/api/simulator").param("start", "8").param("count", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vip").value(1))
                .andExpect(jsonPath("$.priorityRechecks").value(1));

        mockMvc.perform(get("/api/simulator").param("start", "0").param("count", "4"))
                .andExpect(status().isBadRequest());
    }

    private long createLoginBug() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUG))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
