package com.placesure.util;

import com.placesure.config.DatabaseConnection;
import java.sql.*;
import java.util.*;

public final class JdbcUtil {
  private JdbcUtil() {}

  @FunctionalInterface
  public interface Transaction<T> {
    T run(Connection c) throws SQLException;
  }

  public static <T> T transaction(DatabaseConnection db, Transaction<T> work) throws SQLException {
    try (Connection c = db.open()) {
      c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      c.setAutoCommit(false);
      try {
        T result = work.run(c);
        c.commit();
        return result;
      } catch (SQLException | RuntimeException e) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          e.addSuppressed(rollback);
        }
        throw e;
      }
    }
  }

  public static void bind(PreparedStatement p, List<?> values) throws SQLException {
    for (int i = 0; i < values.size(); i++) p.setObject(i + 1, values.get(i));
  }

  public static long scalar(Connection c, String sql, List<?> values) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, values);
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? r.getLong(1) : 0;
      }
    }
  }

  public static int update(Connection c, String sql, Object... values) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, Arrays.asList(values));
      return p.executeUpdate();
    }
  }

  public static String like(String value) {
    return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  public static String instant(Timestamp value) {
    return value == null ? null : value.toInstant().toString();
  }
}
