package com.javaplayground.iojsoncli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class S03JsonWithJackson {
  public static void main(String[] args) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path inputFile = Paths.get("src/main/resources/inp.json");

    Path outputFolder = Paths.get("output");
    Files.createDirectories(outputFolder);
    Path outputFile = outputFolder.resolve("op.json");

    // Deserialization: convert JSON from the input file into Java objects.
    List<InputJson> inputJsonList = mapper.readValue(
        inputFile.toFile(),
        new TypeReference<List<InputJson>>() {
        });

    // These JSON strings are also deserialized, so the mapping code stays the same.
    OutputJson type1OutputJson = mapper.readValue(OutputJson.HARDCODED_JSON_TYPE_1, OutputJson.class);
    OutputJson type2OutputJson = mapper.readValue(OutputJson.HARDCODED_JSON_TYPE_2, OutputJson.class);

    List<OutputJson> outputJsonList = inputJsonList.stream()
        .map(inputJson -> inputJson.type == 1 ? type1OutputJson : type2OutputJson)
        .toList();

    // Serialization: convert Java objects back into JSON and write them to a file.
    mapper.writeValue(outputFile.toFile(), outputJsonList);
  }

  public static class InputJson {
    public int type;
    public String name;
    public boolean active;
    public Map<String, String> details;
    public List<String> tags;
    public String note;

    public InputJson() {
    }
  }

  public static class OutputJson {
    public static final String HARDCODED_JSON_TYPE_1 = """
        {
          "outputType": 1,
          "message": "Created from hardcoded JSON type 1",
          "success": true,
          "extra": {
            "rule": "TYPE_1_RULE"
          },
          "items": [
            "alpha",
            "beta"
          ],
          "error": null
        }
        """;

    public static final String HARDCODED_JSON_TYPE_2 = """
        {
          "outputType": 2,
          "message": "Created from hardcoded JSON type 2",
          "success": false,
          "extra": {
            "rule": "TYPE_2_RULE"
          },
          "items": [
            "gamma",
            "delta"
          ],
          "error": null
        }
        """;

    public int outputType;
    public String message;
    public boolean success;
    public Map<String, String> extra;
    public List<String> items;
    public String error;

    public OutputJson() {
    }
  }
}

// Useful Jackson annotations:
// - @JsonProperty("emp_name") maps a JSON field to a differently named Java field.
// - @JsonIgnore excludes a field from serialization and deserialization.
// - @JsonIgnoreProperties(ignoreUnknown = true) ignores extra JSON fields not present in the class.
//
// Jackson usually needs a no-argument constructor because it creates an object first,
// then fills its fields from the JSON data.
