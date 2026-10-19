package com.placesure.dao;

import com.placesure.model.*;
import com.placesure.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class CompanyDAO {
  private final SkillDAO skills = new SkillDAO();

  public Company get(Connection c, long id) throws SQLException {
    try (PreparedStatement p = c.prepareStatement("SELECT * FROM companies WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) throw ApiException.notFound("Company");
        return read(c, r);
      }
    }
  }

  private Company read(Connection c, ResultSet r) throws SQLException {
    long id = r.getLong("id");
    CompanyCriteria criteria = null;
    try (PreparedStatement p =
        c.prepareStatement("SELECT * FROM company_criteria WHERE company_id=?")) {
      p.setLong(1, id);
      try (ResultSet cr = p.executeQuery()) {
        if (cr.next())
          criteria =
              new CompanyCriteria(
                  cr.getBigDecimal("min_cgpa"),
                  cr.getInt("max_active_arrears"),
                  (Integer) cr.getObject("max_history_arrears"),
                  skills.load(c, "company", id),
                  loadDepartments(c, id),
                  loadYears(c, id));
      }
    }
    return new Company(
        id,
        r.getString("name"),
        r.getString("job_role"),
        r.getString("drive_date"),
        r.getString("salary_package"),
        r.getString("notes"),
        criteria,
        JdbcUtil.instant(r.getTimestamp("created_at")));
  }

  private List<String> loadDepartments(Connection c, long id) throws SQLException {
    List<String> result = new ArrayList<>();
    try (PreparedStatement p =
        c.prepareStatement(
            "SELECT department FROM company_departments WHERE company_id=? ORDER BY department")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) result.add(r.getString(1));
      }
    }
    return List.copyOf(result);
  }

  private List<Integer> loadYears(Connection c, long id) throws SQLException {
    List<Integer> result = new ArrayList<>();
    try (PreparedStatement p =
        c.prepareStatement(
            "SELECT graduation_year FROM company_graduation_years WHERE company_id=? ORDER BY"
                + " graduation_year")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) result.add(r.getInt(1));
      }
    }
    return List.copyOf(result);
  }

  public Page<Company> list(Connection c, QueryParams q) throws SQLException {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> values = new ArrayList<>();
    if (!q.text("q").isBlank()) {
      where.append(" AND (c.name LIKE ? ESCAPE '!' OR c.job_role LIKE ? ESCAPE '!')");
      values.add(JdbcUtil.like(q.text("q")));
      values.add(JdbcUtil.like(q.text("q")));
    }
    if (!q.text("department").isBlank()) {
      where.append(
          " AND (NOT EXISTS(SELECT 1 FROM company_departments d WHERE d.company_id=c.id) OR"
              + " EXISTS(SELECT 1 FROM company_departments d WHERE d.company_id=c.id AND"
              + " d.department=?))");
      values.add(q.text("department"));
    }
    BigDecimal max = q.decimal("maxCgpa");
    if (max != null) {
      where.append(" AND cc.min_cgpa<=?");
      values.add(max);
    }
    String sort = q.text("sort");
    if (sort.isBlank()) sort = "name";
    String column =
        Map.of(
                "name",
                "c.name",
                "driveDate",
                "c.drive_date",
                "minCgpa",
                "cc.min_cgpa",
                "createdAt",
                "c.created_at")
            .get(sort);
    if (column == null) throw QueryParams.invalid("Unsupported company sort field.");
    long total =
        JdbcUtil.scalar(
            c,
            "SELECT COUNT(*) FROM companies c LEFT JOIN company_criteria cc ON cc.company_id=c.id"
                + where,
            values);
    int size = q.size(),
        pages = Math.max(1, (int) Math.ceil((double) total / size)),
        page = Math.min(q.page(), pages);
    String sql =
        "SELECT c.* FROM companies c LEFT JOIN company_criteria cc ON cc.company_id=c.id"
            + where
            + " ORDER BY "
            + (sort.equals("driveDate") ? "c.drive_date IS NULL ASC, " : "")
            + column
            + " "
            + q.direction()
            + ",c.id ASC LIMIT ? OFFSET ?";
    List<Object> params = new ArrayList<>(values);
    params.add(size);
    params.add((page - 1) * size);
    List<Company> items = new ArrayList<>();
    try (PreparedStatement p = c.prepareStatement(sql)) {
      JdbcUtil.bind(p, params);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) items.add(read(c, r));
      }
    }
    return new Page<>(List.copyOf(items), page, size, total, pages);
  }

  public Company create(Connection c, Company company) throws SQLException {
    long id;
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO companies(name,job_role,drive_date,salary_package,notes)"
                + " VALUES(?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
      bind(p, company);
      p.executeUpdate();
      try (ResultSet keys = p.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("Company identifier unavailable.");
        id = keys.getLong(1);
      }
    }
    replaceCriteria(c, id, company.criteria());
    return get(c, id);
  }

  public Company update(Connection c, long id, Company company) throws SQLException {
    get(c, id);
    try (PreparedStatement p =
        c.prepareStatement(
            "UPDATE companies SET name=?,job_role=?,drive_date=?,salary_package=?,notes=? WHERE"
                + " id=?")) {
      bind(p, company);
      p.setLong(6, id);
      p.executeUpdate();
    }
    replaceCriteria(c, id, company.criteria());
    return get(c, id);
  }

  private void bind(PreparedStatement p, Company company) throws SQLException {
    p.setString(1, company.name());
    p.setString(2, company.jobRole());
    p.setString(3, company.driveDate());
    p.setString(4, company.salaryPackage());
    p.setString(5, company.notes());
  }

  public void replaceCriteria(Connection c, long id, CompanyCriteria r) throws SQLException {
    JdbcUtil.update(
        c,
        "INSERT INTO company_criteria(company_id,min_cgpa,max_active_arrears,max_history_arrears)"
            + " VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE"
            + " min_cgpa=VALUES(min_cgpa),max_active_arrears=VALUES(max_active_arrears),max_history_arrears=VALUES(max_history_arrears)",
        id,
        r.minCgpa(),
        r.maxActiveArrears(),
        r.maxHistoryArrears());
    skills.replace(c, "company", id, r.requiredSkills());
    JdbcUtil.update(c, "DELETE FROM company_departments WHERE company_id=?", id);
    try (PreparedStatement p =
        c.prepareStatement("INSERT INTO company_departments(company_id,department) VALUES(?,?)")) {
      for (String d : r.departments()) {
        p.setLong(1, id);
        p.setString(2, d);
        p.addBatch();
      }
      p.executeBatch();
    }
    JdbcUtil.update(c, "DELETE FROM company_graduation_years WHERE company_id=?", id);
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO company_graduation_years(company_id,graduation_year) VALUES(?,?)")) {
      for (Integer year : r.graduationYears()) {
        p.setLong(1, id);
        p.setInt(2, year);
        p.addBatch();
      }
      p.executeBatch();
    }
  }

  public void delete(Connection c, long id) throws SQLException {
    if (JdbcUtil.update(c, "DELETE FROM companies WHERE id=?", id) == 0)
      throw ApiException.notFound("Company");
  }
}
