package com.placesure.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.placesure.util.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

public final class Router implements HttpHandler {
  @FunctionalInterface
  public interface Action {
    void handle(RequestContext request) throws Exception;
  }

  private record Route(String method, Pattern pattern, Action action) {}

  private final List<Route> routes = new ArrayList<>();
  private static final Logger LOG = Logger.getLogger(Router.class.getName());

  public void add(String method, String path, Action action) {
    routes.add(new Route(method, Pattern.compile("^" + path + "/?$"), action));
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestId = UUID.randomUUID().toString().substring(0, 8);
    securityHeaders(exchange);
    exchange.getResponseHeaders().set("X-Request-ID", requestId);
    try {
      String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
      if (path.startsWith("/api/")) {
        verifyOrigin(exchange);
        boolean found = false;
        for (Route route : routes) {
          Matcher m = route.pattern.matcher(path);
          if (!m.matches()) continue;
          found = true;
          if (route.method.equals(method)) {
            route.action.handle(new RequestContext(exchange, m));
            return;
          }
        }
        throw new ApiException(
            found ? 405 : 404,
            found ? "METHOD_NOT_ALLOWED" : "NOT_FOUND",
            found
                ? "This action is not supported for this endpoint."
                : "The requested endpoint was not found.");
      }
      if (!method.equals("GET") && !method.equals("HEAD"))
        throw new ApiException(405, "METHOD_NOT_ALLOWED", "Use GET to open this resource.");
      serveStatic(exchange, path, method.equals("HEAD"));
    } catch (ApiException e) {
      sendError(exchange, e.status(), e.code(), e.getMessage(), e.fields(), requestId);
    } catch (JsonProcessingException e) {
      sendError(
          exchange,
          400,
          "INVALID_JSON",
          "The request contains invalid JSON or an invalid field type. Check numeric values and"
              + " required fields.",
          Map.of(),
          requestId);
    } catch (SQLException e) {
      LOG.warning(
          "Database request "
              + requestId
              + " failed (SQLState="
              + e.getSQLState()
              + ", vendorCode="
              + e.getErrorCode()
              + ")");
      if (e.getErrorCode() == 1062)
        sendError(
            exchange,
            409,
            "DUPLICATE_RECORD",
            "That register number is already in use. Use a unique register number.",
            Map.of("registerNumber", "This register number already exists."),
            requestId);
      else if (e.getSQLState() != null && e.getSQLState().startsWith("23"))
        sendError(
            exchange,
            409,
            "RECORD_CONFLICT",
            "This record conflicts with related data. Refresh the workspace and try again.",
            Map.of(),
            requestId);
      else if (e.getSQLState() != null && e.getSQLState().startsWith("08")
          || e.getErrorCode() == 1045
          || e.getErrorCode() == 1049
          || e.getErrorCode() == 1146)
        sendError(
            exchange,
            503,
            "DATABASE_UNAVAILABLE",
            "We couldn’t connect to the placement database. Check that MySQL is running and that"
                + " the local database configuration and schema are correct.",
            Map.of(),
            requestId);
      else
        sendError(
            exchange,
            500,
            "DATABASE_ERROR",
            "We couldn’t complete this database operation. No partial evaluation has been saved."
                + " Please try again.",
            Map.of(),
            requestId);
    } catch (IllegalArgumentException e) {
      sendError(
          exchange,
          400,
          "INVALID_REQUEST",
          "The request contains an invalid parameter or encoding.",
          Map.of(),
          requestId);
    } catch (Exception e) {
      LOG.severe("Request " + requestId + " failed: " + e.getClass().getSimpleName());
      sendError(
          exchange,
          500,
          "INTERNAL_ERROR",
          "We couldn’t complete this action. Please try again. Reference: " + requestId,
          Map.of(),
          requestId);
    } finally {
      exchange.close();
    }
  }

