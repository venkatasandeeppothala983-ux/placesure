package com.placesure.model;

import java.util.List;
import java.util.Map;

public record Report(
    String title,
    String generatedAt,
    String scope,
    List<Column> columns,
    List<Map<String, Object>> rows,
    Map<String, Object> summary) {
  public record Column(String key, String label) {}
}
