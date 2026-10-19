package com.placesure.controller;

import com.placesure.util.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

public final class RequestContext {
  private final HttpExchange exchange;
  private final Matcher match;
  private final QueryParams query;

  public RequestContext(HttpExchange exchange, Matcher match) {
    this.exchange = exchange;
    this.match = match;
    Map<String, String> params = new LinkedHashMap<>();
    String raw = exchange.getRequestURI().getRawQuery();
    if (raw != null)
      for (String item : raw.split("&")) {
        if (item.isEmpty()) continue;
        String[] p = item.split("=", 2);
        String key = URLDecoder.decode(p[0], StandardCharsets.UTF_8),
            value = p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "";
        if (params.putIfAbsent(key, value) != null)
          throw QueryParams.invalid("Duplicate query parameter: " + key);
      }
    query = new QueryParams(params);
  }

  public QueryParams query() {
    return query;
  }

  public long id() {
    try {
      long id = Long.parseLong(match.group("id"));
      if (id < 1) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException e) {
      throw new ApiException(400, "INVALID_ID", "Enter a valid positive record ID.");
    }
  }

  public <T> T body(Class<T> type) throws IOException {
    String content = exchange.getRequestHeaders().getFirst("Content-Type");
    if (content == null || !content.split(";", 2)[0].trim().equalsIgnoreCase("application/json"))
      throw new ApiException(
          415, "UNSUPPORTED_MEDIA_TYPE", "Send this request as application/json.");
    byte[] bytes = exchange.getRequestBody().readNBytes(65537);
    if (bytes.length > 65536)
      throw new ApiException(
          413, "BODY_TOO_LARGE", "This request is too large. Use at most 64 KB.");
    if (bytes.length == 0)
      throw new ApiException(400, "INVALID_JSON", "A JSON request body is required.");
    return Json.MAPPER.readValue(bytes, type);
  }

  public void data(Object value) throws IOException {
    data(200, value);
  }

  public void data(int status, Object value) throws IOException {
    json(status, Collections.singletonMap("data", value));
  }

  public void json(int status, Object body) throws IOException {
    byte[] bytes = Json.MAPPER.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  public void csv(String filename, byte[] bytes) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
    exchange
        .getResponseHeaders()
        .set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
