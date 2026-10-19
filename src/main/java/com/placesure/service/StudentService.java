package com.placesure.service;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.StudentDAO;
import com.placesure.model.*;
import com.placesure.util.*;
import java.sql.*;
import java.util.*;

public final class StudentService {
  private final DatabaseConnection db;
  private final StudentDAO dao;

  public StudentService(DatabaseConnection db, StudentDAO dao) {
    this.db = db;
    this.dao = dao;
  }

  public Student validate(Student s) {
    if (s == null)
      throw new ApiException(400, "VALIDATION_ERROR", "A student profile is required.");
    ValidationUtil v = new ValidationUtil();
    String name = v.required("fullName", s.fullName(), "Full name", 120),
        reg =
            v.required("registerNumber", s.registerNumber(), "Register number", 32)
                .toUpperCase(Locale.ROOT),
        email = v.required("email", s.email(), "Email", 160),
        department =
            v.required("department", s.department(), "Department", 30).toUpperCase(Locale.ROOT);
    v.check("fullName", name.length() >= 2, "Full name must contain at least 2 characters.");
    v.check(
        "registerNumber",
        reg.matches("[A-Z0-9][A-Z0-9._/\\-]{1,31}"),
        "Use 2–32 letters, numbers, dots, underscores, slashes, or hyphens.");
    v.check("email", email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter a valid email address.");
    v.check(
        "department",
        department.matches("[A-Z][A-Z0-9 &/\\-]{0,29}"),
        "Enter a valid department, for example CSE or IT.");
    v.year("graduationYear", s.graduationYear());
    v.decimal("cgpa", s.cgpa(), "CGPA");
    v.count("activeArrears", s.activeArrears(), "Active arrears");
    v.count("historyArrears", s.historyArrears(), "Historical arrears");
    String phone = ValidationUtil.optional(s.phone());
    v.check(
        "phone",
        phone == null
            || (phone.length() <= 24
                && phone.matches("\\+?[0-9 ()\\-]{7,24}")
                && phone.replaceAll("[^0-9]", "").length() >= 7
                && phone.replaceAll("[^0-9]", "").length() <= 15),
        "Enter a valid phone number or leave it blank.");
    List<String> skills = v.names("skills", s.skills(), 80);
    v.finish();
    return new Student(
        s.id(),
        reg,
        name,
        email,
        department,
        s.graduationYear(),
        s.cgpa().setScale(2),
        s.activeArrears(),
        s.historyArrears(),
        skills,
        phone,
        s.createdAt());
  }

  public Page<Student> list(QueryParams q) throws SQLException {
    try (Connection c = db.open()) {
      return dao.list(c, q);
    }
  }

  public Student get(long id) throws SQLException {
    try (Connection c = db.open()) {
      return dao.get(c, id);
    }
  }

  public Student create(Student s) throws SQLException {
    Student valid = validate(s);
    return JdbcUtil.transaction(db, c -> dao.create(c, valid));
  }

  public Student update(long id, Student s) throws SQLException {
    Student valid = validate(s);
    return JdbcUtil.transaction(db, c -> dao.update(c, id, valid));
  }

  public void delete(long id) throws SQLException {
    JdbcUtil.transaction(
        db,
        c -> {
          dao.delete(c, id);
          return null;
        });
  }
}
