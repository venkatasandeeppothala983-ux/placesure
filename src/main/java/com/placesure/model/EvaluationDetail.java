package com.placesure.model;

public record EvaluationDetail(
    String key,
    String criterion,
    String requirement,
    String actual,
    boolean passed,
    String explanation) {}
