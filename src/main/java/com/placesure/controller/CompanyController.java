package com.placesure.controller;

import com.placesure.model.*;
import com.placesure.service.CompanyService;
import java.util.Map;

public final class CompanyController {
  public CompanyController(Router r, CompanyService s) {
    r.add("GET", "/api/companies", c -> c.data(s.list(c.query())));
    r.add("POST", "/api/companies", c -> c.data(201, s.create(c.body(Company.class))));
    r.add("GET", "/api/companies/(?<id>[0-9]+)", c -> c.data(s.get(c.id())));
    r.add(
        "PUT",
        "/api/companies/(?<id>[0-9]+)",
        c -> c.data(s.update(c.id(), c.body(Company.class))));
    r.add(
        "DELETE",
        "/api/companies/(?<id>[0-9]+)",
        c -> {
          s.delete(c.id());
          c.data(Map.of("deleted", true));
        });
    r.add("GET", "/api/companies/(?<id>[0-9]+)/criteria", c -> c.data(s.get(c.id()).criteria()));
    r.add(
        "PUT",
        "/api/companies/(?<id>[0-9]+)/criteria",
        c -> c.data(s.updateCriteria(c.id(), c.body(CompanyCriteria.class)).criteria()));
    // Criteria are created with their company. Removing a criterion is PUT with the optional field
    // unset.
    // Deleting a required criteria row independently would create an unsafe, incomplete
    // configuration.
  }
}
