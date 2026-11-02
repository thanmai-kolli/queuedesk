package com.queuedesk.util;

import java.util.Scanner;

/**
 * Reads input from the keyboard and keeps asking until the answer is valid,
 * so a typing mistake never crashes the program.
 *
 * Everything is read as a whole line with nextLine(). Mixing nextInt() and nextLine()
 * leaves the Enter key in the input and causes skipped questions.
 */
public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = scanner.nextLine().trim();
            if (!answer.isEmpty()) {
                return answer;
            }
            System.out.println("  This is required. Please type something.");
        }
    }

    // Returns "" if the user just presses Enter.
    public String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String answer = readRequired(prompt);
            try {
                int value = Integer.parseInt(answer);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  '" + answer + "' is not a whole number. Try again.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String answer = readRequired(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("  Please type y or n.");
        }
    }

    // A generic method: shows a numbered list of any enum's values and returns the one chosen.
    public <E extends Enum<E>> E readChoice(String title, E[] options) {
        System.out.println(title);
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        int choice = readInt("Choose 1-" + options.length + ": ", 1, options.length);
        return options[choice - 1];
    }
}
