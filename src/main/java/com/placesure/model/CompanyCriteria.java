package com.placesure.model;

import java.math.BigDecimal;
import java.util.List;

/** maxHistoryArrears == null means the optional historical-arrears rule is disabled. */
public record CompanyCriteria(
    BigDecimal minCgpa,
    Integer maxActiveArrears,
    Integer maxHistoryArrears,
    List<String> requiredSkills,
    List<String> departments,
    List<Integer> graduationYears) {}
