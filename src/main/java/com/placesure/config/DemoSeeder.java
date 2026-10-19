package com.placesure.config;

import com.placesure.service.EligibilityService;
import com.placesure.util.JdbcUtil;
import java.sql.*;
import java.util.*;

/**
 * Invoked explicitly with --seed-demo; computes real outcomes instead of inserting pretend results.
 */
public final class DemoSeeder {
  private DemoSeeder() {}

  public static int seed(DatabaseConnection db, EligibilityService engine) throws SQLException {
    List<Long> students = new ArrayList<>(), companies = new ArrayList<>();
    try (Connection c = db.open()) {
      if (JdbcUtil.scalar(c, "SELECT COUNT(*) FROM evaluations", List.of()) > 0) return 0;
      try (PreparedStatement p = c.prepareStatement("SELECT id FROM students ORDER BY id");
          ResultSet r = p.executeQuery()) {
        while (r.next()) students.add(r.getLong(1));
      }
      try (PreparedStatement p = c.prepareStatement("SELECT id FROM companies ORDER BY id");
          ResultSet r = p.executeQuery()) {
        while (r.next()) companies.add(r.getLong(1));
      }
    }
    if (students.isEmpty() || companies.isEmpty()) return 0;
    int count = 0;
    for (int company = 0; company < companies.size(); company++) {
      Set<Long> chosen = new LinkedHashSet<>();
      for (int j = 0; chosen.size() < Math.min(8, students.size()) && j < students.size() * 2; j++)
        chosen.add(students.get((j * 5 + company * 2) % students.size()));
      // Fallback for cohort sizes divisible by five.
      for (Long id : students) {
        if (chosen.size() >= Math.min(8, students.size())) break;
        chosen.add(id);
      }
      for (Long student : chosen) {
        engine.evaluate(student, companies.get(company));
        count++;
      }
    }
    return count;
  }
}
