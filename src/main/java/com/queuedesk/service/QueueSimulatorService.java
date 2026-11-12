package com.queuedesk.service;

import com.queuedesk.dto.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 1's queue simulation as a service, for GET /api/simulator.
 * The original single-file console version (QueueNumberSimulator) still runs on its own;
 * this one returns the lines instead of reading the keyboard and printing.
 */
@Service
public class QueueSimulatorService {

    public SimulationResult simulate(int start, int count) {
        // Use long for this check: start + count could be too big for an int and wrap around.
        if ((long) start + count - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("start + count goes past the largest ticket number an int can hold");
        }

        List<String> lines = new ArrayList<>();
        int served = 0;
        int vipCount = 0;
        int recheckCount = 0;

        for (int i = 0; i < count; i++) {
            int ticketNumber = start + i;
            String message = "Now serving ticket #" + ticketNumber;

            // Two separate ifs (not else-if): a number like 10 matches BOTH rules.
            if (ticketNumber % 5 == 0) {
                vipCount++;
                message += "  [VIP lane]";
            }
            if (ticketNumber % 10 == 0) {
                recheckCount++;
                message += "  [priority recheck]";
            }
            served++;
            lines.add(message);
        }
        return new SimulationResult(start, count, lines, served, vipCount, recheckCount);
    }
}
