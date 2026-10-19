package com.placesure.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable profile. Validation belongs to StudentService; database access belongs to StudentDAO.
 */
public record Student(
    Long id,
    String registerNumber,
    String fullName,
    String email,
    String department,
    Integer graduationYear,
    BigDecimal cgpa,
    Integer activeArrears,
    Integer historyArrears,
    List<String> skills,
    String phone,
    String createdAt) {}
