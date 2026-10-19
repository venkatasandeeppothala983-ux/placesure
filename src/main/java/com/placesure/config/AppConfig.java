package com.placesure.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Loads a local .env without shell execution. OS environment variables take precedence. */
public final class AppConfig {
  private final Map<String, String> values;

  public AppConfig(Map<String, String> values) {
    this.values = Map.copyOf(values);
  }

  public static AppConfig load() {
    Map<String, String> values = new HashMap<>();
    Path path = Path.of(".env");
    if (Files.isRegularFile(path))
      try {
        for (String line : Files.readAllLines(path)) {
          line = line.trim();
          if (line.isBlank() || line.startsWith("#")) continue;
          int split = line.indexOf('=');
          if (split < 1) continue;
          String key = line.substring(0, split).trim(), value = line.substring(split + 1).trim();
          if (value.length() > 1
              && ((value.startsWith("\"") && value.endsWith("\""))
                  || (value.startsWith("'") && value.endsWith("'"))))
            value = value.substring(1, value.length() - 1);
          values.put(key, value);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read local configuration file.");
      }
    values.putAll(System.getenv());
    return new AppConfig(values);
  }

  public String get(String key, String fallback) {
    return values.getOrDefault(key, fallback);
  }

  public int port() {
    int p = Integer.parseInt(get("PORT", "8080"));
    if (p < 1 || p > 65535) throw new IllegalArgumentException("PORT must be 1–65535.");
    return p;
  }

  public String host() {
    return get("BIND_HOST", "0.0.0.0");
  }
}
