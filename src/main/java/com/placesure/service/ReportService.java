package com.placesure.service;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.*;
import com.placesure.model.*;
import com.placesure.util.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class ReportService {
  private final DatabaseConnection db;
  private final ReportDAO dao;
  private final EvaluationDAO evaluations;

  public ReportService(DatabaseConnection db, ReportDAO dao, EvaluationDAO evaluations) {
    this.db = db;
    this.dao = dao;
    this.evaluations = evaluations;
  }

  public Report generate(QueryParams q) throws SQLException {
    // Repeatable-read ensures report rows and aggregates share the same database snapshot.
    return JdbcUtil.transaction(db, c -> generate(c, q));
  }

  private Report generate(Connection c, QueryParams q) throws SQLException {
    String type = q.text("type");
    if (type.isBlank()) type = "summary";
    if (!Set.of("summary", "student", "eligible", "ineligible", "failures", "history")
        .contains(type)) throw QueryParams.invalid("Choose a valid report type.");
    boolean latest = Set.of("student", "eligible", "ineligible").contains(type);
    String forced =
        type.equals("eligible") ? "eligible" : type.equals("ineligible") ? "ineligible" : null;
    List<Map<String, Object>> attempts = dao.attempts(c, q, latest, forced);
    long eligible = attempts.stream().filter(r -> Boolean.TRUE.equals(r.get("eligible"))).count();
    Map<String, Object> summary =
        Map.of(
            "total",
            attempts.size(),
            "eligible",
            eligible,
            "ineligible",
            attempts.size() - eligible,
            "basis",
            latest ? "LATEST MATCHES" : "EVALUATION ATTEMPTS");
    List<Map<String, Object>> rows = new ArrayList<>();
    List<Report.Column> columns;
    String title;
    String scope =
        latest
            ? "Latest saved evaluation per original student–company pair, as of the selected end"
                  + " date. No repeated attempts are counted twice."
            : "All saved evaluation attempts in the selected range. Repeated checks are counted"
                  + " separately.";
    if (type.equals("summary")) {
      title = "Overall eligibility summary";
      columns =
          cols(
              "companyName",
              "Company",
              "total",
              "Evaluations",
              "eligible",
              "Eligible",
              "ineligible",
              "Not eligible",
              "matchRate",
              "Match rate");
      Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
      for (Map<String, Object> e : attempts) {
        long key = ((Number) e.get("companyId")).longValue();
        Map<String, Object> row =
            grouped.computeIfAbsent(
                key,
                k -> {
                  Map<String, Object> n = new LinkedHashMap<>();
                  n.put("companyName", e.get("companyName"));
                  n.put("total", 0);
                  n.put("eligible", 0);
                  n.put("ineligible", 0);
                  return n;
                });
        row.put("total", (int) row.get("total") + 1);
        String status = Boolean.TRUE.equals(e.get("eligible")) ? "eligible" : "ineligible";
        row.put(status, (int) row.get(status) + 1);
      }
      for (Map<String, Object> row : grouped.values()) {
        row.put(
            "matchRate",
            Math.round(100.0 * (int) row.get("eligible") / (int) row.get("total")) + "%");
        rows.add(row);
      }
    } else if (type.equals("failures")) {
      title = "Failed-criterion analysis";
      columns =
          cols(
              "criterion",
              "Criterion",
              "failedCount",
              "Failed checks",
              "share",
              "Share of evaluations");
      rows.addAll(dao.failures(c, q));
      for (Map<String, Object> row : rows)
        row.put(
            "share",
            (attempts.isEmpty()
                    ? 0
                    : Math.round(
                        100.0 * ((Number) row.get("failedCount")).longValue() / attempts.size()))
                + "%");
      scope +=
          " A single attempt may fail several criteria, so criterion percentages can add up to more"
              + " than 100%.";
    } else {
      title =
          switch (type) {
            case "student" -> "Student-wise eligibility report";
            case "eligible" -> "Company-wise eligible students";
            case "ineligible" -> "Company-wise ineligible students";
            default -> "Evaluation history report";
          };
      columns =
          cols(
              "evaluationId",
              "Evaluation ID",
              "studentName",
              "Student",
              "registerNumber",
              "Register number",
              "department",
              "Department",
              "companyName",
              "Company",
              "jobRole",
              "Job role",
              "result",
              "Result",
              "passedCount",
              "Passed",
              "failedCount",
              "Failed",
              "evaluatedAt",
              "Evaluated at (UTC)");
      for (Map<String, Object> e : attempts) {
        Map<String, Object> row = new LinkedHashMap<>(e);
        row.put("evaluationId", "PS-" + String.format("%04d", e.get("id")));
        row.put("result", Boolean.TRUE.equals(e.get("eligible")) ? "Eligible" : "Not eligible");
        rows.add(row);
      }
    }
    List<String> filters = new ArrayList<>();
    for (String key : List.of("q", "companyId", "studentId", "department", "status", "from", "to"))
      if (!q.text(key).isBlank()) filters.add(key + "=" + q.text(key));
    scope +=
        " Filters: "
            + (filters.isEmpty() ? "none (all records)" : String.join("; ", filters))
            + ". Date boundaries are inclusive UTC dates.";
    return new Report(title, Instant.now().toString(), scope, columns, List.copyOf(rows), summary);
  }

  public Report evaluation(long id) throws SQLException {
    try (Connection c = db.open()) {
      Evaluation e = evaluations.get(c, id);
      List<Map<String, Object>> rows = new ArrayList<>();
      for (EvaluationDetail d : e.details()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("criterion", d.criterion());
        row.put("actual", d.actual());
        row.put("requirement", d.requirement());
        row.put("result", d.passed() ? "PASS" : "FAIL");
        row.put("explanation", d.explanation());
        rows.add(row);
      }
      String scope =
          "Evaluation PS-"
              + String.format("%04d", e.id())
              + " | "
              + e.studentName()
              + " | "
              + e.registerNumber()
              + " | "
              + e.companyName()
              + " | "
              + e.jobRole()
              + " | "
              + (e.eligible() ? "ELIGIBLE" : "NOT ELIGIBLE")
              + " | Evaluated "
              + e.evaluatedAt()
              + ". Uses immutable student and criteria snapshots. Eligibility is not an offer"
              + " guarantee.";
      return new Report(
          "Placement eligibility report",
          Instant.now().toString(),
          scope,
          cols(
              "criterion",
              "Criterion",
              "actual",
              "Student value",
              "requirement",
              "Company requirement",
              "result",
              "Result",
              "explanation",
              "Explanation"),
          List.copyOf(rows),
          Map.of("total", 1, "eligible", e.eligible() ? 1 : 0));
    }
  }

  private static List<Report.Column> cols(String... pairs) {
    List<Report.Column> result = new ArrayList<>();
    for (int i = 0; i < pairs.length; i += 2) result.add(new Report.Column(pairs[i], pairs[i + 1]));
    return List.copyOf(result);
  }
}
