package com.placesure.service;

import com.placesure.config.DatabaseConnection;
import com.placesure.dao.*;
import com.placesure.model.*;
import com.placesure.util.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The single source of truth for eligibility. The pure rule method is independently unit-testable.
 */
public final class EligibilityService {
  private final DatabaseConnection db;
  private final StudentDAO students;
  private final CompanyDAO companies;
  private final EvaluationDAO evaluations;

  public EligibilityService() {
    this(null, new StudentDAO(), new CompanyDAO(), new EvaluationDAO());
  }

  public EligibilityService(
      DatabaseConnection db, StudentDAO students, CompanyDAO companies, EvaluationDAO evaluations) {
    this.db = db;
    this.students = students;
    this.companies = companies;
    this.evaluations = evaluations;
  }

  /** A consistent database snapshot and an atomic write of the summary and all detail rows. */
  public Evaluation evaluate(long studentId, long companyId) throws SQLException {
    if (studentId < 1 || companyId < 1)
      throw new ApiException(400, "INVALID_ID", "Select a valid student and company.");
    if (db == null)
      throw new IllegalStateException("Database configuration is required to persist evaluations.");
    return JdbcUtil.transaction(
        db,
        c -> {
          Student student = students.get(c, studentId);
          Company company = companies.get(c, companyId);
          return evaluations.save(c, evaluateRules(student, company));
        });
  }

