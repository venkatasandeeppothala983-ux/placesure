package com.placesure.service;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.CompanyDAO;
import com.placesure.model.*;
import com.placesure.util.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class CompanyService {
  private final DatabaseConnection db;
  private final CompanyDAO dao;

  public CompanyService(DatabaseConnection db, CompanyDAO dao) {
    this.db = db;
    this.dao = dao;
  }

  public CompanyCriteria validateCriteria(CompanyCriteria r) {
    if (r == null)
      throw new ApiException(
          400,
          "VALIDATION_ERROR",
          "Company criteria are required.",
          Map.of("criteria", "Configure minimum CGPA and maximum active arrears."));
    ValidationUtil v = new ValidationUtil();
    v.decimal("minCgpa", r.minCgpa(), "Minimum CGPA");
    v.count("maxActiveArrears", r.maxActiveArrears(), "Maximum active arrears");
    if (r.maxHistoryArrears() != null)
      v.count("maxHistoryArrears", r.maxHistoryArrears(), "Maximum historical arrears");
    List<String> skills = v.names("requiredSkills", r.requiredSkills(), 80);
    List<String> departments =
        v.names("departments", r.departments(), 30).stream()
            .map(s -> s.toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
    for (String d : departments)
      v.check(
          "departments",
          d.matches("[A-Z][A-Z0-9 &/\\-]{0,29}"),
          "Enter valid comma-separated departments.");
    List<Integer> years = new ArrayList<>();
    if (r.graduationYears() != null) {
      v.check(
          "graduationYears", r.graduationYears().size() <= 50, "Use at most 50 graduation years.");
      for (Integer year : r.graduationYears()) {
        v.year("graduationYears", year);
        if (year != null && !years.contains(year)) years.add(year);
      }
    }
    v.finish();
    return new CompanyCriteria(
        r.minCgpa().setScale(2),
        r.maxActiveArrears(),
        r.maxHistoryArrears(),
        skills,
        departments,
        List.copyOf(years));
  }

  public Company validate(Company company) {
    if (company == null)
      throw new ApiException(400, "VALIDATION_ERROR", "A company profile is required.");
    ValidationUtil v = new ValidationUtil();
    String name = v.required("name", company.name(), "Company name", 120),
        role = v.required("jobRole", company.jobRole(), "Job role", 120),
        drive = ValidationUtil.optional(company.driveDate()),
        salary = ValidationUtil.optional(company.salaryPackage()),
        notes = ValidationUtil.optional(company.notes());
    if (drive != null)
      try {
        LocalDate date = LocalDate.parse(drive);
        v.check(
            "driveDate",
            date.getYear() >= 2000 && date.getYear() <= 2100,
            "Drive date must be between 2000 and 2100.");
      } catch (DateTimeParseException e) {
        v.check("driveDate", false, "Use a valid date in YYYY-MM-DD format.");
      }
    v.check(
        "salaryPackage",
        salary == null || salary.length() <= 80,
        "Salary package must be at most 80 characters.");
    v.check(
        "notes",
        notes == null || notes.length() <= 2000,
        "Notes must be at most 2,000 characters.");
    v.finish();
    return new Company(
        company.id(),
        name,
        role,
        drive,
        salary,
        notes,
        validateCriteria(company.criteria()),
        company.createdAt());
  }

  public Page<Company> list(QueryParams q) throws SQLException {
    try (Connection c = db.open()) {
      return dao.list(c, q);
    }
  }

  public Company get(long id) throws SQLException {
    try (Connection c = db.open()) {
      return dao.get(c, id);
    }
  }

  public Company create(Company company) throws SQLException {
    Company valid = validate(company);
    return JdbcUtil.transaction(db, c -> dao.create(c, valid));
  }

  public Company update(long id, Company company) throws SQLException {
    Company valid = validate(company);
    return JdbcUtil.transaction(db, c -> dao.update(c, id, valid));
  }

  public Company updateCriteria(long id, CompanyCriteria criteria) throws SQLException {
    CompanyCriteria valid = validateCriteria(criteria);
    return JdbcUtil.transaction(
        db,
        c -> {
          dao.get(c, id);
          dao.replaceCriteria(c, id, valid);
          return dao.get(c, id);
        });
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
