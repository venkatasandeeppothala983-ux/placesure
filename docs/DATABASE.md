# PlaceSure database design

The canonical runnable SQL is `database/database.sql` (schema and fictional input data) or `database/schema.sql` (empty schema). Target: MySQL 8.0+ / InnoDB / utf8mb4. Actually executed here: MariaDB 11.8.6 using MySQL Connector/J 8.4.0.

## Relationships

```text
students ───< student_skills >─── skills ───< company_required_skills >─── companies
   │                                                                       │
   │                                                                       ├── company_criteria (1:1)
   │                                                                       ├─< company_departments
   │                                                                       └─< company_graduation_years
   │                                                                       │
   └───────────────< evaluations >──────────────────────────────────────────┘
                            │
                            └──< evaluation_details
```

A student can have many skills; a skill can belong to many students. A company requires many skills, admits an optional list of departments and years, and has one academic criteria row. A saved evaluation belongs to one original student-company pair and has an ordered collection of criterion results.

## Core fields

- `students.id`: server-generated BIGINT primary key; register number has a UNIQUE constraint.
- CGPA values: `DECIMAL(4,2)`, constrained to 0–10. Counts are nonnegative SMALLINT; the service additionally limits counts to 100 and years to 2000–2100.
- Active arrears and historical arrears are independent stored counts. Historical means previously held arrears, not an automatic sum including the current count.
- `companies`: name, role, optional ISO drive date, display package, advisory notes.
- `company_criteria.company_id`: both primary key and foreign key, enforcing at most one row per company.
- `min_cgpa` and `max_active_arrears`: mandatory; `max_history_arrears` nullable, where NULL means do not consider history.
- Skill names have a unique case-insensitive database constraint. Java additionally collapses whitespace and uses case-normalized exact values. There is no fuzzy/subsequence skill comparison or synonym expansion.
- Mapping tables use composite primary keys to prevent duplicate links. Departments/years use actual normalized rows, not substring-matched CSV.

## Evaluation snapshots

`evaluations` stores:
- Nullable live `student_id` / `company_id` foreign keys.
- **Non-null original `student_source_id` / `company_source_id`** without cascading deletion. These preserve pair identity for latest-match reports after deletion.
- Snapshot student/company names, register number, department, and job role for straightforward indexed queries.
- JSON snapshots of the complete evaluated student and company/criteria objects.
- Eligible boolean, passed/failed counts, UTC `TIMESTAMP(6)`.

`evaluation_details` stores:
- Evaluation foreign key, a stable criterion key, readable label, position.
- Requirement and actual-value snapshots, PASS/FAIL boolean, explanation.
- UNIQUE `(evaluation_id, criterion_key)` so one rule cannot be stored twice in one result.
- TEXT snapshot columns so all 50 supported 80-character skill values fit without truncation. This boundary was integration-tested.

The application never UPDATEs completed evaluation results. Opening a result retrieves its snapshots; it does not compare current records again.

## Delete behavior

- Delete student → its skill links cascade; evaluations' live student FK becomes NULL.
- Delete company → criteria, skill, department, and year mappings cascade; evaluation live company FK becomes NULL.
- Source IDs, snapshots, outcome, and all details survive either deletion.
- Delete an evaluation directly as a database administrator → its detail rows cascade. No evaluation DELETE endpoint is exposed in the application.
- Unused master skills are retained; they remain useful suggestions and do not affect eligibility.

Retention is deliberate for traceability, not a privacy-policy implementation. A real institution must choose retention/anonymization rules before using personal data.

## Indexes and consistency

Indexes cover register uniqueness, department, names, drive dates, evaluation timestamps, original student-company pairing, company/result, department snapshots, and failed criterion queries. Index definitions are explicit in the SQL.

All value-bearing reads and writes use JDBC PreparedStatement. Only private/allowlisted SQL identifiers are composed in code. Profile+mapping writes, company+criteria writes, and evaluation+detail writes use transactions with rollback. Evaluations and multi-query reports use repeatable-read isolation to prevent mixed-version reads.

A CHECK constraint prevents a stored eligible summary with failed criteria, or an ineligible summary with zero failed criteria. The Java service computes all summary counts from its actual detail list. Database integration tests compare those counts and verify an intentional detail-insert failure rolls back the summary too.

## Reports: attempts versus latest matches

All-attempt reports read all matching snapshot rows. Latest-match reports use a `NOT EXISTS` query to exclude a row if a newer evaluation exists for the same original pair, up to the selected end date. The outcome filter applies to that latest decision, not to an arbitrary historical pass. Editing current criteria does not retroactively change any report; a new saved check is needed.

Date filters are inclusive UTC calendar days implemented as `timestamp >= from 00:00:00` and `< day-after-to 00:00:00`. This includes all microseconds on the end date while keeping the timestamp index usable.

## Demo data and safe imports

The seed input file includes 18 fictional students, five fictional companies, 14 master skills, and realistic mapping combinations. Email addresses use `example.test`. Drive dates are relative to the database's current date on import.

There are no fake eligibility inserts in SQL. `DemoSeeder` calls `EligibilityService` for up to eight students per company only when explicitly invoked with `--seed-demo` and the evaluation table is empty. The given input data yields 40 persisted checks: 16 eligible and 24 not eligible.

For a completely fresh workspace, import `schema.sql` and omit `--seed-demo`. Neither schema file automatically drops an existing database. The sample INSERTs are intended for a single import into a new database.

## Test database safety

Production/demo database: `placesure`. Integration-test database: **`placesure_test`**. Java verifies the catalog before destructive test cleanup. The HTTP test launcher also validates the exact test database name. Never grant application-schema DDL permissions just to run tests; grant them on the separate test schema only.
