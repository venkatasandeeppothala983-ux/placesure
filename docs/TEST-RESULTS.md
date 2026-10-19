# PlaceSure — executed test results

**Date:** 18 October 2026. These are executed functional checks, not projected results or a claim of production certification.

## Environment actually used

| Component | Actual environment |
|---|---|
| OS | Debian GNU/Linux 13 |
| Java | OpenJDK 21.0.12.1; Maven compiler release 17 (class major version 61 verified) |
| Maven | 3.9.9 |
| Database | MariaDB 11.8.6, InnoDB, accessed using MySQL Connector/J 8.4.0 |
| Unit test framework | JUnit Jupiter 5.11.4 |
| Browser automation | Playwright 1.64.0, bundled headless Chromium 156 |
| Python | 3.13.16 |
| Main app | Port 8080, database `placesure` |
| Isolated populated/empty workflow instance | Port 8081, database `placesure_test` |
| Real connection-failure instance | Port 8082, intentionally unreachable database port |

Oracle MySQL and a standalone Java 17 runtime were **not** executed here. The schema targets MySQL 8, and Java bytecode targets 17. No H2/SQLite or mocked database was substituted for the JDBC integration suite.

## Final completed runs

| Suite | Executed result | Raw evidence |
|---|---|---|
| `mvn test` | **45 passed; 0 failures; 0 errors; 0 skipped** | `reports/unit-test-run.txt` |
| `mvn -Pintegration verify` | **16 integration cases passed; 0 failures/errors/skips**; also runs the unit suite | `reports/integration-test-run.txt` |
| `python3 tests/api_smoke.py` | **20 HTTP workflow checks passed** | `reports/http-test-run.txt` |
| `python3 tests/browser_smoke.py` | **27 Chromium workflow checks passed**, including 21 page/viewport combinations | `reports/browser-test-run.txt` |
| `python3 tests/empty_state_smoke.py` | **7 real empty-database page checks passed** | `reports/empty-state-test-run.txt` |
| Fresh-database setup helper | Scratch database created, SQL imported, restricted app user read **18 students / 5 companies**; scratch objects then removed | Executed `scripts/setup-local-db.py` with isolated names |

Unit breakdown: 28 eligibility cases, 17 validation/CSV cases (parameterized invocations included). Integration tests ran against a separate database, cleared fixtures before each case, and verified the exact database catalog before cleanup. HTTP/browser test data never modified the main demo database.

## Requested minimum scenarios — evidence mapping

| Required scenario | Actual tests |
|---|---|
| 1. Student meets all requirements | `studentMeetsEveryConfiguredRequirement`; populated browser eligible flow |
| 2. CGPA below minimum | `cgpaBelowMinimumFails` |
| 3. Active arrears above maximum | `activeArrearsAreAMaximumNotAnEquality` |
| 4. Required skill missing | `missingRequiredSkillFailsAndNamesIt`; Java/JavaScript and C/C++ exact-match tests |
| 5. Department fails | `departmentMismatchFails` |
| 6. Multiple failures | `allFailuresAreCollectedNotJustTheFirst`; HTTP/browser ineligible flows |
| 7. Student incomplete | Missing CGPA, mandatory identity, skills, historical data, and null-student cases |
| 8. Company criteria incomplete/invalid | Missing criteria/minimum/active maximum and malformed skill/limit configuration cases |
| 9. CGPA exactly at minimum | `cgpaExactlyEqualToMinimumPassesUsingDecimalArithmetic` |
| 10. Evaluation saves correctly | `completeEvaluationAndEveryDetailArePersistedTogether`; exact HTTP GET round-trip |
| 11. History survives criteria changes | `changingCriteriaDoesNotRewriteHistoricalResults`; profile-edit and delete-retention tests |
| 12. Reports correct | `reportsReturnCorrectSummaryAndEveryFailedCriterion`; latest-match, date/department, CSV, and six browser export checks |

