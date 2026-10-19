package com.placesure.config;

import java.sql.*;
import java.util.Properties;

/** A short-lived JDBC connection per operation; credentials are never returned by an API. */
public final class DatabaseConnection {
  private final String url, user, password;

  public DatabaseConnection(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public static DatabaseConnection application(AppConfig config) {
    return new DatabaseConnection(
        config.get(
            "DB_URL",
            "jdbc:mysql://127.0.0.1:3306/placesure?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"),
        config.get("DB_USER", "placesure_app"),
        config.get("DB_PASSWORD", ""));
  }

  public static DatabaseConnection testing(AppConfig config) {
    String url = config.get("TEST_DB_URL", "");
    if (url.isBlank() || !url.contains("placesure_test"))
      throw new IllegalStateException(
          "Integration tests require TEST_DB_URL pointing to a separate placesure_test database.");
    return new DatabaseConnection(
        url, config.get("TEST_DB_USER", "placesure_app"), config.get("TEST_DB_PASSWORD", ""));
  }

  public Connection open() throws SQLException {
    Properties props = new Properties();
    props.setProperty("user", user);
    props.setProperty("password", password);
    props.setProperty("connectTimeout", "3000");
    props.setProperty("socketTimeout", "15000");
    props.setProperty("connectionTimeZone", "UTC");
    props.setProperty("forceConnectionTimeZoneToSession", "true");
    return DriverManager.getConnection(url, props);
  }
}
