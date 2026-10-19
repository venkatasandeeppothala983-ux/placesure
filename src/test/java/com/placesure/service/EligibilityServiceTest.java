package com.placesure.service;

import static org.junit.jupiter.api.Assertions.*;

import com.placesure.Fixtures;
import com.placesure.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EligibilityServiceTest {
  private final EligibilityService engine = new EligibilityService();

  private Evaluation check(Student s) {
    return engine.evaluateRules(s, Fixtures.company());
  }

  private EvaluationDetail criterion(Evaluation e, String key) {
    return e.details().stream().filter(d -> d.key().equals(key)).findFirst().orElseThrow();
  }

  @Test
  void studentMeetsEveryConfiguredRequirement() {
    Evaluation e = check(Fixtures.student());
    assertTrue(e.eligible());
    assertEquals(6, e.passedCount());
    assertEquals(0, e.failedCount());
    assertTrue(e.details().stream().allMatch(EvaluationDetail::passed));
  }

  @Test
  void cgpaBelowMinimumFails() {
    Evaluation e = check(Fixtures.student("6.99", 0, 0, "CSE", 2027, List.of("Java", "SQL")));
    assertFalse(e.eligible());
    assertFalse(criterion(e, "cgpa").passed());
    assertEquals(1, e.failedCount());
  }

  @Test
  void activeArrearsAreAMaximumNotAnEquality() {
    Evaluation e = check(Fixtures.student("8.20", 2, 0, "CSE", 2027, List.of("Java", "SQL")));
    assertFalse(e.eligible());
    assertTrue(criterion(e, "active_arrears").explanation().contains("maximum of 0"));
  }

  @Test
  void missingRequiredSkillFailsAndNamesIt() {
    Evaluation e = check(Fixtures.student("8.20", 0, 0, "CSE", 2027, List.of("Java")));
    assertFalse(e.eligible());
    assertTrue(criterion(e, "skills").explanation().contains("SQL"));
  }

  @Test
  void departmentMismatchFails() {
    Evaluation e = check(Fixtures.student("8.20", 0, 0, "MECH", 2027, List.of("Java", "SQL")));
    assertFalse(criterion(e, "department").passed());
  }

  @Test
  void allFailuresAreCollectedNotJustTheFirst() {
    Evaluation e = check(Fixtures.student("5.50", 3, 4, "MECH", 2028, List.of("HTML")));
    assertFalse(e.eligible());
    assertEquals(6, e.failedCount());
    assertEquals(0, e.passedCount());
  }

  @Test
  void missingCgpaCannotPass() {
    Evaluation e = check(Fixtures.student(null, 0, 0, "CSE", 2027, List.of("Java", "SQL")));
    assertFalse(e.eligible());
    assertFalse(criterion(e, "cgpa").passed());
  }

  @Test
  void missingMandatoryIdentityCannotPass() {
    Student p = Fixtures.student();
    Student incomplete =
        new Student(
            1L,
            p.registerNumber(),
            null,
            p.email(),
            p.department(),
            p.graduationYear(),
            p.cgpa(),
            p.activeArrears(),
            p.historyArrears(),
            p.skills(),
            null,
            null);
    assertFalse(check(incomplete).eligible());
    assertFalse(criterion(check(incomplete), "profile").passed());
  }

  @Test
  void nullStudentCannotPass() {
    Evaluation e = check(null);
    assertFalse(e.eligible());
    assertTrue(e.failedCount() >= 2);
  }

  @Test
  void incompleteCompanyCriteriaCannotPass() {
    Evaluation e =
        engine.evaluateRules(
            Fixtures.student(),
            Fixtures.company(new CompanyCriteria(null, null, null, null, null, null)));
    assertFalse(e.eligible());
    assertFalse(criterion(e, "cgpa").passed());
    assertFalse(criterion(e, "active_arrears").passed());
  }

  @Test
  void absentCompanyCriteriaCannotPass() {
    assertFalse(engine.evaluateRules(Fixtures.student(), Fixtures.company(null)).eligible());
  }

  @Test
  void nullCompanyCannotPass() {
    assertFalse(engine.evaluateRules(Fixtures.student(), null).eligible());
  }

  @Test
  void cgpaExactlyEqualToMinimumPassesUsingDecimalArithmetic() {
    assertTrue(
        check(Fixtures.student("7.00", 0, 0, "CSE", 2027, List.of("Java", "SQL"))).eligible());
  }

  @Test
  void skillComparisonIsCaseAndWhitespaceNormalized() {
    Evaluation e = check(Fixtures.student("8.20", 0, 0, "cse", 2027, List.of("  JAVA ", " sql ")));
    assertTrue(e.eligible());
  }

  @Test
  void substringSkillsNeverMatch() {
    Evaluation e = check(Fixtures.student("8.20", 0, 0, "CSE", 2027, List.of("JavaScript", "SQL")));
    assertFalse(criterion(e, "skills").passed());
    assertTrue(criterion(e, "skills").explanation().contains("Java"));
  }

  @Test
  void cAndCPlusPlusAreDistinct() {
    Company c =
        Fixtures.company(
            new CompanyCriteria(
                new BigDecimal("7"), 0, null, List.of("C++"), List.of(), List.of()));
    assertFalse(
        engine.evaluateRules(Fixtures.student("8", 0, 0, "CSE", 2027, List.of("C")), c).eligible());
  }

  @Test
  void optionalUnconfiguredCriteriaAreNotFailures() {
    Company c =
        Fixtures.company(
            new CompanyCriteria(new BigDecimal("7"), 2, null, List.of(), List.of(), List.of()));
    Evaluation e = engine.evaluateRules(Fixtures.student("8", 1, null, "MECH", 2028, List.of()), c);
    assertTrue(e.eligible());
    assertEquals(2, e.details().size());
  }

  @Test
  void zeroHistoryLimitIsConfiguredNotIgnored() {
    Company c =
        Fixtures.company(
            new CompanyCriteria(new BigDecimal("7"), 0, 0, List.of(), List.of(), List.of()));
    Evaluation e = engine.evaluateRules(Fixtures.student("8", 0, 1, "CSE", 2027, List.of()), c);
    assertFalse(criterion(e, "history_arrears").passed());
  }

  @Test
  void unknownHistoryFailsWhenCompanyConsidersIt() {
    assertFalse(
        criterion(
                check(Fixtures.student("8", 0, null, "CSE", 2027, List.of("Java", "SQL"))),
                "history_arrears")
            .passed());
  }

  @Test
  void graduationYearRestrictionFails() {
    assertFalse(
        criterion(
                check(Fixtures.student("8", 0, 0, "CSE", 2026, List.of("Java", "SQL"))),
                "graduation_year")
            .passed());
  }

  @Test
  void missingSkillsCannotPassARequiredSkillsRule() {
    assertFalse(
        criterion(check(Fixtures.student("8", 0, 0, "CSE", 2027, null)), "skills").passed());
  }

  @Test
  void negativeArrearsCannotPass() {
    assertFalse(
        check(Fixtures.student("8", -1, 0, "CSE", 2027, List.of("Java", "SQL"))).eligible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "10.01", "8.001"})
  void invalidStudentCgpaCannotPass(String value) {
    assertFalse(
        check(Fixtures.student(value, 0, 0, "CSE", 2027, List.of("Java", "SQL"))).eligible());
  }

  @Test
  void invalidCompanyLimitsCannotPass() {
    Company c =
        Fixtures.company(
            new CompanyCriteria(new BigDecimal("11"), -1, -1, List.of(), List.of(), List.of()));
    assertFalse(engine.evaluateRules(Fixtures.student(), c).eligible());
  }

  @Test
  void malformedRequiredSkillsCannotBeSilentlyIgnored() {
    Company c =
        Fixtures.company(
            new CompanyCriteria(
                new BigDecimal("7"), 0, null, Arrays.asList("SQL", null), List.of(), List.of()));
    assertFalse(engine.evaluateRules(Fixtures.student(), c).eligible());
  }

  @Test
  void everyCriterionHasAnExplanationAndSnapshot() {
    Evaluation e = check(Fixtures.student());
    for (EvaluationDetail d : e.details()) {
      assertFalse(d.actual().isBlank());
      assertFalse(d.requirement().isBlank());
      assertFalse(d.explanation().isBlank());
    }
    assertEquals(e.details().size(), e.passedCount() + e.failedCount());
    assertSame(Fixtures.company().getClass(), e.companySnapshot().getClass());
  }
}