## Additional verified boundaries and reliability

- Case/whitespace-normalized exact skill matching, optional unconfigured rules, zero historical maximum, invalid/unknown values, accepted graduation years.
- Safe BigDecimal comparison; out-of-range and excess-precision CGPAs fail.
- Backend rejects negative/fractional counts, invalid years/emails/phone values, missing names, duplicate register numbers, wrong media types, and bodies over 64 KB.
- A deliberately duplicated detail key causes a real SQL constraint error; both summary and detail writes roll back.
- Eight concurrent checks create eight summaries and 48 detail rows, with matching dashboard counts. This is an atomicity check, **not** a load benchmark.
- All 50 supported 80-character skill names fit stored snapshots without truncation.
- Updating and deleting live records preserves original evaluation identities, outcomes, and requirement/value snapshots.
- Latest-match reports supersede an older pass with a newer failure and do not list the same pair contradictorily.
- Inclusive UTC date filters and department filters return the correct rows; reversed date ranges are rejected. The UI restores the last valid range rather than printing stale rows under invalid filter labels.
- CSV quotes commas, embedded quotes, and newlines; formula-like strings are neutralized; real CSV downloads include title/date/scope/headings.
- User-supplied HTML in names and advisory notes displays as text rather than creating executable elements.
- SQL injection-shaped searches are treated as literal values. Sort fields, report types, duplicate query parameters, unsupported methods, and cross-origin mutation requests are checked.
- Print tabs open complete evaluation/report data; Chromium generated actual nonempty A4 PDFs, and the print control invokes `window.print`.
- Global keyboard search, modal focus/inert behavior, Escape, delete cancellation, and saved page-size preference were exercised.
- A genuinely unreachable JDBC endpoint returns a safe connection screen and setup help. This was **not** a mocked successful database response.
- An empty real test database produces zero dashboard counts and useful empty states on every page.
- No uncaught JavaScript errors were recorded in the completed browser suite.

## Responsive matrix

All seven routes rendered with a real backend, visible expected page heading, and no document-level horizontal overflow at:

- **Desktop:** 1440 × 1000
- **Tablet:** 768 × 1024
- **Mobile:** 390 × 844

Wide tables intentionally scroll inside their own containers on smaller screens. The mobile menu opens, navigates, and closes. These are viewport tests in Chromium, not tests on physical phones.

## Reproduction order

1. Configure `placesure_test` credentials using README. Run `mvn test` and `mvn -Pintegration verify`.
2. Start the isolated test server without resetting data if checking empty states: `python3 tests/run_test_server.py` (do not use `--seed-demo` inputs yet; the table remains empty after integration tests).
3. Run `python3 tests/empty_state_smoke.py` against the empty test database.
4. Stop that test instance. Start `python3 tests/run_test_server.py --reset-demo` to import populated inputs and compute demo checks.
5. Separately start `python3 tests/run_test_server.py --unavailable` for the real connection-failure screen.
6. Run the HTTP suite, then the browser suite. Stop the test processes when finished.

The explicit `--reset-demo` command is destructive **only within the guarded `placesure_test` database**. No reset or evaluation-delete action is exposed in the main UI.

## Issues found and resolved during development

Full workflow testing caught a running-JAR replacement/classloader problem, which was fixed by using immutable runtime copies; export-error handling now shows a toast instead of downloading JSON; asynchronous dialogs are cancelled correctly on Escape/navigation; invalid date changes revert; maximum supported skill lists now fit TEXT snapshots. Test-harness header casing and strict selector ambiguities were also corrected before the clean final runs.

## Honest limits

No percentage code-coverage claim, full accessibility audit, penetration test, multi-user production load test, authentication test, backup/restore certification, Firefox/Safari run, physical-device run, or Oracle MySQL execution is claimed. Browser-assisted PDF export is verified; there is no server PDF API. All demo identities are fictional.
