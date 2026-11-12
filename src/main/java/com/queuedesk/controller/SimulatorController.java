package com.queuedesk.controller;

import com.queuedesk.dto.SimulationResult;
import com.queuedesk.service.QueueSimulatorService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/simulator?start=8&count=4  ->  Milestone 1's queue simulation as JSON.
 * The @Min/@Max annotations on the parameters are checked before the method runs.
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final QueueSimulatorService simulatorService;

    public SimulatorController(QueueSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @GetMapping
    public SimulationResult simulate(
            @RequestParam @Min(value = 1, message = "must be at least 1") int start,
            @RequestParam @Min(value = 0, message = "cannot be negative")
            @Max(value = 1000, message = "must be at most 1000") int count) {
        return simulatorService.simulate(start, count);
    }
}
