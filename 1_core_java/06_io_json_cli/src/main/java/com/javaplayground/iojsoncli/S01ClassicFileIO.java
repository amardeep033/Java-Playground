package com.javaplayground.iojsoncli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class S01ClassicFileIO {
    public static void main(String[] args) throws IOException {
        File inputFolder = new File("src/main/resources");
        File inputFile1 = new File(inputFolder, "inp1.txt");
        File inputFile2 = new File(inputFolder, "inp2.txt");

        File outputFolder = new File("output");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        File outputFile1 = new File(outputFolder, "op1.txt");
        File outputFile2 = new File(outputFolder, "op2.txt");

        // Example 1: read one line at a time, transform it, and write each line.
        try (
                BufferedReader reader = new BufferedReader(new FileReader(inputFile1));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile1))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String modifiedLine = "Line by line modified: " + line.toUpperCase();
                writer.write(modifiedLine);
                writer.newLine();
            }
        }

        // Example 2: read the whole file in chunks, transform the full text, and write it once.
        StringBuilder bulkText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile2))) {
            char[] buffer = new char[1024];
            int characterCount;
            while ((characterCount = reader.read(buffer)) != -1) {
                bulkText.append(buffer, 0, characterCount);
            }
        }
        String modifiedBulkText = bulkText.toString().replace("Line", "Bulk modified line");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile2))) {
            writer.write(modifiedBulkText);
        }
    }
}
