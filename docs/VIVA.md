# PlaceSure — simple viva notes

**Tagline:** Know Your Eligibility. Own Your Opportunity.

## A 30-second introduction

“PlaceSure is a Java-based placement eligibility checker for a placement officer. It compares a student’s CGPA, active arrears, skills, and other configured criteria with a company’s requirements. It explains every pass and failure, saves the complete decision in MySQL, and generates reports. Eligibility is not a guarantee of a job offer.”

## 1. What problem does it solve?

Different employers have different placement requirements. Manually checking many spreadsheets can cause mistakes and makes rejected cases difficult to explain. PlaceSure gives one consistent decision and an understandable reason for each condition.

## 2. Why Java and JDBC?

Java is strongly typed and makes the business rules easy to organize and test. JDBC lets Java communicate directly with a relational database using SQL. Prepared statements safely bind input values instead of joining user text into SQL.

The JDK HTTP server serves our pages and APIs. Jackson converts Java records to and from JSON. We did not need a large backend framework for this focused project.

## 3. Where are classes, objects, and encapsulation used?

A `Student` object represents a profile, `Company` represents an opportunity, and `CompanyCriteria` represents its rules. Immutable Java records encapsulate these fields and generate accessors. DAOs encapsulate database operations. Services encapsulate validation and business logic. The interface cannot edit model fields directly in the Java process.

## 4. Where are Collections used?

- `List<EvaluationDetail>` stores the ordered breakdown of criteria.
- `Set<String>` holds normalized student skills for exact membership checks.
- A missing-skills `List` records every required skill the student does not have.
- `Map<String, String>` collects field-validation errors.
- `Map` groups report rows by original company ID and holds dashboard values.
- Lists hold allowed departments and graduation years.

These collections are used by running code, not just mentioned in documentation.

## 5. Where is conditional logic used?

In `EligibilityService.evaluateRules`, `if` conditions decide whether each rule is configured and whether the student meets it. For example, if a historical-arrears maximum is `null`, that optional rule is omitted. If it is zero, the rule is enabled and requires zero historical arrears.

The service does not return after the first failure. It evaluates the rest so the officer sees every gap.

## 6. Explain the eligibility algorithm

1. Fetch student and company requirements inside a database transaction.
2. Check that mandatory data and criteria exist and are valid.
3. Compare CGPA, active arrears, and any configured historical-arrears limit.
4. Check that **all** required skills exist in the student’s normalized skill set.
5. Apply optional department and graduation-year restrictions.
6. Add a PASS/FAIL result and explanation for each rule.
7. If any detail fails, the final outcome is not eligible.
8. Save the summary, all detail snapshots, and profile/criteria snapshots together.
9. Commit; roll back if a database operation fails.

## 7. Why use BigDecimal for CGPA?

`double` represents binary floating-point numbers and can introduce rounding surprises. `BigDecimal` represents decimal values accurately. We use `actual.compareTo(minimum) >= 0`, so a CGPA exactly equal to the minimum passes.

## 8. Are arrears a maximum or an exact count?

They are a **maximum permitted count**. If the company permits two active arrears, students with zero, one, or two satisfy that rule. Historical arrears are a separate count of previously held arrears. They are considered only when the company enables that rule.

## 9. How do skills match?

The database has one master `skills` table and mapping tables for students and company requirements. The service trims/collapses whitespace and compares case-insensitive exact skill names using sets. `Java` does not match `JavaScript`. `C` and `C++` are different. We do not use substring matching or guess synonyms/proficiency.

## 10. Why MySQL?

The information is relational: students have many skills, companies have many required skills, and one evaluation has many detail rows. MySQL provides foreign keys, unique constraints, indexes, decimal columns, and transactions. Our schema targets MySQL 8. The environment actually demonstrated it on compatible MariaDB 11.8 using MySQL Connector/J; Oracle MySQL itself was not separately tested.

## 11. What are the main tables?

`students`, `companies`, `company_criteria`, `skills`, `student_skills`, `company_required_skills`, `company_departments`, `company_graduation_years`, `evaluations`, and `evaluation_details`.

## 12. Why save snapshots?

A company may change its minimum CGPA tomorrow. Yesterday’s saved decision must still explain the requirement used yesterday. We save the requirement, actual student value, explanation, and complete profiles at evaluation time. Deleting a current student or company sets the live foreign key to NULL but leaves the original IDs and snapshots available.

## 13. Why use a transaction?

An evaluation summary without its detail rows would be incomplete. A transaction ensures all those rows save together or none of them save. Repeatable-read isolation also means the student and criteria queries see one consistent database snapshot.

## 14. How are reports generated?

The report service reads saved evaluations through parameterized DAO queries and builds report rows. Summary and failure-analysis reports count every attempt. Student/company match lists use the latest saved evaluation for each student-company pair, so one pair is not listed as both eligible and ineligible because of repeated checks.

The CSV utility quotes commas/newlines/quotes, adds headings and generation information, and neutralizes spreadsheet formulas. The browser renders a separate clean print report; its Save as PDF option creates the PDF.

## 15. What tests did you actually run?

- 45 unit cases covering eligibility, validation, and CSV safety.
- 16 integration cases against a real JDBC database, including rollback, concurrent checks, snapshot retention, correct reports, and long skill values.
- 20 HTTP checks covering CRUD, validation, errors, exports, and query security.
- 27 Chromium workflow checks, including seven pages at three viewport sizes.
- Seven real empty-database UI checks.

All passed in the recorded final runs. The exact environment, logs, and limitations are in `docs/TEST-RESULTS.md`. These are functional checks, not a claim of production load/security certification.

## 16. What are the limitations?

No authentication, no multi-role portal, no bulk import, no automatic shortlisting, and no guarantee of a job. Notes are advisory rather than arbitrary executable rules. PDF export uses the browser. It is a trusted local single-officer demonstration; a public deployment needs proper login, HTTPS, privacy/retention policy, backups, and scaling work.

## A five-minute demonstration

1. Open **Overview** and explain that numbers come from SQL.
2. View **Aarav Sharma** and **NovaTech** criteria.
3. Run that eligible match and explain its six PASS rows.
4. Run **Dev Kapoor → NovaTech** and point out all failures.
5. Open the saved check in **Evaluation History**.
6. Show a report, download CSV, and open Print / Save PDF.
7. Open `EligibilityService.java`, point to `BigDecimal`, `Set`, optional-rule conditions, and the transaction.

**One-line conclusion:** “PlaceSure makes placement eligibility consistent, explainable, and traceable, while keeping the implementation understandable.”
