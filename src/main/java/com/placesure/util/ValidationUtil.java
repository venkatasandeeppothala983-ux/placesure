package com.placesure.util;

import java.math.BigDecimal;
import java.util.*;

/**
 * Collects field-specific validation errors so the UI can explain more than one issue at a time.
 */
public final class ValidationUtil {
  private final Map<String, String> errors = new LinkedHashMap<>();

  public static String clean(String s) {
    return s == null ? "" : s.strip().replaceAll("\\s+", " ");
  }

  public static String optional(String s) {
    String v = clean(s);
    return v.isBlank() ? null : v;
  }

  public static String key(String s) {
    return clean(s).toLowerCase(Locale.ROOT);
  }

  public String required(String key, String value, String label, int max) {
    String v = clean(value);
    if (v.isBlank()) errors.put(key, label + " is required.");
    else if (v.length() > max) errors.put(key, label + " must be at most " + max + " characters.");
    return v;
  }

  public void check(String key, boolean condition, String message) {
    if (!condition) errors.putIfAbsent(key, message);
  }

  public void decimal(String key, BigDecimal n, String label) {
    check(
        key,
        n != null
            && n.compareTo(BigDecimal.ZERO) >= 0
            && n.compareTo(BigDecimal.TEN) <= 0
            && n.stripTrailingZeros().scale() <= 2,
        label + " must be between 0.00 and 10.00 with at most two decimal places.");
  }

  public void count(String key, Integer n, String label) {
    check(key, n != null && n >= 0 && n <= 100, label + " must be a whole number from 0 to 100.");
  }

  public void year(String key, Integer n) {
    check(
        key, n != null && n >= 2000 && n <= 2100, "Graduation year must be between 2000 and 2100.");
  }

  public List<String> names(String field, List<String> values, int maxLength) {
    if (values == null) return List.of();
    check(field, values.size() <= 50, "Use at most 50 values.");
    Map<String, String> normalized = new LinkedHashMap<>();
    for (String item : values) {
      String value = clean(item);
      check(
          field,
          !value.isBlank() && value.length() <= maxLength,
          "Each value must contain 1–" + maxLength + " characters.");
      if (!value.isBlank()) normalized.putIfAbsent(key(value), value);
    }
    return List.copyOf(normalized.values());
  }

  public void finish() {
    if (!errors.isEmpty())
      throw new ApiException(
          400, "VALIDATION_ERROR", "Please correct the highlighted fields.", errors);
  }
}
