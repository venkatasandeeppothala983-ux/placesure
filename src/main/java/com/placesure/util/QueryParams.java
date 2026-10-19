package com.placesure.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class QueryParams {
  private final Map<String, String> values;

  public QueryParams(Map<String, String> values) {
    this.values = values;
  }

  public String text(String key) {
    String v = values.getOrDefault(key, "").trim();
    if (v.length() > 200) throw invalid(key + " is too long.");
    return v;
  }

  public int integer(String key, int fallback, int min, int max) {
    String v = text(key);
    if (v.isBlank()) return fallback;
    try {
      int n = Integer.parseInt(v);
      if (n < min || n > max) throw invalid(key + " must be between " + min + " and " + max + ".");
      return n;
    } catch (NumberFormatException e) {
      throw invalid(key + " must be a whole number.");
    }
  }

  public Long id(String key) {
    String v = text(key);
    if (v.isBlank()) return null;
    try {
      long n = Long.parseLong(v);
      if (n < 1) throw invalid("Invalid " + key + ".");
      return n;
    } catch (NumberFormatException e) {
      throw invalid("Invalid " + key + ".");
    }
  }

  public BigDecimal decimal(String key) {
    String v = text(key);
    if (v.isBlank()) return null;
    try {
      BigDecimal n = new BigDecimal(v);
      if (n.compareTo(BigDecimal.ZERO) < 0
          || n.compareTo(BigDecimal.TEN) > 0
          || n.stripTrailingZeros().scale() > 2)
        throw invalid(key + " must be 0–10, with up to two decimal places.");
      return n;
    } catch (NumberFormatException e) {
      throw invalid("Invalid " + key + ".");
    }
  }

  public LocalDate date(String key) {
    String v = text(key);
    if (v.isBlank()) return null;
    try {
      LocalDate d = LocalDate.parse(v);
      if (d.getYear() < 2000 || d.getYear() > 2100)
        throw invalid(key + " must be between 2000 and 2100.");
      return d;
    } catch (DateTimeParseException e) {
      throw invalid(key + " must use YYYY-MM-DD.");
    }
  }

  public int page() {
    return integer("page", 1, 1, 1000000);
  }

  public int size() {
    return integer("size", 10, 1, 200);
  }

  public String direction() {
    String d = text("direction");
    if (d.isBlank()) return "DESC";
    if (!d.equalsIgnoreCase("asc") && !d.equalsIgnoreCase("desc"))
      throw invalid("Sort direction must be asc or desc.");
    return d.toUpperCase(Locale.ROOT);
  }

  public static ApiException invalid(String message) {
    return new ApiException(400, "INVALID_QUERY", message);
  }
}
