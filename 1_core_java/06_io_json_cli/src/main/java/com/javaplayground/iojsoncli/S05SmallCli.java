package com.javaplayground.iojsoncli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "study-cli", mixinStandardHelpOptions = true, description = "Small picocli example with one required value and one optional mode.")
public class S05SmallCli implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "option", description = "Allowed values: 1, 2, 3.", converter = StudyOptionConverter.class)
    private StudyOption option;

    @Parameters(index = "1", arity = "0..1", paramLabel = "mode", defaultValue = "summary", description = "Allowed values: summary, detail.", converter = ModeConverter.class)
    private Mode mode;

    public static void main(String[] args) {
        // Picocli parses args, validates parameters, and returns a process exit code.
        int exitCode = new CommandLine(new S05SmallCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
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

    static class StudyOptionConverter implements ITypeConverter<StudyOption> {
        @Override
        public StudyOption convert(String value) {
            // Picocli calls this converter for the first positional parameter.
            return StudyOption.fromValue(value);
        }
    }

    static class ModeConverter implements ITypeConverter<Mode> {
        @Override
        public Mode convert(String value) {
            // Picocli calls this converter for the optional second positional parameter.
            return Mode.fromValue(value);
        }
    }
}
