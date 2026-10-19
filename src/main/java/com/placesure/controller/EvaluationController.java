package com.placesure.controller;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.EvaluationDAO;

public final class EvaluationController {
  public EvaluationController(Router r, DatabaseConnection db, EvaluationDAO dao) {
    r.add(
        "GET",
        "/api/evaluations",
        c -> {
          try (var connection = db.open()) {
            c.data(dao.list(connection, c.query()));
          }
        });
    r.add(
        "GET",
        "/api/evaluations/(?<id>[0-9]+)",
        c -> {
          try (var connection = db.open()) {
            c.data(dao.get(connection, c.id()));
          }
        });
  }
}
