package com.placesure.util;

import java.util.Map;

public final class ApiException extends RuntimeException {
  private final int status;
  private final String code;
  private final Map<String, String> fields;

  public ApiException(int status, String code, String message) {
    this(status, code, message, Map.of());
  }

  public ApiException(int status, String code, String message, Map<String, String> fields) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = Map.copyOf(fields);
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public Map<String, String> fields() {
    return fields;
  }

  public static ApiException notFound(String type) {
    return new ApiException(404, "NOT_FOUND", type + " was not found. It may have been deleted.");
  }
}
