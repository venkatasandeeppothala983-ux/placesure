package com.placesure;

import com.placesure.model.*;
import java.math.BigDecimal;
import java.util.List;

public final class Fixtures {
  private Fixtures() {}

  public static Student student() {
    return student("8.20", 0, 0, "CSE", 2027, List.of("Java", "SQL", "Git", "Python"));
  }

  public static Student student(
      String cgpa,
      Integer active,
      Integer history,
      String department,
      Integer year,
      List<String> skills) {
    return new Student(
        1L,
        "TEST001",
        "Test Student",
        "student@example.test",
        department,
        year,
        cgpa == null ? null : new BigDecimal(cgpa),
        active,
        history,
        skills,
        null,
        null);
  }

  public static Company company() {
    return company(
        new CompanyCriteria(
            new BigDecimal("7.00"),
            0,
            1,
            List.of("Java", "SQL"),
            List.of("CSE", "IT"),
            List.of(2027)));
  }

  public static Company company(CompanyCriteria r) {
    return new Company(1L, "Test Partner", "Software Engineer", null, null, null, r, null);
  }
}
