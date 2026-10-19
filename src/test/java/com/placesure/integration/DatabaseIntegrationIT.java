package com.placesure.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.placesure.Fixtures;
import com.placesure.config.*;
import com.placesure.dao.*;
import com.placesure.model.*;
import com.placesure.service.*;
import com.placesure.util.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

/** Real JDBC tests; never substitutes H2, mocks, or an in-memory database for MySQL. */
class DatabaseIntegrationIT {
  static DatabaseConnection db;
  StudentDAO studentDAO = new StudentDAO();
  CompanyDAO companyDAO = new CompanyDAO();
  EvaluationDAO evaluationDAO = new EvaluationDAO();
  StudentService students;
  CompanyService companies;
  EligibilityService engine;
  ReportService reports;

  @BeforeAll
  static void schema() throws Exception {
    db = DatabaseConnection.testing(AppConfig.load());
    try (Connection c = db.open()) {
      if (!"placesure_test".equals(c.getCatalog()))
        throw new IllegalStateException("Refusing to modify anything except placesure_test.");
      String script =
          Files.readString(Path.of("database/database.sql")).split("-- Explicit IDs")[0];
      script =
          script
              .lines()
              .filter(line -> !line.stripLeading().startsWith("--"))
              .reduce("", (a, b) -> a + b + "\n");
      for (String sql : script.split(";"))
        if (!sql.isBlank())
          try (PreparedStatement p = c.prepareStatement(sql)) {
            p.execute();
          }
    }
  }

  @BeforeEach
  void prepare() throws SQLException {
    clear();
    students = new StudentService(db, studentDAO);
    companies = new CompanyService(db, companyDAO);
    engine = new EligibilityService(db, studentDAO, companyDAO, evaluationDAO);
    reports = new ReportService(db, new ReportDAO(evaluationDAO), evaluationDAO);
  }

  @AfterAll
  static void finish() throws SQLException {
    if (db != null) clear();
  }

  static void clear() throws SQLException {
    try (Connection c = db.open()) {
      if (!"placesure_test".equals(c.getCatalog()))
        throw new IllegalStateException("Wrong test database.");
      for (String table : List.of("evaluations", "students", "companies", "skills"))
        try (PreparedStatement p = c.prepareStatement("DELETE FROM " + table)) {
          p.executeUpdate();
        }
    }
  }

