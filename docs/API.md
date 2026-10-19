# PlaceSure REST API

Base URL: `/api`. Success: `{ "data": ... }`. Errors: `{ "error": { "code": "...", "message": "...", "fields": {}, "requestId": "..." } }`.

Use `Content-Type: application/json` for POST/PUT, and same-origin browser requests. Request bodies are capped at 64 KB. There is no authentication: use only on a trusted network. Origin validation is not a replacement for login/authorization.

## Endpoints

| Method | Path | Behavior |
|---|---|---|
| GET | `/health` | Actual DB connectivity, server product/version, Java version, application version |
| GET | `/meta` | Current record counts, skills/departments, current and archived IDs for filters |
| GET | `/dashboard` | Live SQL totals, company breakdown, recent checks/students, upcoming drives |
| GET | `/students` | Searchable, filtered, sorted, paginated directory |
| POST | `/students` | Validate and create a profile and normalized skills atomically |
| GET | `/students/{id}` | Full profile |
| PUT | `/students/{id}` | Replace all editable fields and skill mappings atomically |
| DELETE | `/students/{id}` | Delete current profile; preserve completed evaluations |
| GET | `/companies` | Searchable, filtered, sorted opportunities with criteria |
| POST | `/companies` | Create company plus its full criteria and mapping rows atomically |
| GET | `/companies/{id}` | Full opportunity and criteria |
| PUT | `/companies/{id}` | Replace profile and all criteria atomically |
| DELETE | `/companies/{id}` | Delete opportunity and its live criteria; preserve evaluations |
| GET | `/companies/{id}/criteria` | Read the criterion configuration |
| PUT | `/companies/{id}/criteria` | Validate/replace or create criteria for an existing company |
| POST | `/evaluations` | Fetch the live records, evaluate in Java, persist summary and all snapshots atomically |
| GET | `/evaluations` | Search/filter/paginate saved summaries |
| GET | `/evaluations/{id}` | Full original snapshots and criterion breakdown, never recomputed |
| GET | `/evaluations/{id}/export` | Download criterion-by-criterion CSV from the saved result |
| GET | `/reports` | Generate one of six reports from stored results |
| GET | `/reports/export` | Same report as quoted/formula-safe UTF-8 CSV |

Creating/deleting a company's mandatory criteria is part of company creation/deletion. Optional criteria are removed by PUT with `maxHistoryArrears: null` or an empty list. There is intentionally no standalone DELETE that would silently remove mandatory academic rules and make incomplete criteria look valid.

The SPA is served at `/`; local CSS/JavaScript/fonts are served by the same process. `/docs/README.md`, `/docs/VIVA.md`, `/docs/API.md`, `/docs/ARCHITECTURE.md`, `/docs/TEST-RESULTS.md`, and `/docs/database.sql` are allowlisted documentation downloads.

## Pagination and queries

Paged responses:

```json
{"data":{"items":[],"page":1,"size":10,"total":0,"totalPages":1}}
```

`page`: positive integer (maximum 1,000,000); `size`: 1–200, default 10. Pages beyond the last populated page clamp to the last page. User-controlled values are bound through PreparedStatement; SQL sort fields are selected from allowlists.

### Students

- `q`: name or register number; substring search with SQL wildcard characters escaped.
- `department`: exact department (database case-insensitive collation).
- `minCgpa`: inclusive minimum 0–10, max two decimal places.
- `maxArrears`: inclusive active-arrears maximum, integer 0–100.
- `sort`: `name`, `registerNumber`, `cgpa`, `graduationYear`, or `createdAt`.
- `direction`: `asc` or `desc`.

Example: `/students?q=aarav&department=CSE&minCgpa=7&maxArrears=0&sort=cgpa&direction=desc&page=1&size=10`.

### Companies

- `q`: company name or job role.
- `department`: companies accepting that department **or having no department restriction**.
- `maxCgpa`: companies whose configured minimum CGPA is at most this value.
- `sort`: `name`, `driveDate`, `minCgpa`, or `createdAt`; plus `direction`.
- `page`, `size` as above.

### Evaluations and reports

- `q`: snapshot student name/register.
- `studentId`, `companyId`: original source IDs (also work after live records are deleted).
- `department`: evaluated department snapshot.
- `status`: `eligible` or `ineligible`.
- `from`, `to`: inclusive **UTC** dates in `YYYY-MM-DD`; start cannot be after end.
- History pagination is newest first by evaluation ID.
- Reports accept `type`: `summary`, `student`, `eligible`, `ineligible`, `failures`, `history`.
- Fixed eligible/ineligible report types enforce their own status; the interface disables the redundant status filter.
- Reports return all matching rows up to a safe maximum of 10,000 evaluation attempts; UI pagination is applied to the returned report rows. Exceeding the limit returns a readable 400, not a truncated export.