  public Evaluation evaluateRules(Student s, Company company) {
    List<EvaluationDetail> details = new ArrayList<>();
    CompanyCriteria r = company == null ? null : company.criteria();
    boolean profileKnown =
        s != null
            && !blank(s.fullName())
            && !blank(s.registerNumber())
            && !blank(s.email())
            && !blank(s.department())
            && validYear(s.graduationYear());
    if (!profileKnown)
      add(
          details,
          "profile",
          "Student profile",
          "Complete student identity and academic information",
          "Incomplete",
          false,
          "Required student information is missing or invalid. Complete the profile before relying"
              + " on this match.");
    if (company == null || blank(company.name()) || blank(company.jobRole()))
      add(
          details,
          "configuration",
          "Company configuration",
          "Complete company name, job role, and criteria",
          "Incomplete",
          false,
          "The company profile is incomplete. Ask the placement officer to configure it.");

    BigDecimal actualCgpa = s == null ? null : s.cgpa(), min = r == null ? null : r.minCgpa();
    boolean validMin = validCgpa(min),
        validActual = validCgpa(actualCgpa),
        cgpaPass = validMin && validActual && actualCgpa.compareTo(min) >= 0;
    add(
        details,
        "cgpa",
        "CGPA",
        validMin ? "Minimum " + decimal(min) : "Valid minimum CGPA required",
        decimal(actualCgpa),
        cgpaPass,
        !validMin
            ? "Company minimum CGPA is missing or invalid; eligibility cannot be confirmed."
            : !validActual
                ? "Student CGPA is missing or invalid. Record a value between 0.00 and 10.00."
                : cgpaPass
                    ? "CGPA requirement satisfied: "
                        + decimal(actualCgpa)
                        + " is greater than or equal to "
                        + decimal(min)
                        + "."
                    : "CGPA is below the minimum: "
                        + decimal(actualCgpa)
                        + " is less than "
                        + decimal(min)
                        + ".");

    Integer active = s == null ? null : s.activeArrears(),
        maxActive = r == null ? null : r.maxActiveArrears();
    boolean validActiveLimit = validCount(maxActive),
        validActive = validCount(active),
        activePass = validActiveLimit && validActive && active <= maxActive;
    add(
        details,
        "active_arrears",
        "Active arrears",
        validActiveLimit ? "Maximum " + maxActive : "Valid maximum active-arrears count required",
        count(active),
        activePass,
        !validActiveLimit
            ? "The company's maximum permitted active arrears is missing or invalid."
            : !validActive
                ? "The student's active-arrears count is missing or invalid."
                : activePass
                    ? "Active-arrears requirement satisfied: "
                        + active
                        + " is within the maximum of "
                        + maxActive
                        + "."
                    : "The company allows a maximum of "
                        + maxActive
                        + " active arrears, but this student has "
                        + active
                        + ".");

    // NULL is deliberately different from zero. Zero enables the rule and permits no historical
    // arrears.
    if (r != null && r.maxHistoryArrears() != null) {
      Integer history = s == null ? null : s.historyArrears(), limit = r.maxHistoryArrears();
      boolean pass = validCount(limit) && validCount(history) && history <= limit;
      add(
          details,
          "history_arrears",
          "Historical arrears",
          "Maximum " + limit,
          count(history),
          pass,
          !validCount(limit)
              ? "The configured historical-arrears maximum is invalid."
              : !validCount(history)
                  ? "Historical-arrears information is missing or invalid; this company considers"
                        + " it."
                  : pass
                      ? "Historical arrears satisfy the requirement: "
                          + history
                          + " is within the maximum of "
                          + limit
                          + "."
                      : "The company permits at most "
                          + limit
                          + " historical arrears, but this student has "
                          + history
                          + ".");
    }

    if (r != null && r.requiredSkills() != null && !r.requiredSkills().isEmpty()) {
      List<String> required = r.requiredSkills();
      boolean valid =
          required.stream().allMatch(x -> !blank(x) && ValidationUtil.clean(x).length() <= 80);
      Set<String> actualSkills = normalized(s == null ? null : s.skills());
      List<String> missing =
          required.stream()
              .filter(Objects::nonNull)
              .filter(x -> !actualSkills.contains(ValidationUtil.key(x)))
              .toList();
      boolean pass = valid && s != null && s.skills() != null && missing.isEmpty();
      add(
          details,
          "skills",
          "Required skills",
          join(required),
          s == null || s.skills() == null || s.skills().isEmpty()
              ? "Not recorded"
              : join(s.skills()),
          pass,
          !valid
              ? "The required-skills configuration contains an invalid value. Review the company"
                    + " criteria."
              : pass
                  ? "All required skills are present: " + join(required) + "."
                  : "Missing required skill"
                      + (missing.size() == 1 ? "" : "s")
                      + ": "
                      + join(missing)
                      + ". Skills must match exactly; partial names do not count.");
    }
    if (r != null && r.departments() != null && !r.departments().isEmpty()) {
      String department = s == null ? null : s.department();
      boolean valid =
          r.departments().stream()
              .allMatch(x -> !blank(x) && ValidationUtil.clean(x).length() <= 30);
      boolean pass =
          valid
              && !blank(department)
              && normalized(r.departments()).contains(ValidationUtil.key(department));
      add(
          details,
          "department",
          "Department",
          join(r.departments()),
          blank(department) ? "Not provided" : department,
          pass,
          !valid
              ? "The eligible-departments configuration contains an invalid value."
              : pass
                  ? "The student's department is included in the company's eligible departments."
                  : blank(department)
                      ? "Student department is missing; the department requirement cannot be"
                            + " confirmed."
                      : "Department "
                          + department
                          + " is not in the eligible list: "
                          + join(r.departments())
                          + ".");
    }
    if (r != null && r.graduationYears() != null && !r.graduationYears().isEmpty()) {
      Integer year = s == null ? null : s.graduationYear();
      boolean valid = r.graduationYears().stream().allMatch(EligibilityService::validYear);
      boolean pass = valid && validYear(year) && r.graduationYears().contains(year);
      add(
          details,
          "graduation_year",
          "Graduation year",
          r.graduationYears().stream().map(String::valueOf).collect(Collectors.joining(", ")),
          year == null ? "Not provided" : year.toString(),
          pass,
          !valid
              ? "The graduation-year configuration contains an invalid year."
              : pass
                  ? "Graduation year " + year + " is accepted for this placement drive."
                  : "The student's graduation year is missing or is not accepted for this placement"
                        + " drive.");
    }
    int passed = (int) details.stream().filter(EvaluationDetail::passed).count(),
        failed = details.size() - passed;
    return new Evaluation(
        null,
        s == null ? null : s.id(),
        company == null ? null : company.id(),
        s == null ? "Unknown student" : s.fullName(),
        s == null ? "Unknown" : s.registerNumber(),
        s == null ? "Unknown" : s.department(),
        company == null ? "Unknown company" : company.name(),
        company == null ? "Unknown role" : company.jobRole(),
        !details.isEmpty() && failed == 0,
        passed,
        failed,
        Instant.now().toString(),
        s,
        company,
        List.copyOf(details));
  }

  private static void add(
      List<EvaluationDetail> out,
      String key,
      String label,
      String requirement,
      String actual,
      boolean passed,
      String explanation) {
    out.add(new EvaluationDetail(key, label, requirement, actual, passed, explanation));
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static boolean validCgpa(BigDecimal n) {
    return n != null
        && n.compareTo(BigDecimal.ZERO) >= 0
        && n.compareTo(BigDecimal.TEN) <= 0
        && n.stripTrailingZeros().scale() <= 2;
  }

  private static boolean validCount(Integer n) {
    return n != null && n >= 0 && n <= 100;
  }

  private static boolean validYear(Integer n) {
    return n != null && n >= 2000 && n <= 2100;
  }

  private static String decimal(BigDecimal n) {
    return n == null ? "Not provided" : n.toPlainString();
  }

  private static String count(Integer n) {
    return n == null ? "Not provided" : n.toString();
  }

  private static Set<String> normalized(List<String> values) {
    Set<String> set = new HashSet<>();
    if (values != null) for (String v : values) if (!blank(v)) set.add(ValidationUtil.key(v));
    return set;
  }

  private static String join(List<String> values) {
    return values.stream()
        .map(v -> v == null ? "Invalid value" : ValidationUtil.clean(v))
        .collect(Collectors.joining(", "));
  }
}
