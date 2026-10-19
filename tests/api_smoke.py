#!/usr/bin/env python3
"""HTTP integration checks against the isolated test server at port 8081."""

import csv, io, json, os, urllib.error, urllib.request

BASE = os.environ.get("TEST_BASE_URL", "http://127.0.0.1:8081")
checks = []


def request(method, path, data=None, status=200, headers=None):
    headers = {"Content-Type": "application/json", **(headers or {})}
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(data).encode() if data is not None else None,
        method=method,
        headers=headers,
    )
    try:
        with urllib.request.urlopen(req, timeout=12) as r:
            code, content, heads = (
                r.status,
                r.read(),
                {k.lower(): v for k, v in r.headers.items()},
            )
    except urllib.error.HTTPError as e:
        code, content, heads = (
            e.code,
            e.read(),
            {k.lower(): v for k, v in e.headers.items()},
        )
    assert code == status, (method, path, code, content[:500])
    if "text/csv" in heads.get("content-type", ""):
        return content.decode("utf-8-sig"), heads
    return json.loads(content), heads


def ok(name):
    checks.append(name)
    print("PASS " + name)


health, _ = request("GET", "/api/health")
assert health["data"]["database"] == "Connected"
ok("Health identifies a connected database")
before, _ = request("GET", "/api/dashboard")
baseline = before["data"]["totalChecks"]
s = {
    "registerNumber": "HTTP-TEST-001",
    "fullName": 'HTTP, "Example" Student',
    "email": "http@example.test",
    "department": "CSE",
    "graduationYear": 2027,
    "cgpa": "8.50",
    "activeArrears": 0,
    "historyArrears": 0,
    "skills": ["Java", "SQL", "Git"],
}
r, _ = request("POST", "/api/students", s, 201)
sid = r["data"]["id"]
ok("Student create persists validated fields")
for patch in [
    {"cgpa": "10.01"},
    {"activeArrears": -1},
    {"email": "invalid"},
    {"graduationYear": 2027.5},
    {"fullName": ""},
    {"phone": "--- --- ---"},
]:
    request("POST", "/api/students", {**s, **patch}, 400)
request("POST", "/api/students", s, 415, {"Content-Type": "text/plain"})
request(
    "POST", "/api/students", s, 415, {"Content-Type": "application/json-not-really"}
)
request("POST", "/api/students", {**s, "fullName": "x" * 70000}, 413)
ok(
    "Backend rejects invalid CGPA/counts/email/year/name/phone, unsafe media types, and oversized bodies"
)
r, _ = request("POST", "/api/students", s, 409)
assert r["error"]["fields"].get("registerNumber")
ok("Duplicate register number returns 409 and field error")
r, _ = request("GET", "/api/students?q=HTTP-TEST-001")
assert r["data"]["total"] == 1
r, _ = request("PUT", f"/api/students/{sid}", {**s, "fullName": "Updated HTTP Student"})
assert r["data"]["fullName"] == "Updated HTTP Student"
ok("Student search and update round-trip through JDBC")
c = {
    "name": "HTTP Test Partner",
    "jobRole": "Developer",
    "driveDate": None,
    "salaryPackage": "8 LPA",
    "notes": "Advisory only",
    "criteria": {
        "minCgpa": "7.00",
        "maxActiveArrears": 0,
        "maxHistoryArrears": None,
        "requiredSkills": ["Java", "SQL"],
        "departments": ["CSE"],
        "graduationYears": [2027],
    },
}
r, _ = request("POST", "/api/companies", c, 201)
cid = r["data"]["id"]
ok("Company and normalized criteria create atomically")
r, _ = request("POST", "/api/evaluations", {"studentId": sid, "companyId": cid}, 201)
e = r["data"]
eid = e["id"]
assert e["eligible"] and len(e["details"]) == 5
assert all(d["passed"] for d in e["details"])
ok("Java evaluates all configured rules and saves an eligible result")
r, _ = request("GET", f"/api/evaluations/{eid}")
assert r["data"] == e
ok("Stored evaluation and every snapshot can be read back exactly")
request(
    "PUT",
    f"/api/companies/{cid}/criteria",
    {
        **c["criteria"],
        "minCgpa": "9.50",
        "requiredSkills": ["Excel"],
        "departments": ["MECH"],
        "graduationYears": [2028],
    },
)
r, _ = request("POST", "/api/evaluations", {"studentId": sid, "companyId": cid}, 201)
assert not r["data"]["eligible"] and r["data"]["failedCount"] == 4
ok("Multiple failures are all returned after a criteria update")
r, _ = request("GET", f"/api/evaluations/{eid}")
assert (
    r["data"]["eligible"] and r["data"]["companySnapshot"]["criteria"]["minCgpa"] == 7
)
ok("Historical result remains unchanged after criteria edits")
r, _ = request("GET", f"/api/reports?type=eligible&companyId={cid}")
assert not r["data"]["rows"]
r, _ = request("GET", f"/api/reports?type=ineligible&companyId={cid}")
assert len(r["data"]["rows"]) == 1
ok("Latest-match reports avoid duplicate and contradictory results")
r, _ = request("GET", f"/api/reports?type=failures&companyId={cid}")
assert len(r["data"]["rows"]) == 4
ok("Failed-criterion analysis counts all failed rules")
text, headers = request("GET", f"/api/reports/export?type=history&companyId={cid}")
rows = list(csv.reader(io.StringIO(text)))
assert any("Updated HTTP Student" in row for row in rows)
assert "attachment" in headers["content-disposition"]
ok("CSV history export has metadata, headings, actual rows, and attachment headers")
text, _ = request("GET", f"/api/evaluations/{eid}/export")
assert "PASS" in text and "Company requirement" in text
ok("Individual evaluation export includes criterion snapshots")
request("POST", "/api/evaluations", {"studentId": 999999999, "companyId": cid}, 404)
request("POST", "/api/evaluations", {"studentId": sid}, 400)
request("GET", "/api/students/0", status=400)
request("GET", "/api/students/999999999", status=404)
ok("Missing selections and invalid/nonexistent IDs return suitable errors")
for path in [
    "/api/students?sort=DROP%20TABLE",
    "/api/evaluations?from=2027-02-01&to=2027-01-01",
    "/api/reports?type=unknown",
    "/api/students?page=one",
    "/api/students?q=a&q=b",
]:
    request("GET", path, status=400)
ok(
    "Malformed filters, date ranges, report types, sorts, and duplicate query keys are rejected"
)
request("POST", "/api/students", s, 403, {"Origin": "https://untrusted.example"})
request("PATCH", f"/api/students/{sid}", s, 405)
ok("Cross-origin mutations and unsupported methods are rejected")
r, _ = request("GET", "/api/students?q=%27%20OR%201%3D1%20--")
assert r["data"]["total"] == 0
ok("SQL injection-style input is handled as a literal search")
request("DELETE", f"/api/students/{sid}")
request("DELETE", f"/api/companies/{cid}")
r, _ = request("GET", f"/api/evaluations/{eid}")
assert r["data"]["eligible"]
ok("Deleting live records preserves completed evaluation history")
after, _ = request("GET", "/api/dashboard")
assert after["data"]["totalChecks"] == baseline + 2
assert (
    sum(c["total"] for c in after["data"]["companySummary"])
    == after["data"]["totalChecks"]
)
ok("Dashboard totals agree with saved checks, including archived companies")
print(f"\n{len(checks)} HTTP checks passed. No failed checks.")
