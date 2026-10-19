package com.placesure.dao;

import com.placesure.model.*;
import com.placesure.util.*;
import java.sql.*;
import java.util.*;

public final class EvaluationDAO {
  public Evaluation save(Connection c, Evaluation e) throws SQLException {
    if (c.getAutoCommit())
      throw new SQLException("Evaluation persistence requires an explicit transaction.");
    String sql =
        "INSERT INTO"
            + " evaluations(student_id,company_id,student_source_id,company_source_id,student_name,register_number,department,company_name,job_role,student_snapshot,company_snapshot,eligible,passed_count,failed_count)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    long id;
    try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      p.setLong(1, e.studentId());
      p.setLong(2, e.companyId());
      p.setLong(3, e.studentId());
      p.setLong(4, e.companyId());
      p.setString(5, e.studentName());
      p.setString(6, e.registerNumber());
      p.setString(7, e.department());
      p.setString(8, e.companyName());
      p.setString(9, e.jobRole());
      p.setString(10, Json.stringify(e.studentSnapshot()));
      p.setString(11, Json.stringify(e.companySnapshot()));
      p.setBoolean(12, e.eligible());
      p.setInt(13, e.passedCount());
      p.setInt(14, e.failedCount());
      p.executeUpdate();
      try (ResultSet keys = p.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("Evaluation identifier unavailable.");
        id = keys.getLong(1);
      }
    }
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO"
                + " evaluation_details(evaluation_id,criterion_key,criterion,requirement_snapshot,actual_snapshot,passed,explanation,position)"
                + " VALUES(?,?,?,?,?,?,?,?)")) {
      int position = 0;
      for (EvaluationDetail d : e.details()) {
        p.setLong(1, id);
        p.setString(2, d.key());
        p.setString(3, d.criterion());
        p.setString(4, d.requirement());
        p.setString(5, d.actual());
        p.setBoolean(6, d.passed());
        p.setString(7, d.explanation());
        p.setInt(8, position++);
        p.addBatch();
      }
      p.executeBatch();
    }
    return get(c, id);
  }

  public Evaluation get(Connection c, long id) throws SQLException {
    try (PreparedStatement p = c.prepareStatement("SELECT * FROM evaluations WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) throw ApiException.notFound("Evaluation");
        List<EvaluationDetail> details = new ArrayList<>();
        try (PreparedStatement dp =
            c.prepareStatement(
                "SELECT * FROM evaluation_details WHERE evaluation_id=? ORDER BY position")) {
          dp.setLong(1, id);
          try (ResultSet dr = dp.executeQuery()) {
            while (dr.next())
              details.add(
                  new EvaluationDetail(
                      dr.getString("criterion_key"),
                      dr.getString("criterion"),
                      dr.getString("requirement_snapshot"),
                      dr.getString("actual_snapshot"),
                      dr.getBoolean("passed"),
                      dr.getString("explanation")));
          }
        }
        return new Evaluation(
            id,
            r.getLong("student_source_id"),
            r.getLong("company_source_id"),
            r.getString("student_name"),
            r.getString("register_number"),
            r.getString("department"),
            r.getString("company_name"),
            r.getString("job_role"),
            r.getBoolean("eligible"),
            r.getInt("passed_count"),
            r.getInt("failed_count"),
            JdbcUtil.instant(r.getTimestamp("evaluated_at")),
            Json.parse(r.getString("student_snapshot"), Student.class),
            Json.parse(r.getString("company_snapshot"), Company.class),
            List.copyOf(details));
      }
    }
  }

  public Page<Map<String, Object>> list(Connection c, QueryParams q) throws SQLException {
    EvaluationFilter f = EvaluationFilter.from(q, false, null);
    long total = JdbcUtil.scalar(c, "SELECT COUNT(*) FROM evaluations e" + f.where(), f.values());
    int size = q.size(),
        pages = Math.max(1, (int) Math.ceil((double) total / size)),
        page = Math.min(q.page(), pages);
    List<Object> values = new ArrayList<>(f.values());
    values.add(size);
    values.add((page - 1) * size);
    List<Map<String, Object>> items =
        query(
            c,
            "SELECT e.* FROM evaluations e" + f.where() + " ORDER BY e.id DESC LIMIT ? OFFSET ?",
            values);
    return new Page<>(List.copyOf(items), page, size, total, pages);
  }

  public List<Map<String, Object>> query(Connection c, String sql, List<Object> values)
      throws SQLException {
    List<Map<String, Object>> result = new ArrayList<>();
    try (PreparedStatement p = c.prepareStatement(sql)) {
      JdbcUtil.bind(p, values);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) result.add(summary(r));
      }
    }
    return result;
  }

  public Map<String, Object> summary(ResultSet r) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", r.getLong("id"));
    row.put("studentId", r.getLong("student_source_id"));
    row.put("companyId", r.getLong("company_source_id"));
    row.put("studentName", r.getString("student_name"));
    row.put("registerNumber", r.getString("register_number"));
    row.put("department", r.getString("department"));
    row.put("companyName", r.getString("company_name"));
    row.put("jobRole", r.getString("job_role"));
    row.put("eligible", r.getBoolean("eligible"));
    row.put("passedCount", r.getInt("passed_count"));
    row.put("failedCount", r.getInt("failed_count"));
    row.put("evaluatedAt", JdbcUtil.instant(r.getTimestamp("evaluated_at")));
    return row;
  }
}