  private static void verifyOrigin(HttpExchange e) {
    if (Set.of("GET", "HEAD", "OPTIONS").contains(e.getRequestMethod())) return;
    String origin = e.getRequestHeaders().getFirst("Origin");
    if (origin == null) return; // CLI requests are supported; this is not authentication.
    try {
      String host = URI.create(origin).getRawAuthority(),
          requestHost = e.getRequestHeaders().getFirst("Host"),
          forwarded = e.getRequestHeaders().getFirst("X-Forwarded-Host");
      if (host != null
          && (host.equalsIgnoreCase(requestHost)
              || (forwarded != null && host.equalsIgnoreCase(forwarded.split(",")[0].trim()))))
        return;
    } catch (IllegalArgumentException ignored) {
    }
    throw new ApiException(
        403,
        "ORIGIN_REJECTED",
        "Cross-origin changes are not allowed. Open PlaceSure in its own workspace.");
  }

  private static void securityHeaders(HttpExchange e) {
    e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    e.getResponseHeaders().set("Referrer-Policy", "same-origin");
    e.getResponseHeaders().set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    e.getResponseHeaders()
        .set(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src"
                + " 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri"
                + " 'self'; form-action 'self'");
  }

  private static void sendError(
      HttpExchange e,
      int status,
      String code,
      String message,
      Map<String, String> fields,
      String requestId)
      throws IOException {
    byte[] bytes =
        Json.MAPPER.writeValueAsBytes(
            Map.of(
                "error",
                Map.of(
                    "code", code, "message", message, "fields", fields, "requestId", requestId)));
    e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    e.getResponseHeaders().set("Cache-Control", "no-store");
    e.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = e.getResponseBody()) {
      out.write(bytes);
    }
  }

  private void serveStatic(HttpExchange e, String path, boolean head) throws IOException {
    if (path.contains("..") || path.indexOf('\0') >= 0 || path.contains("\\"))
      throw new ApiException(404, "NOT_FOUND", "Resource not found.");
    String relative = path.equals("/") ? "index.html" : path.substring(1);
    byte[] bytes;
    String type;
    if (relative.startsWith("docs/")) {
      String name = relative.substring(5);
      if (!Set.of(
              "README.md",
              "VIVA.md",
              "API.md",
              "ARCHITECTURE.md",
              "TEST-RESULTS.md",
              "database.sql")
          .contains(name)) throw new ApiException(404, "NOT_FOUND", "Resource not found.");
      Path local =
          name.equals("README.md")
              ? Path.of(name)
              : name.equals("database.sql") ? Path.of("database", name) : Path.of("docs", name);
      bytes = read(local, "docs/" + name);
      type = "text/plain; charset=utf-8";
      e.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + name + "\"");
    } else {
      String ext = relative.contains(".") ? relative.substring(relative.lastIndexOf('.') + 1) : "";
      type =
          Map.of(
                  "html",
                  "text/html; charset=utf-8",
                  "css",
                  "text/css; charset=utf-8",
                  "js",
                  "text/javascript; charset=utf-8",
                  "svg",
                  "image/svg+xml",
                  "ttf",
                  "font/ttf",
                  "woff2",
                  "font/woff2",
                  "png",
                  "image/png",
                  "jpg",
                  "image/jpeg")
              .get(ext);
      if (type == null) throw new ApiException(404, "NOT_FOUND", "Resource not found.");
      bytes = read(Path.of("frontend", relative), "web/" + relative);
    }
    e.getResponseHeaders().set("Content-Type", type);
    e.getResponseHeaders()
        .set("Cache-Control", relative.endsWith(".ttf") ? "public, max-age=604800" : "no-cache");
    e.sendResponseHeaders(200, head ? -1 : bytes.length);
    if (!head)
      try (OutputStream out = e.getResponseBody()) {
        out.write(bytes);
      }
  }

  private byte[] read(Path path, String resource) throws IOException {
    if (Files.isRegularFile(path)) return Files.readAllBytes(path);
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (in == null) throw new ApiException(404, "NOT_FOUND", "Resource not found.");
      return in.readAllBytes();
    }
  }
}
