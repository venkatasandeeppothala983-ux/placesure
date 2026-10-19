package com.placesure.dao;

import com.placesure.util.*;
import java.sql.*;
import java.util.*;

public final class ReportDAO {
  private final EvaluationDAO evaluations;

  public ReportDAO(EvaluationDAO evaluations) {
    this.evaluations = evaluations;
  }

  public List<Map<String, Object>> attempts(
      Connection c, QueryParams q, boolean latest, String forcedStatus) throws SQLException {
    EvaluationFilter f = EvaluationFilter.from(q, latest, forcedStatus);
    long count = JdbcUtil.scalar(c, "SELECT COUNT(*) FROM evaluations e" + f.where(), f.values());
    if (count > 10000)
      throw new ApiException(
          400,
          "REPORT_TOO_LARGE",
          "This report exceeds 10,000 evaluations. Narrow the company, student, or date filters and"
              + " try again.");
    return evaluations.query(
        c, "SELECT e.* FROM evaluations e" + f.where() + " ORDER BY e.id DESC", f.values());
  }

  public List<Map<String, Object>> failures(Connection c, QueryParams q) throws SQLException {
    EvaluationFilter f = EvaluationFilter.from(q, false, null);
    List<Map<String, Object>> rows = new ArrayList<>();
    String sql =
        "SELECT d.criterion_key,d.criterion,COUNT(*) AS failed_count FROM evaluation_details d JOIN"
            + " evaluations e ON e.id=d.evaluation_id"
            + f.where()
            + " AND d.passed=0 GROUP BY d.criterion_key,d.criterion ORDER BY failed_count"
            + " DESC,d.criterion";
    try (PreparedStatement p = c.prepareStatement(sql)) {
      JdbcUtil.bind(p, f.values());
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("criterion", r.getString("criterion"));
          row.put("failedCount", r.getLong("failed_count"));
          rows.add(row);
        }
      }
    }
    return rows;
  }

  public Map<String, Object> dashboard(Connection c) throws SQLException {
    Map<String, Object> d = new LinkedHashMap<>();
    d.put("totalStudents", JdbcUtil.scalar(c, "SELECT COUNT(*) FROM students", List.of()));
    d.put("totalCompanies", JdbcUtil.scalar(c, "SELECT COUNT(*) FROM companies", List.of()));
    d.put(
        "departmentCount",
        JdbcUtil.scalar(c, "SELECT COUNT(DISTINCT department) FROM students", List.of()));
    d.put("totalChecks", JdbcUtil.scalar(c, "SELECT COUNT(*) FROM evaluations", List.of()));
    d.put(
        "eligibleMatches",
        JdbcUtil.scalar(c, "SELECT COUNT(*) FROM evaluations WHERE eligible=1", List.of()));
    d.put(
        "recentEvaluations",
        evaluations.query(c, "SELECT * FROM evaluations ORDER BY id DESC LIMIT 6", List.of()));
    List<Map<String, Object>> companySummary = new ArrayList<>();
    String sql =
        "SELECT source.id,source.name,COUNT(e.id) total,COALESCE(SUM(e.eligible=1),0)"
            + " eligible,COALESCE(SUM(e.eligible=0),0) ineligible FROM (SELECT id,name FROM"
            + " companies UNION SELECT company_source_id id,MAX(company_name) name FROM evaluations"
            + " WHERE company_id IS NULL GROUP BY company_source_id) source LEFT JOIN evaluations e"
            + " ON e.company_source_id=source.id GROUP BY source.id,source.name ORDER BY source.id";
    try (PreparedStatement p = c.prepareStatement(sql);
        ResultSet r = p.executeQuery()) {
      while (r.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getLong("id"));
        row.put("name", r.getString("name"));
        row.put("total", r.getLong("total"));
        row.put("eligible", r.getLong("eligible"));
        row.put("ineligible", r.getLong("ineligible"));
        companySummary.add(row);
      }
    }
    d.put("companySummary", companySummary);
    List<Map<String, Object>> drives = new ArrayList<>();
    try (PreparedStatement p =
            c.prepareStatement(
                "SELECT id,name,job_role,drive_date FROM companies WHERE drive_date>=CURRENT_DATE"
                    + " ORDER BY drive_date,id LIMIT 3");
        ResultSet r = p.executeQuery()) {
      while (r.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getLong("id"));
        row.put("name", r.getString("name"));
        row.put("jobRole", r.getString("job_role"));
        row.put("driveDate", r.getString("drive_date"));
        drives.add(row);
      }
    }
    d.put("upcomingDrives", drives);
    List<Map<String, Object>> students = new ArrayList<>();
    try (PreparedStatement p =
            c.prepareStatement(
                "SELECT id,full_name,department,graduation_year FROM students ORDER BY created_at"
                    + " DESC,id DESC LIMIT 3");
        ResultSet r = p.executeQuery()) {
      while (r.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getLong("id"));
        row.put("fullName", r.getString("full_name"));
        row.put("department", r.getString("department"));
        row.put("graduationYear", r.getInt("graduation_year"));
        students.add(row);
      }
    }
    d.put("recentStudents", students);
    return d;
  }

  public Map<String, Object> meta(Connection c) throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("studentCount", JdbcUtil.scalar(c, "SELECT COUNT(*) FROM students", List.of()));
    m.put("companyCount", JdbcUtil.scalar(c, "SELECT COUNT(*) FROM companies", List.of()));
    List<String> departments = new ArrayList<>(), skills = new ArrayList<>();
    try (PreparedStatement p =
            c.prepareStatement(
                "SELECT department FROM students UNION SELECT department FROM evaluations ORDER BY"
                    + " department");
        ResultSet r = p.executeQuery()) {
      while (r.next()) departments.add(r.getString(1));
    }
    try (PreparedStatement p = c.prepareStatement("SELECT name FROM skills ORDER BY name");
        ResultSet r = p.executeQuery()) {
      while (r.next()) skills.add(r.getString(1));
    }
    m.put("departments", departments);
    m.put("skills", skills);
    List<Map<String, Object>> companies = new ArrayList<>();
    try (PreparedStatement p =
            c.prepareStatement(
                "SELECT id,name FROM companies UNION SELECT company_source_id id,MAX(company_name)"
                    + " name FROM evaluations WHERE company_id IS NULL GROUP BY company_source_id"
                    + " ORDER BY name");
        ResultSet r = p.executeQuery()) {
      while (r.next()) companies.add(Map.of("id", r.getLong(1), "name", r.getString(2)));
    }
    m.put("companies", companies);
    List<Map<String, Object>> students = new ArrayList<>();
    try (PreparedStatement p =
            c.prepareStatement(
                "SELECT id,full_name,register_number FROM students UNION SELECT student_source_id"
                    + " id,MAX(student_name) full_name,MAX(register_number) register_number FROM"
                    + " evaluations WHERE student_id IS NULL GROUP BY student_source_id ORDER BY"
                    + " full_name");
        ResultSet r = p.executeQuery()) {
      while (r.next())
        students.add(
            Map.of(
                "id", r.getLong(1), "fullName", r.getString(2), "registerNumber", r.getString(3)));
    }
    m.put("students", students);
    return m;
  }
}
