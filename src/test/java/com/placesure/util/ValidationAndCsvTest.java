package com.placesure.util;

import static org.junit.jupiter.api.Assertions.*;

import com.placesure.Fixtures;
import com.placesure.dao.*;
import com.placesure.model.*;
import com.placesure.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationAndCsvTest {
  private final StudentService students = new StudentService(null, new StudentDAO());
  private final CompanyService companies = new CompanyService(null, new CompanyDAO());

  @Test
  void validProfileIsNormalized() {
    Student raw =
        new Student(
            null,
            " test-001 ",
            "  Test   Student ",
            "test@example.test",
            " cse ",
            2027,
            new BigDecimal("8.2"),
            0,
            0,
            List.of(" Java ", "java", "SQL"),
            "",
            null);
    Student s = students.validate(raw);
    assertEquals("TEST-001", s.registerNumber());
    assertEquals("Test Student", s.fullName());
    assertEquals("CSE", s.department());
    assertEquals(2, s.skills().size());
    assertEquals(new BigDecimal("8.20"), s.cgpa());
    assertNull(s.phone());
  }

  @Test
  void missingRequiredFieldsAreRejected() {
    ApiException e =
        assertThrows(
            ApiException.class,
            () ->
                students.validate(
                    new Student(
                        null, null, null, null, null, null, null, null, null, null, null, null)));
    assertEquals(400, e.status());
    assertTrue(e.fields().containsKey("cgpa"));
    assertTrue(e.fields().containsKey("registerNumber"));
  }

  @Test
  void negativeArrearsAreRejected() {
    assertThrows(
        ApiException.class,
        () -> students.validate(Fixtures.student("8", -1, 0, "CSE", 2027, List.of())));
  }

  @Test
  void excessDecimalPrecisionIsRejected() {
    assertThrows(
        ApiException.class,
        () -> students.validate(Fixtures.student("8.123", 0, 0, "CSE", 2027, List.of())));
  }

  @Test
  void invalidCompanyCriteriaAreRejected() {
    assertThrows(
        ApiException.class,
        () ->
            companies.validateCriteria(
                new CompanyCriteria(
                    new BigDecimal("10.01"), 0, null, List.of(), List.of(), List.of())));
  }

  @Test
  void optionalHistoricalRuleRemainsNull() {
    assertNull(
        companies
            .validateCriteria(
                new CompanyCriteria(new BigDecimal("7"), 0, null, List.of(), List.of(), List.of()))
            .maxHistoryArrears());
  }

  @Test
  void csvEscapesCommasQuotesAndNewlines() {
    assertEquals("\"Doe, \"\"Test\"\"\nStudent\"", CsvExportUtil.escape("Doe, \"Test\"\nStudent"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"=SUM(A1:A2)", "+1+1", "-2+3", "@SUM(A1)", "  =1+1", "\tcommand", "\rcommand"})
  void csvNeutralizesFormulaInjection(String value) {
    assertTrue(CsvExportUtil.escape(value).startsWith("\"'"));
  }

  @Test
  void numericCsvValuesAreNotRewrittenAsFormulas() {
    assertEquals("\"-12\"", CsvExportUtil.escape(-12));
  }

  @Test
  void queryRejectsInvalidDateRangesTypesAndSortDirection() {
    assertThrows(
        ApiException.class, () -> new QueryParams(Map.of("from", "not-a-date")).date("from"));
    assertThrows(ApiException.class, () -> new QueryParams(Map.of("page", "1.2")).page());
    assertThrows(
        ApiException.class, () -> new QueryParams(Map.of("direction", "DROP TABLE")).direction());
  }

  @Test
  void optionalPhoneMustContainARealisticNumberOfDigits() {
    Student p = Fixtures.student();
    assertThrows(
        ApiException.class,
        () ->
            students.validate(
                new Student(
                    p.id(),
                    p.registerNumber(),
                    p.fullName(),
                    p.email(),
                    p.department(),
                    p.graduationYear(),
                    p.cgpa(),
                    p.activeArrears(),
                    p.historyArrears(),
                    p.skills(),
                    "--- --- ---",
                    null)));
    assertEquals(
        "+91 90000 00000",
        students
            .validate(
                new Student(
                    p.id(),
                    p.registerNumber(),
                    p.fullName(),
                    p.email(),
                    p.department(),
                    p.graduationYear(),
                    p.cgpa(),
                    p.activeArrears(),
                    p.historyArrears(),
                    p.skills(),
                    "+91 90000 00000",
                    null))
            .phone());
  }
}
