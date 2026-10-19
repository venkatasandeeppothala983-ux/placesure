package com.placesure.dao;

import com.placesure.util.*;
import java.time.LocalDate;
import java.util.*;

/** Centralized filters are shared by history and all reports. Dates are inclusive UTC dates. */
public record EvaluationFilter(String where, List<Object> values) {
  public static EvaluationFilter from(QueryParams q, boolean latest, String forcedStatus) {
    StringBuilder w = new StringBuilder(" WHERE 1=1");
    List<Object> values = new ArrayList<>();
    String search = q.text("q");
    if (!search.isBlank()) {
      w.append(" AND (e.student_name LIKE ? ESCAPE '!' OR e.register_number LIKE ? ESCAPE '!')");
      values.add(JdbcUtil.like(search));
      values.add(JdbcUtil.like(search));
    }
    Long company = q.id("companyId"), student = q.id("studentId");
    if (company != null) {
      w.append(" AND e.company_source_id=?");
      values.add(company);
    }
    if (student != null) {
      w.append(" AND e.student_source_id=?");
      values.add(student);
    }
    String department = q.text("department");
    if (!department.isBlank()) {
      w.append(" AND e.department=?");
      values.add(department);
    }
    LocalDate from = q.date("from"), to = q.date("to");
    if (from != null && to != null && from.isAfter(to))
      throw QueryParams.invalid("The start date cannot be later than the end date.");
    if (from != null) {
      w.append(" AND e.evaluated_at>=?");
      values.add(from.toString() + " 00:00:00");
    }
    if (to != null) {
      w.append(" AND e.evaluated_at<?");
      values.add(to.plusDays(1).toString() + " 00:00:00");
    }
    String status = forcedStatus == null ? q.text("status") : forcedStatus;
    if (!status.isBlank()) {
      if (!status.equals("eligible") && !status.equals("ineligible"))
        throw QueryParams.invalid("Status must be eligible or ineligible.");
      w.append(" AND e.eligible=?");
      values.add(status.equals("eligible"));
    }
    // Latest as of the date-range upper bound. Status is deliberately applied AFTER choosing the
    // latest.
    if (latest) {
      w.append(
          " AND NOT EXISTS(SELECT 1 FROM evaluations newer WHERE"
              + " newer.student_source_id=e.student_source_id AND"
              + " newer.company_source_id=e.company_source_id AND newer.id>e.id");
      if (to != null) {
        w.append(" AND newer.evaluated_at<?");
        values.add(to.plusDays(1).toString() + " 00:00:00");
      }
      w.append(")");
    }
    return new EvaluationFilter(w.toString(), List.copyOf(values));
  }
}
