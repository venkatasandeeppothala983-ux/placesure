# PlaceSure architecture

One Java process serves the REST API and locally bundled HTML/CSS/JavaScript. A MySQL 8 database stores every operational record. MariaDB 11.8 is the compatible local demonstration server. Java is compiled for release 17 and runs on Java 17+.

```
Browser → HTTP controllers → validation/services → JDBC DAOs → MySQL
                               ↓
                        EligibilityService
                   BigDecimal + normalized Sets
                               ↓
               evaluation + immutable detail snapshots
```

No eligibility calculation occurs in JavaScript. Student and company data are read in one repeatable-read database transaction. The service evaluates every configured rule, then atomically saves the result and all detail rows. Missing or invalid mandatory data produces failure, never success. Optional criteria are omitted when unset.

Students and companies can be edited or deleted without altering completed evaluations. Foreign keys become NULL on deletion; original source IDs, names, profile/criteria JSON snapshots, and all detail snapshots remain. Live dashboards count completed evaluations. Company/student match reports use the latest evaluation per original student-company pair; history and failure-analysis reports include every attempt. This distinction is always labeled.

Skills have a normalized master table and many-to-many mapping tables. Skill matching uses case-insensitive, whitespace-normalized exact values, not substring matching. C and C++ remain distinct. Company departments and graduation years are normalized mapping tables.

Security boundary: this is a single-officer, unauthenticated local project. It must not be exposed to untrusted networks without authentication, TLS, and access control. Requests are bounded, SQL values are parameterized, sorting is allowlisted, HTML is escaped, CSV is quoted and formula-neutralized, and mutation requests reject cross-origin browser requests. Credentials live in environment variables or an ignored .env file.

Use browser printing for PDF: report and evaluation views have clean, standalone print layouts. No external service is required at runtime.
