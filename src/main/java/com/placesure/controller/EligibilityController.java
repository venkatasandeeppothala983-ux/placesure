package com.placesure.controller;

import com.placesure.service.EligibilityService;
import com.placesure.util.ApiException;

public final class EligibilityController {
  public record Request(Long studentId, Long companyId) {}

  public EligibilityController(Router r, EligibilityService s) {
    r.add(
        "POST",
        "/api/evaluations",
        c -> {
          Request body = c.body(Request.class);
          if (body == null || body.studentId() == null || body.companyId() == null)
            throw new ApiException(400, "VALIDATION_ERROR", "Select both a student and a company.");
          c.data(201, s.evaluate(body.studentId(), body.companyId()));
        });
  }
}
