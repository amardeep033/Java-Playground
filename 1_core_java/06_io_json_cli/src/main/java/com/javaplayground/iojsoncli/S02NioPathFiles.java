package com.javaplayground.iojsoncli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class S02NioPathFiles {
    public static void main(String[] args) throws IOException {
        Path inputFolder = Paths.get("src/main/resources");
        Path inputFile1 = inputFolder.resolve("inp1.txt");
        Path inputFile2 = inputFolder.resolve("inp2.txt");

        Path outputFolder = Paths.get("output");
        Files.createDirectories(outputFolder);
        Path outputFile1 = outputFolder.resolve("op1.txt");
        Path outputFile2 = outputFolder.resolve("op2.txt");

        // Example 1: Files creates the reader and writer, but they still need try-with-resources.
        try (
                BufferedReader reader = Files.newBufferedReader(inputFile1);
                BufferedWriter writer = Files.newBufferedWriter(outputFile1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String modifiedLine = "Line by line modified: " + line.toUpperCase();
                writer.write(modifiedLine);
                writer.newLine();
            }
        }

        // Example 2: readString/writeString are convenient when the whole file can fit in memory.
        String bulkText = Files.readString(inputFile2);
        String modifiedBulkText = bulkText.replace("Line", "Bulk modified line");
        Files.writeString(outputFile2, modifiedBulkText);
    }
}

// Quick comparison:
// - java.io.File is the older API for representing files and directories.
// - java.nio.file.Path is the modern API for representing file-system paths.
// - Files provides utility methods such as copy, delete, move, readString, and writeString.
// - Prefer Path + Files for new code.
