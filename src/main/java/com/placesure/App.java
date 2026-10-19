package com.placesure;

import com.placesure.config.*;
import com.placesure.controller.*;
import com.placesure.dao.*;
import com.placesure.service.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public final class App {
  public static void main(String[] args) throws Exception {
    AppConfig config = AppConfig.load();
    DatabaseConnection db = DatabaseConnection.application(config);
    StudentDAO students = new StudentDAO();
    CompanyDAO companies = new CompanyDAO();
    EvaluationDAO evaluations = new EvaluationDAO();
    ReportDAO reports = new ReportDAO(evaluations);
    StudentService studentService = new StudentService(db, students);
    CompanyService companyService = new CompanyService(db, companies);
    EligibilityService eligibility = new EligibilityService(db, students, companies, evaluations);
    ReportService reportService = new ReportService(db, reports, evaluations);
    if (Arrays.asList(args).contains("--seed-demo"))
      try {
        System.out.println(
            "Demo evaluations computed and saved: " + DemoSeeder.seed(db, eligibility));
      } catch (java.sql.SQLException e) {
        System.err.println(
            "Demo seeding skipped: database is not ready. Check local configuration; the"
                + " application will show a connection-help screen.");
      }
    Router router = new Router();
    new StudentController(router, studentService);
    new CompanyController(router, companyService);
    new EligibilityController(router, eligibility);
    new EvaluationController(router, db, evaluations);
    new DashboardController(router, db, reports);
    new ReportController(router, reportService);
    HttpServer server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 64);
    ExecutorService executor = Executors.newFixedThreadPool(12);
    server.setExecutor(executor);
    server.createContext("/", router);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  server.stop(1);
                  executor.shutdown();
                }));
    server.start();
    System.out.println("PlaceSure 1.0.0 ready on http://" + config.host() + ":" + config.port());
    System.out.println(
        "Know Your Eligibility. Own Your Opportunity. | Java "
            + System.getProperty("java.version"));
  }
}
