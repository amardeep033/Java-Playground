package com.javaplayground.iojsoncli;

public class S04SmallCli {
    public static void main(String[] args) {
        // main receives command-line input as String[] args.
        int exitCode = run(args);
        System.exit(exitCode);
    }

    private static int run(String[] args) {
        // This program expects one required argument and one optional argument.
        if (args.length < 1 || args.length > 2) {
            printUsage();
            return 1;
        }

        StudyOption option;
        Mode mode;
        try {
            // Convert raw strings into enums so the rest of the program uses known values.
            option = StudyOption.fromValue(args[0]);
            mode = args.length == 2 ? Mode.fromValue(args[1]) : Mode.SUMMARY;
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsage();
            return 1;
        }

        System.out.println("Selected option: " + option.value);
        System.out.println("Mode: " + mode.displayName);

        if (mode == Mode.SUMMARY) {
            System.out.println(option.summary);
            return 0;
        }

        System.out.println(option.summary);
        System.out.println(option.detail);
        return 0;
    }

    private static void printUsage() {
        System.err.println("Usage: java S04SmallCli <option> [mode]");
        System.err.println("  option: 1, 2, or 3");
        System.err.println("  mode: summary or detail (default: summary)");
    }

    enum StudyOption {
        OPTION_1(1, "File IO selected.", "Classic IO uses FileReader, FileWriter, BufferedReader, and BufferedWriter."),
        OPTION_2(2, "NIO selected.", "NIO uses Path and Files for modern file operations."),
        OPTION_3(3, "JSON selected.", "Jackson maps JSON to Java objects and Java objects back to JSON.");

        private final int value;
        private final String summary;
        private final String detail;

        StudyOption(int value, String summary, String detail) {
            this.value = value;
            this.summary = summary;
            this.detail = detail;
        }

        static StudyOption fromValue(String rawValue) {
            for (StudyOption option : values()) {
                if (String.valueOf(option.value).equals(rawValue)) {
                    return option;
                }
            }
            throw new IllegalArgumentException("Option must be 1, 2, or 3.");
        }
    }

    enum Mode {
        SUMMARY("summary"),
        DETAIL("detail");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        static Mode fromValue(String rawValue) {
            for (Mode mode : values()) {
                if (mode.displayName.equalsIgnoreCase(rawValue)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Mode must be summary or detail.");
        }
    }
}

// Notes:
// - String[] args contains the command-line arguments passed to main.
// - Always check args.length before reading args[0], args[1], and so on.
// - Parsing early keeps the rest of the program working with safe enum values.
