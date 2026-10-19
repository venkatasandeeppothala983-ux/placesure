package com.placesure.dao;

import com.placesure.model.*;
import com.placesure.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class StudentDAO {
  private final SkillDAO skills = new SkillDAO();

  public Student get(Connection c, long id) throws SQLException {
    try (PreparedStatement p = c.prepareStatement("SELECT * FROM students WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) throw ApiException.notFound("Student");
        return read(c, r);
      }
    }
  }

  private Student read(Connection c, ResultSet r) throws SQLException {
    long id = r.getLong("id");
    return new Student(
        id,
        r.getString("register_number"),
        r.getString("full_name"),
        r.getString("email"),
        r.getString("department"),
        r.getInt("graduation_year"),
        r.getBigDecimal("cgpa"),
        r.getInt("active_arrears"),
        r.getInt("history_arrears"),
        skills.load(c, "student", id),
        r.getString("phone"),
        JdbcUtil.instant(r.getTimestamp("created_at")));
  }

  public Page<Student> list(Connection c, QueryParams q) throws SQLException {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> values = new ArrayList<>();
    if (!q.text("q").isBlank()) {
      where.append(" AND (full_name LIKE ? ESCAPE '!' OR register_number LIKE ? ESCAPE '!')");
      values.add(JdbcUtil.like(q.text("q")));
      values.add(JdbcUtil.like(q.text("q")));
    }
    if (!q.text("department").isBlank()) {
      where.append(" AND department=?");
      values.add(q.text("department"));
    }
    BigDecimal min = q.decimal("minCgpa");
    if (min != null) {
      where.append(" AND cgpa>=?");
      values.add(min);
    }
    if (!q.text("maxArrears").isBlank()) {
      where.append(" AND active_arrears<=?");
      values.add(q.integer("maxArrears", 0, 0, 100));
    }
    String sort = q.text("sort");
    if (sort.isBlank()) sort = "createdAt";
    String column =
        Map.of(
                "name",
                "full_name",
                "cgpa",
                "cgpa",
                "registerNumber",
                "register_number",
                "createdAt",
                "created_at",
                "graduationYear",
                "graduation_year")
            .get(sort);
    if (column == null) throw QueryParams.invalid("Unsupported student sort field.");
    long total = JdbcUtil.scalar(c, "SELECT COUNT(*) FROM students" + where, values);
    int size = q.size(),
        pages = Math.max(1, (int) Math.ceil((double) total / size)),
        page = Math.min(q.page(), pages);
    String sql =
        "SELECT * FROM students"
            + where
            + " ORDER BY "
            + column
            + " "
            + q.direction()
            + ", id "
            + q.direction()
            + " LIMIT ? OFFSET ?";
    List<Object> params = new ArrayList<>(values);
    params.add(size);
    params.add((page - 1) * size);
    List<Student> items = new ArrayList<>();
    try (PreparedStatement p = c.prepareStatement(sql)) {
      JdbcUtil.bind(p, params);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) items.add(read(c, r));
      }
    }
    return new Page<>(List.copyOf(items), page, size, total, pages);
  }

  public Student create(Connection c, Student s) throws SQLException {
    long id;
    String sql =
        "INSERT INTO"
            + " students(register_number,full_name,email,department,graduation_year,cgpa,active_arrears,history_arrears,phone)"
            + " VALUES(?,?,?,?,?,?,?,?,?)";
    try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(p, s);
      p.executeUpdate();
      try (ResultSet keys = p.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("Student identifier unavailable.");
        id = keys.getLong(1);
      }
    }
    skills.replace(c, "student", id, s.skills());
    return get(c, id);
  }

  public Student update(Connection c, long id, Student s) throws SQLException {
    get(c, id);
    try (PreparedStatement p =
        c.prepareStatement(
            "UPDATE students SET"
                + " register_number=?,full_name=?,email=?,department=?,graduation_year=?,cgpa=?,active_arrears=?,history_arrears=?,phone=?"
                + " WHERE id=?")) {
      bind(p, s);
      p.setLong(10, id);
      p.executeUpdate();
    }
    skills.replace(c, "student", id, s.skills());
    return get(c, id);
  }

  private void bind(PreparedStatement p, Student s) throws SQLException {
    p.setString(1, s.registerNumber());
    p.setString(2, s.fullName());
    p.setString(3, s.email());
    p.setString(4, s.department());
    p.setInt(5, s.graduationYear());
    p.setBigDecimal(6, s.cgpa());
    p.setInt(7, s.activeArrears());
    p.setInt(8, s.historyArrears());
    p.setString(9, s.phone());
  }

  public void delete(Connection c, long id) throws SQLException {
    if (JdbcUtil.update(c, "DELETE FROM students WHERE id=?", id) == 0)
      throw ApiException.notFound("Student");
  }
}
