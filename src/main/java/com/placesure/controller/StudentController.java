package com.placesure.controller;

import com.placesure.model.Student;
import com.placesure.service.StudentService;
import java.util.Map;

public final class StudentController {
  public StudentController(Router r, StudentService s) {
    r.add("GET", "/api/students", c -> c.data(s.list(c.query())));
    r.add("POST", "/api/students", c -> c.data(201, s.create(c.body(Student.class))));
    r.add("GET", "/api/students/(?<id>[0-9]+)", c -> c.data(s.get(c.id())));
    r.add(
        "PUT", "/api/students/(?<id>[0-9]+)", c -> c.data(s.update(c.id(), c.body(Student.class))));
    r.add(
        "DELETE",
        "/api/students/(?<id>[0-9]+)",
        c -> {
          s.delete(c.id());
          c.data(Map.of("deleted", true));
        });
  }
}
