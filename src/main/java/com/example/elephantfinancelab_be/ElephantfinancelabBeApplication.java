package com.example.elephantfinancelab_be;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ElephantfinancelabBeApplication {

  public static void main(String[] args) {
    loadDotenvIfPresent();
    SpringApplication.run(ElephantfinancelabBeApplication.class, args);
  }

  private static void loadDotenvIfPresent() {
    Path dotenv = Path.of(".env");
    if (!Files.exists(dotenv)) return;

    Map<String, String> values = new LinkedHashMap<>();
    try {
      for (String rawLine : Files.readAllLines(dotenv, StandardCharsets.UTF_8)) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        int idx = line.indexOf('=');
        if (idx <= 0) continue;
        String key = line.substring(0, idx).trim();
        String value = line.substring(idx + 1).trim();
        value = stripQuotes(value);
        if (!key.isEmpty()) values.put(key, value);
      }
    } catch (IOException e) {
      return;
    }

    for (var entry : values.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (System.getProperty(key) == null) {
        System.setProperty(key, value);
      }
    }
  }

  private static String stripQuotes(String value) {
    if (value == null) return "";
    if (value.length() >= 2) {
      char first = value.charAt(0);
      char last = value.charAt(value.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return value.substring(1, value.length() - 1);
      }
    }
    return value;
  }
}
