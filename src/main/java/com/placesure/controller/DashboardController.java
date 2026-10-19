package com.placesure.controller;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.ReportDAO;
import com.placesure.util.JdbcUtil;
import java.util.Map;

public final class DashboardController {
  public DashboardController(Router r, DatabaseConnection db, ReportDAO dao) {
    r.add("GET", "/api/dashboard", c -> c.data(JdbcUtil.transaction(db, dao::dashboard)));
    r.add("GET", "/api/meta", c -> c.data(JdbcUtil.transaction(db, dao::meta)));
    r.add(
        "GET",
        "/api/health",
        c -> {
          try (var connection = db.open();
              var p = connection.prepareStatement("SELECT COUNT(*) FROM students");
              var rs = p.executeQuery()) {
            rs.next();
            String version = connection.getMetaData().getDatabaseProductVersion();
            String product =
                version.toLowerCase(java.util.Locale.ROOT).contains("mariadb")
                    ? "MariaDB"
                    : "MySQL";
            c.data(
                Map.of(
                    "status",
                    "ok",
                    "database",
                    "Connected",
                    "databaseProduct",
                    product + " " + version.split("-")[0],
                    "javaVersion",
                    System.getProperty("java.version"),
                    "version",
                    "1.0.0"));
          }
        });
  }
}
