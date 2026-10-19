package com.placesure.model;

public record Company(
    Long id,
    String name,
    String jobRole,
    String driveDate,
    String salaryPackage,
    String notes,
    CompanyCriteria criteria,
    String createdAt) {}