Latest-match reports (`student`, `eligible`, `ineligible`) first identify the latest saved evaluation per original pair as of `to`, then apply its outcome. Other reports count all attempts. Failure-analysis counts each failed criterion once per evaluated attempt; percentages may sum above 100%.

## Student request

```json
{
  "registerNumber": "PS27CS099",
  "fullName": "Example Student",
  "email": "example.student@example.test",
  "department": "CSE",
  "graduationYear": 2027,
  "cgpa": "8.20",
  "activeArrears": 0,
  "historyArrears": 1,
  "skills": ["Java", "SQL", "Git"],
  "phone": null
}
```

IDs and creation timestamps are server-generated. Register numbers become uppercase and must be unique. Profile fields are required except phone; the skills list may be empty (it will not satisfy a company requiring skills). Counts must be whole numbers from 0–100; years 2000–2100. CGPA must be 0–10 with at most two significant decimal places after trailing zero normalization.

Skill collections accept up to 50 values, each 1–80 characters. Whitespace and case-equivalent duplicates are normalized. No substring matching occurs. Full profile creation/update is atomic with its mapping changes.

## Company request

```json
{
  "name": "Example Partner",
  "jobRole": "Software Engineer",
  "driveDate": "2026-11-04",
  "salaryPackage": "8–10 LPA",
  "notes": "Bring a project portfolio. Advisory information only.",
  "criteria": {
    "minCgpa": "7.00",
    "maxActiveArrears": 0,
    "maxHistoryArrears": null,
    "requiredSkills": ["Java", "SQL"],
    "departments": ["CSE", "IT"],
    "graduationYears": [2026, 2027]
  }
}
```

`minCgpa` and `maxActiveArrears` are mandatory. `null` history maximum = disabled; zero = enabled, maximum zero. Empty optional arrays mean no restriction. Job-role/name maximum 120 characters; package maximum 80; notes maximum 2,000. Notes do not add automated rules. Dates use valid ISO dates between years 2000 and 2100.

PUT `/companies/{id}/criteria` accepts only the inner `criteria` object, not its wrapper.

## Evaluation request and result

```bash
curl -X POST http://localhost:8080/api/evaluations \
  -H 'Content-Type: application/json' \
  -d '{"studentId":1,"companyId":1}'
```

The 201 response contains `id`, original IDs/identity, `eligible`, `passedCount`, `failedCount`, `evaluatedAt`, `studentSnapshot`, `companySnapshot`, and ordered `details`. Each detail has `key`, `criterion`, `actual`, `requirement`, `passed`, and `explanation`. The server fetches the values from MySQL; a caller cannot submit an eligibility result.

This operation is not idempotent: each deliberate check creates a new historical attempt. It never silently recalculates or overwrites an older evaluation.

## Reports and CSV

```bash
curl 'http://localhost:8080/api/reports?type=eligible&companyId=1'
curl 'http://localhost:8080/api/reports/export?type=history&department=CSE' \
  -o placesure-history.csv
curl 'http://localhost:8080/api/evaluations/1/export' -o evaluation.csv
```

JSON report: `title`, UTC `generatedAt`, explicit `scope`, `columns` (`key`,`label`), `rows`, and `summary`. `summary.basis` identifies all attempts versus latest matches. CSV includes metadata before the header row, safe double-quote escaping, CRLF, a UTF-8 BOM, and formula-neutralization for dangerous text cells.

Print/PDF is a frontend print-friendly document, not a server `/pdf` API. All printed result and report data still comes from these stored backend records.

## Error contract

| Status | Examples |
|---:|---|
| 400 | Missing required field, invalid/fractional count, bad CGPA, invalid ID/query/JSON, reversed date range, report too large |
| 403 | Cross-origin browser mutation |
| 404 | Student/company/evaluation/resource does not exist |
| 405 | Unsupported endpoint method |
| 409 | Duplicate register number or conflicting related data |
| 413 | Request exceeds 64 KB |
| 415 | Mutation body is not `application/json` |
| 500 | Safe generic operation failure; no stack trace/credentials in response |
| 503 | Database unreachable, credentials/schema not ready |

The request ID can be matched with sanitized server logs. No database password, JDBC URL, SQL text, or internal stack trace is sent to the UI.
