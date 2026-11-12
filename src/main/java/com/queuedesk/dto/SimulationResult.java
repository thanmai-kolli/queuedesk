package com.queuedesk.dto;

import java.util.List;

/**
 * GET /api/simulator: Milestone 1's queue simulation, returned as JSON instead of printed.
 */
public record SimulationResult(
        int start,
        int count,
        List<String> lines,
        int served,
        int vip,
        int priorityRechecks) {
}
