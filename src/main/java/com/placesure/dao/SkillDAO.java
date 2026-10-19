package com.placesure.dao;

import java.sql.*;
import java.util.*;

/** Tables and ID columns are chosen from a private allowlist, never from client input. */
public final class SkillDAO {
  public List<String> load(Connection c, String owner, long id) throws SQLException {
    String table = owner.equals("student") ? "student_skills" : "company_required_skills";
    String key = owner.equals("student") ? "student_id" : "company_id";
    List<String> skills = new ArrayList<>();
    try (PreparedStatement p =
        c.prepareStatement(
            "SELECT s.name FROM skills s JOIN "
                + table
                + " m ON m.skill_id=s.id WHERE m."
                + key
                + "=? ORDER BY s.name")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) skills.add(r.getString(1));
      }
    }
    return List.copyOf(skills);
  }

  public void replace(Connection c, String owner, long id, List<String> names) throws SQLException {
    String table = owner.equals("student") ? "student_skills" : "company_required_skills";
    String key = owner.equals("student") ? "student_id" : "company_id";
    try (PreparedStatement p =
        c.prepareStatement("DELETE FROM " + table + " WHERE " + key + "=?")) {
      p.setLong(1, id);
      p.executeUpdate();
    }
    try (PreparedStatement insert =
            c.prepareStatement(
                "INSERT INTO skills(name) VALUES(?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                Statement.RETURN_GENERATED_KEYS);
        PreparedStatement link =
            c.prepareStatement("INSERT INTO " + table + "(" + key + ",skill_id) VALUES(?,?)")) {
      for (String name : names) {
        insert.setString(1, name);
        insert.executeUpdate();
        try (ResultSet keys = insert.getGeneratedKeys()) {
          if (!keys.next()) throw new SQLException("Could not resolve normalized skill.");
          link.setLong(1, id);
          link.setLong(2, keys.getLong(1));
          link.executeUpdate();
        }
      }
    }
  }
}