  Student createStudent() {
    try {
      return students.create(Fixtures.student());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  Company createCompany() {
    try {
      return companies.create(Fixtures.company());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  Evaluation saved() {
    Student s = createStudent();
    Company c = createCompany();
    try {
      return engine.evaluate(s.id(), c.id());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  long count(String table) throws SQLException {
    try (Connection c = db.open()) {
      return JdbcUtil.scalar(c, "SELECT COUNT(*) FROM " + table, List.of());
    }
  }

  QueryParams q(String... pairs) {
    Map<String, String> values = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
    return new QueryParams(values);
  }

  @Test
  void studentCrudUsesNormalizedSkillsAndUniqueRegisterNumbers() throws Exception {
    Student s = createStudent();
    assertNotNull(s.id());
    assertEquals(4, s.skills().size());
    assertThrows(SQLException.class, () -> students.create(Fixtures.student()));
    assertEquals(1, count("students"));
    Student edited =
        new Student(
            s.id(),
            s.registerNumber(),
            "Edited Student",
            s.email(),
            s.department(),
            2027,
            new BigDecimal("9.10"),
            0,
            0,
            List.of("java", "SQL", "Java"),
            null,
            null);
    Student update = students.update(s.id(), edited);
    assertEquals("Edited Student", update.fullName());
    assertEquals(2, update.skills().size());
    students.delete(s.id());
    assertEquals(0, count("students"));
    assertThrows(ApiException.class, () -> students.get(s.id()));
  }

  @Test
  void companyCriteriaCanBeCreatedReadUpdatedAndRemovedIndividually() throws Exception {
    Company c = createCompany();
    assertEquals(2, c.criteria().requiredSkills().size());
    CompanyCriteria criteria =
        new CompanyCriteria(
            new BigDecimal("6.50"), 2, null, List.of("Linux", "Networking"), List.of(), List.of());
    Company changed = companies.updateCriteria(c.id(), criteria);
    assertNull(changed.criteria().maxHistoryArrears());
    assertEquals(0, changed.criteria().departments().size());
    assertEquals(2, changed.criteria().maxActiveArrears());
    companies.delete(c.id());
    assertEquals(0, count("company_criteria"));
    assertEquals(0, count("company_required_skills"));
  }

  @Test
  void completeEvaluationAndEveryDetailArePersistedTogether() throws Exception {
    Evaluation e = saved();
    assertTrue(e.eligible());
    assertNotNull(e.id());
    assertEquals(6, count("evaluation_details"));
    try (Connection c = db.open()) {
      Evaluation read = evaluationDAO.get(c, e.id());
      assertEquals(e, read);
      assertEquals(read.passedCount() + read.failedCount(), read.details().size());
    }
  }

  @Test
  void changingCriteriaDoesNotRewriteHistoricalResults() throws Exception {
    Evaluation original = saved();
    CompanyCriteria harder =
        new CompanyCriteria(
            new BigDecimal("9.50"), 0, 0, List.of("Java", "SQL"), List.of("CSE"), List.of(2027));
    companies.updateCriteria(original.companyId(), harder);
    Evaluation newCheck = engine.evaluate(original.studentId(), original.companyId());
    assertFalse(newCheck.eligible());
    try (Connection c = db.open()) {
      Evaluation historical = evaluationDAO.get(c, original.id());
      assertTrue(historical.eligible());
      assertEquals(new BigDecimal("7.00"), historical.companySnapshot().criteria().minCgpa());
      assertEquals(original.details(), historical.details());
    }
  }

  @Test
  void editingStudentDoesNotRewriteItsSnapshot() throws Exception {
    Evaluation original = saved();
    Student s = students.get(original.studentId());
    students.update(
        s.id(),
        new Student(
            s.id(),
            s.registerNumber(),
            "Changed Name",
            s.email(),
            "MECH",
            2028,
            new BigDecimal("5.00"),
            2,
            3,
            List.of("HTML"),
            null,
            null));
    try (Connection c = db.open()) {
      Evaluation historical = evaluationDAO.get(c, original.id());
      assertEquals("Test Student", historical.studentName());
      assertEquals(new BigDecimal("8.20"), historical.studentSnapshot().cgpa());
      assertTrue(historical.eligible());
    }
  }

  @Test
  void deletionsPreserveHistorySnapshotsAndReports() throws Exception {
    Evaluation e = saved();
    students.delete(e.studentId());
    companies.delete(e.companyId());
    try (Connection c = db.open()) {
      Evaluation historical = evaluationDAO.get(c, e.id());
      assertTrue(historical.eligible());
      assertEquals("Test Partner", historical.companyName());
      assertEquals(6, historical.details().size());
      assertEquals(1L, new ReportDAO(evaluationDAO).dashboard(c).get("totalChecks"));
    }
    Report report = reports.generate(q("type", "history"));
    assertEquals(1, report.rows().size());
    assertEquals("Test Student", report.rows().get(0).get("studentName"));
  }

  @Test
  void aFailureSavingDetailsRollsBackTheSummaryAsWell() throws Exception {
    Student s = createStudent();
    Company c = createCompany();
    Evaluation raw = engine.evaluateRules(s, c);
    EvaluationDetail d = raw.details().get(0);
    Evaluation broken =
        new Evaluation(
            null,
            s.id(),
            c.id(),
            raw.studentName(),
            raw.registerNumber(),
            raw.department(),
            raw.companyName(),
            raw.jobRole(),
            true,
            2,
            0,
            raw.evaluatedAt(),
            s,
            c,
            List.of(d, d));
    assertThrows(
        SQLException.class,
        () -> JdbcUtil.transaction(db, connection -> evaluationDAO.save(connection, broken)));
    assertEquals(0, count("evaluations"));
    assertEquals(0, count("evaluation_details"));
  }

  @Test
  void reportsReturnCorrectSummaryAndEveryFailedCriterion() throws Exception {
    Evaluation pass = saved();
    CompanyCriteria hard =
        new CompanyCriteria(
            new BigDecimal("9.50"), 0, 0, List.of("Excel"), List.of("MECH"), List.of(2028));
    companies.updateCriteria(pass.companyId(), hard);
    Evaluation fail = engine.evaluate(pass.studentId(), pass.companyId());
    assertEquals(4, fail.failedCount());
    Report summary = reports.generate(q("type", "summary"));
    assertEquals(2, summary.summary().get("total"));
    assertEquals(1L, summary.summary().get("eligible"));
    assertEquals(2, summary.rows().get(0).get("total"));
    Report failures = reports.generate(q("type", "failures"));
    assertEquals(4, failures.rows().size());
    assertTrue(
        failures.rows().stream().allMatch(r -> ((Number) r.get("failedCount")).intValue() == 1));
  }

  @Test
  void latestMatchReportsNeverShowContradictoryRepeatedAttempts() throws Exception {
    Evaluation first = saved();
    companies.updateCriteria(
        first.companyId(),
        new CompanyCriteria(new BigDecimal("9.99"), 0, null, List.of(), List.of(), List.of()));
    engine.evaluate(first.studentId(), first.companyId());
    assertEquals(0, reports.generate(q("type", "eligible")).rows().size());
    assertEquals(1, reports.generate(q("type", "ineligible")).rows().size());
    assertEquals(1, reports.generate(q("type", "student")).rows().size());
    assertEquals(2, reports.generate(q("type", "history")).rows().size());
  }

  @Test
  void reportDateAndDepartmentFiltersAreCorrectAndInclusive() throws Exception {
    saved();
    String today = LocalDate.now(ZoneOffset.UTC).toString(),
        yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString();
    assertEquals(
        1,
        reports
            .generate(q("type", "history", "from", today, "to", today, "department", "CSE"))
            .rows()
            .size());
    assertEquals(0, reports.generate(q("type", "history", "to", yesterday)).rows().size());
    assertEquals(0, reports.generate(q("department", "MECH")).rows().size());
    assertThrows(ApiException.class, () -> reports.generate(q("from", today, "to", yesterday)));
  }

  @Test
  void csvExportContainsSavedResultsAndSafelyQuotesNames() throws Exception {
    Student p = Fixtures.student();
    Student s =
        students.create(
            new Student(
                null,
                p.registerNumber(),
                "Student, \"Example\"",
                p.email(),
                p.department(),
                p.graduationYear(),
                p.cgpa(),
                0,
                0,
                p.skills(),
                null,
                null));
    Company c = createCompany();
    Evaluation e = engine.evaluate(s.id(), c.id());
    String csv =
        new String(
            CsvExportUtil.export(reports.generate(q("type", "history"))),
            java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("\uFEFF"));
    assertTrue(csv.contains("\"Student, \"\"Example\"\"\""));
    assertTrue(csv.contains("\"Eligible\""));
    assertEquals(6, reports.evaluation(e.id()).rows().size());
  }

  @Test
  void paginationSearchAndSortAreDatabaseBacked() throws Exception {
    Student base = createStudent();
    students.create(
        new Student(
            null,
            "TEST002",
            "Another Student",
            "another@example.test",
            "IT",
            2027,
            new BigDecimal("9.20"),
            1,
            0,
            List.of("Java"),
            null,
            null));
    Page<Student> page = students.list(q("sort", "cgpa", "direction", "desc", "size", "1"));
    assertEquals(2, page.total());
    assertEquals(new BigDecimal("9.20"), page.items().get(0).cgpa());
    assertEquals(1, students.list(q("q", base.registerNumber())).total());
    assertEquals(1, students.list(q("maxArrears", "0")).total());
  }

  @Test
  void invalidIdsDoNotCreatePartialEvaluationRows() throws Exception {
    assertThrows(ApiException.class, () -> engine.evaluate(999999, 999999));
    assertEquals(0, count("evaluations"));
    assertEquals(0, count("evaluation_details"));
  }

  @Test
  void concurrentChecksRemainAtomicAndDashboardTotalsAgree() throws Exception {
    Student s = createStudent();
    Company c = createCompany();
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Future<Evaluation>> futures = new ArrayList<>();
      for (int i = 0; i < 8; i++) futures.add(pool.submit(() -> engine.evaluate(s.id(), c.id())));
      for (Future<Evaluation> f : futures) assertTrue(f.get(15, TimeUnit.SECONDS).eligible());
    } finally {
      pool.shutdownNow();
    }
    assertEquals(8, count("evaluations"));
    assertEquals(48, count("evaluation_details"));
    try (Connection connection = db.open()) {
      Map<String, Object> dashboard = new ReportDAO(evaluationDAO).dashboard(connection);
      assertEquals(8L, dashboard.get("totalChecks"));
      assertEquals(8L, dashboard.get("eligibleMatches"));
    }
  }

  @Test
  void maximumSupportedSkillsStillFitSavedSnapshots() throws Exception {
    List<String> skills = new ArrayList<>();
    for (int i = 0; i < 50; i++) skills.add(String.format("Skill %02d ", i) + "x".repeat(71));
    Student p = Fixtures.student();
    Student s =
        students.create(
            new Student(
                null,
                "LONG-SKILLS",
                p.fullName(),
                p.email(),
                p.department(),
                p.graduationYear(),
                p.cgpa(),
                0,
                0,
                skills,
                null,
                null));
    Company c =
        companies.create(
            Fixtures.company(
                new CompanyCriteria(new BigDecimal("7"), 0, null, skills, List.of(), List.of())));
    Evaluation e = engine.evaluate(s.id(), c.id());
    assertTrue(e.eligible());
    assertTrue(
        e.details().stream()
                .filter(d -> d.key().equals("skills"))
                .findFirst()
                .orElseThrow()
                .actual()
                .length()
            > 2000);
    try (Connection connection = db.open()) {
      assertEquals(e, evaluationDAO.get(connection, e.id()));
    }
  }

  @Test
  void emptyWorkspaceReturnsUsefulZeroCountsAndEmptyReports() throws Exception {
    try (Connection c = db.open()) {
      Map<String, Object> d = new ReportDAO(evaluationDAO).dashboard(c);
      assertEquals(0L, d.get("totalStudents"));
      assertEquals(0L, d.get("totalCompanies"));
      assertEquals(0L, d.get("totalChecks"));
      assertEquals(0L, d.get("eligibleMatches"));
      assertTrue(((List<?>) d.get("companySummary")).isEmpty());
    }
    for (String type :
        List.of("summary", "student", "eligible", "ineligible", "failures", "history")) {
      Report report = reports.generate(q("type", type));
      assertTrue(report.rows().isEmpty());
      assertEquals(0, report.summary().get("total"));
    }
  }
}
