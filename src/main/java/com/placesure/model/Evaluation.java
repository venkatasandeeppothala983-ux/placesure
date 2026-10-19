package com.placesure.model;

import java.util.List;

/**
 * Snapshots are independent of live student/company records and are never recalculated on reads.
 */
public record Evaluation(
    Long id,
    Long studentId,
    Long companyId,
    String studentName,
    String registerNumber,
    String department,
    String companyName,
    String jobRole,
    boolean eligible,
    int passedCount,
    int failedCount,
    String evaluatedAt,
    Student studentSnapshot,
    Company companySnapshot,
    List<EvaluationDetail> details) {}
