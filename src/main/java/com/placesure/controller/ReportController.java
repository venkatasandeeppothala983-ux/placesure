package com.placesure.controller;

import com.placesure.service.ReportService;
import com.placesure.util.CsvExportUtil;

public final class ReportController {
  public ReportController(Router r, ReportService s) {
    r.add("GET", "/api/reports", c -> c.data(s.generate(c.query())));
    r.add(
        "GET",
        "/api/reports/export",
        c -> c.csv("placesure-report.csv", CsvExportUtil.export(s.generate(c.query()))));
    r.add(
        "GET",
        "/api/evaluations/(?<id>[0-9]+)/export",
        c ->
            c.csv(
                "placesure-evaluation-" + c.id() + ".csv",
                CsvExportUtil.export(s.evaluation(c.id()))));
  }
}
