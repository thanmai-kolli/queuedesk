package com.queuedesk;

import java.util.Scanner;

/**
 * Milestone 1: Queue Number Simulator.
 *
 * Serves customers in ticket-number order and keeps running totals:
 *  - every ticket number divisible by 5 goes to the VIP lane
 *  - every ticket number divisible by 10 also gets a priority recheck
 *
 * It is self-contained on purpose (it uses only Scanner, loops, if and %),
 * so it can run on its own or from option 8 in the QueueDesk menu.
 */
public class QueueNumberSimulator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        run(scanner);
    }

    public static void run(Scanner scanner) {
        System.out.println("=== Queue Number Simulator ===");
        int start = readNumber(scanner, "Starting ticket number (1 or more): ", 1);
        int count = readNumber(scanner, "How many customers to serve (0 or more): ", 0);

        // Use long for this check: start + count could be too big for an int and wrap around.
        if ((long) start + count - 1 > Integer.MAX_VALUE) {
            System.out.println("Those numbers go past the largest ticket number Java's int can hold. Try smaller numbers.");
            return;
        }

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

            System.out.println(message);
            System.out.println("    running totals -> served: " + served
                    + ", VIP: " + vipCount + ", rechecks: " + recheckCount);
        }

        System.out.println("Finished. Served " + served + " customers, " + vipCount
                + " in the VIP lane, " + recheckCount + " priority rechecks.");
    }

    private static int readNumber(Scanner scanner, String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(text);
                if (number >= min) {
                    return number;
                }
                System.out.println("Please enter a number of at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("'" + text + "' is not a whole number. Try again.");
            }
        }
    }
}
