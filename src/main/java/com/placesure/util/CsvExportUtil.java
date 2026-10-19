package com.placesure.util;

import com.placesure.model.Report;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** RFC 4180-style quoting, UTF-8 BOM for Excel, plus spreadsheet-formula neutralization. */
public final class CsvExportUtil {
  private CsvExportUtil() {}

  public static String escape(Object value) {
    String s = value == null ? "" : String.valueOf(value);
    if (!(value instanceof Number)) {
      String probe = s.stripLeading();
      if (!probe.isEmpty() && ("=+-@".indexOf(probe.charAt(0)) >= 0)
          || s.startsWith("\t")
          || s.startsWith("\r")) s = "'" + s;
    }
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  public static String row(List<?> values) {
    return values.stream().map(CsvExportUtil::escape).collect(Collectors.joining(",")) + "\r\n";
  }

  public static byte[] export(Report report) {
    StringBuilder out = new StringBuilder("\uFEFF");
    out.append(row(List.of("PlaceSure — " + report.title())));
    out.append(row(List.of("Generated at (UTC)", report.generatedAt())));
    out.append(row(List.of("Scope", report.scope())));
    out.append("\r\n");
    out.append(row(report.columns().stream().map(Report.Column::label).toList()));
    for (Map<String, Object> r : report.rows())
      out.append(row(report.columns().stream().map(c -> r.get(c.key())).toList()));
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }
}
