#!/usr/bin/env python3
"""Real Chromium UI workflows on the isolated test database. Install Playwright first."""

import datetime, json, os, pathlib, urllib.request, uuid
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get("TEST_BASE_URL", "http://127.0.0.1:8081")
ROOT = pathlib.Path(__file__).resolve().parents[1]
ART = ROOT / ".runtime" / "browser-tests"
ART.mkdir(parents=True, exist_ok=True)
checks = []
errors = []


def ok(name):
    checks.append(name)
    print("PASS " + name, flush=True)


def api(path):
    with urllib.request.urlopen(BASE + "/api" + path, timeout=10) as r:
        return json.load(r)["data"]


def nav(page, route, title):
    page.locator(f'#navigation a[href="#/{route}"]').click()
    expect(page.locator("main h1")).to_have_text(title)


def fill_student(page, reg, name="Browser Demo Student"):
    for key, value in {
        "fullName": name,
        "registerNumber": reg,
        "email": "browser@example.test",
        "department": "CSE",
        "graduationYear": "2027",
        "cgpa": "8.75",
        "activeArrears": "0",
        "historyArrears": "0",
    }.items():
        page.locator('[name="' + key + '"]').fill(value)


def empty_filter(page, selector, value):
    page.locator(selector).select_option(value)
    page.wait_for_load_state("networkidle")


with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(
        viewport={"width": 1440, "height": 1050}, accept_downloads=True
    )
    page = context.new_page()
    page.on("pageerror", lambda e: errors.append(str(e)))
    page.goto(BASE, wait_until="networkidle")
    expect(page.locator("main h1")).to_have_text("Overview")
    d = api("/dashboard")
    assert [
        int(x.replace(",", ""))
        for x in page.locator(".stat-number").all_text_contents()
    ] == [
        d["totalStudents"],
        d["totalCompanies"],
        d["totalChecks"],
        d["eligibleMatches"],
    ]
    assert page.locator("canvas").count() == 2
    ok("Dashboard cards and charts render actual database totals")
    nav(page, "students", "Students")
    page.locator("#student-search").fill("Aarav")
    expect(page.locator(".student-table tbody tr")).to_have_count(1)
    expect(page.locator(".student-table")).to_contain_text("Aarav Sharma")
    page.locator("#student-search").fill("")
    expect(page.locator(".student-table tbody tr")).to_have_count(10)
    empty_filter(page, "#student-department", "IT")
    expect(page.locator(".student-table tbody tr")).to_have_count(4)
    empty_filter(page, "#student-cgpa", "8")
    expect(page.locator(".student-table tbody tr")).to_have_count(2)
    empty_filter(page, "#student-arrears", "0")
    expect(page.locator(".student-table tbody tr")).to_have_count(2)
    ok("Student search, department, CGPA, and arrears filters work together")
    empty_filter(page, "#student-department", "")
    empty_filter(page, "#student-cgpa", "")
    empty_filter(page, "#student-arrears", "")
    empty_filter(page, "#student-sort", "cgpa:desc")
    expect(page.locator(".student-table tbody tr").first).to_contain_text("Nisha Patel")
    page.get_by_role("button", name="Page 2", exact=True).click()
    expect(page.locator(".pagination")).to_contain_text("11–18")
    ok("Student sorting and server-side pagination work")
    page.locator('[data-action="add-student"]').click()
    expect(page.get_by_role("dialog")).to_be_visible()
    reg = "BROWSER-" + uuid.uuid4().hex[:9].upper()
    name = "Browser <img src=x onerror=alert(1)> Demo"
    fill_student(page, reg, name)
    page.locator('[name="cgpa"]').fill("11")
    page.locator('#student-form [type="submit"]').click()
    assert page.locator('[name="cgpa"]').evaluate("(e)=>!e.validity.valid")
    expect(page.get_by_role("dialog")).to_be_visible()
    page.locator('[name="cgpa"]').fill("8.75")
    page.locator('[name="activeArrears"]').fill("-1")
    page.locator('#student-form [type="submit"]').click()
    assert page.locator('[name="activeArrears"]').evaluate("(e)=>!e.validity.valid")
    page.locator('[name="activeArrears"]').fill("0")
    page.locator(".tag-entry").fill("Java, SQL, Git")
    page.locator(".tag-entry").press("Enter")
    expect(page.locator("[data-tag-value]")).to_have_count(3)
    page.locator('#student-form [type="submit"]').click()
    expect(page.get_by_role("dialog")).to_have_count(0)
    ok(
        "Student modal validates bounds and creates a profile with normalized skill tags"
    )
    page.locator("#student-search").fill(reg)
    expect(page.locator(".student-table tbody tr")).to_have_count(1)
    expect(page.locator(".student-table")).to_contain_text(name)
    assert page.locator("main img").count() == 0
    ok("User-controlled HTML is displayed as text, not executed")
    sid = api("/students?q=" + reg)["items"][0]["id"]
    page.locator('button.icon-button[data-action="view-student"]').click()
    expect(page.get_by_role("dialog")).to_contain_text("8.75")
    page.locator('#modal-root [data-action="edit-student"]').click()
    page.locator('[name="fullName"]').fill("Browser Demo Edited")
    page.locator('[name="cgpa"]').fill("8.90")
    page.locator('#student-form [type="submit"]').click()
    expect(page.get_by_role("dialog")).to_have_count(0)
    expect(page.locator(".student-table")).to_contain_text("Browser Demo Edited")
    assert api("/students/" + str(sid))["cgpa"] == 8.9
    ok("View and edit student dialogs persist changes")
    page.locator('[data-action="add-student"]').click()
    fill_student(page, reg)
    page.locator('#student-form [type="submit"]').click()
    expect(page.locator(".form-error")).to_be_visible()
    expect(page.locator(".form-error")).to_contain_text("already in use")
    page.locator('#modal-root [data-action="close-modal"]').first.click()
    ok("Duplicate-register errors are explained inside the form")
    page.locator('[data-action="delete-student"]').click()
    page.get_by_role("button", name="Keep student", exact=True).click()
    expect(page.locator(".student-table tbody tr")).to_have_count(1)
    page.locator('[data-action="delete-student"]').click()
    page.locator('[data-action="confirm-delete"]').click()
    expect(page.locator(".empty-state h3")).to_have_text("No matching profiles")
    ok("Student deletion requires confirmation; cancellation keeps the profile")
    nav(page, "companies", "Companies")
    page.locator('[data-action="add-company"]').click()
    for key, value in {
        "name": "Browser QA Partner",
        "jobRole": "Quality Engineer",
        "minCgpa": "7.00",
        "maxActiveArrears": "0",
        "departments": "CSE, IT",
        "graduationYears": "2027",
        "salaryPackage": "6–8 LPA",
        "driveDate": "2026-11-03",
        "notes": "Advisory <b>not HTML</b>",
    }.items():
        page.locator('[name="' + key + '"]').fill(value)
    page.locator("#consider-history").check()
    page.locator('[name="maxHistoryArrears"]').fill("1")
    page.locator(".tag-entry").fill("Java, SQL")
    page.locator(".tag-entry").press("Enter")
    page.locator('#company-form [type="submit"]').click()
    expect(page.get_by_role("dialog")).to_have_count(0)
    page.locator("#company-search").fill("Browser QA")
    expect(page.locator(".company-card")).to_have_count(1)
    cid = api("/companies?q=Browser%20QA")["items"][0]["id"]
    assert api("/companies/" + str(cid))["criteria"]["maxHistoryArrears"] == 1
    ok("Company form saves its profile and all configurable criteria")
    page.locator('[data-action="view-company"]').click()
    expect(page.get_by_role("dialog")).to_contain_text("Advisory <b>not HTML</b>")
    assert page.locator("#modal-root b").count() == 0
    page.locator('#modal-root [data-action="edit-company"]').click()
    page.locator("#consider-history").uncheck()
    page.locator('[name="minCgpa"]').fill("6.50")
    page.locator('#company-form [type="submit"]').click()
    expect(page.get_by_role("dialog")).to_have_count(0)
    assert api("/companies/" + str(cid))["criteria"]["maxHistoryArrears"] is None
    ok(
        "Company edits can disable optional historical-arrears rules without inventing failures"
    )
    page.locator('[data-action="delete-company"]').click()
    page.get_by_role("button", name="Keep company", exact=True).click()
    expect(page.locator(".company-card")).to_have_count(1)
    page.locator('[data-action="delete-company"]').click()
    page.locator('[data-action="confirm-delete"]').click()
    expect(page.locator(".company-card")).to_have_count(0)
    ok("Company deletion has the same safe confirmation flow")
    nav(page, "checker", "Eligibility Checker")
    expect(page.locator("#evaluate-button")).to_be_disabled()
    page.locator("#student-picker").fill("Aarav")
    page.locator('[data-action="choose-student"]').filter(
        has_text="Aarav Sharma"
    ).click()
    page.locator("#company-picker").fill("NovaTech")
    page.locator('[data-action="choose-company"]').filter(has_text="NovaTech").click()
    expect(page.locator("#evaluate-button")).to_be_enabled()
    expect(page.locator(".checker-selection")).to_contain_text("8.72")
    page.locator("#evaluate-button").click()
    expect(page.locator(".result-eyebrow")).to_have_text("ELIGIBLE MATCH")
    expect(page.locator(".criteria-table tbody tr")).to_have_count(6)
    assert page.locator(".criteria-table .danger").count() == 0
    ok(
        "Searchable selectors drive a real Java evaluation with six explained PASS results"
    )
    eid = page.locator('[data-action="export-evaluation"]').get_attribute("data-id")
    with page.expect_download() as result:
        page.locator('[data-action="export-evaluation"]').click()
    file = ART / "evaluation.csv"
    result.value.save_as(file)
    assert "Company requirement" in file.read_text(encoding="utf-8-sig")
    ok("Evaluation CSV downloads successfully from the result screen")
    with page.expect_popup() as popup:
        page.locator('[data-action="print-evaluation"]').click()
    printed = popup.value
    expect(printed.locator("h1")).to_have_text("Placement eligibility report")
    expect(printed.locator("tbody tr")).to_have_count(6)
    printed.pdf(path=str(ART / "evaluation.pdf"), format="A4", print_background=True)
    assert (ART / "evaluation.pdf").stat().st_size > 1000
    printed.evaluate("window.print=()=>{window.didPrint=true}")
    printed.locator("#print").click()
    assert printed.evaluate("window.didPrint")
    printed.close()
    ok("Print-friendly evaluation opens, invokes printing, and produces a valid A4 PDF")
    page.locator('[data-action="change-student"]').click()
    page.locator("#student-picker").fill("Dev Kapoor")
    page.locator('[data-action="choose-student"]').filter(has_text="Dev Kapoor").click()
    page.locator("#evaluate-button").click()
    expect(page.locator(".result-eyebrow")).to_have_text("NOT ELIGIBLE")
    assert page.locator(".criteria-table .badge.danger").count() >= 3
    ok("Ineligible workflow exposes every failed requirement, not only the first")
    nav(page, "history", "Evaluation History")
    page.locator("#history-search").fill("Aarav")
    page.locator("#history-company").select_option("1")
    page.locator("#history-status").select_option("eligible")
    today = datetime.datetime.now(datetime.timezone.utc).date().isoformat()
    page.locator("#history-from").fill(today)
    page.locator("#history-from").dispatch_event("change")
    page.wait_for_load_state("networkidle")
    page.locator("#history-to").fill(today)
    page.locator("#history-to").dispatch_event("change")
    page.wait_for_load_state("networkidle")
    tomorrow = (
        datetime.datetime.now(datetime.timezone.utc).date() + datetime.timedelta(days=1)
    ).isoformat()
    page.locator("#history-from").fill(tomorrow)
    page.locator("#history-from").dispatch_event("change")
    expect(page.locator("#history-from")).to_have_value(today)
    expect(page.locator(".toast.error").last).to_contain_text(
        "start date must not be later"
    )
    expect(page.locator(".history-table tbody tr")).not_to_have_count(0)
    page.locator('[data-action="view-evaluation"]').first.click()
    expect(page.get_by_role("dialog")).to_contain_text("immutable snapshot")
    expect(page.locator("#modal-root .criteria-table tbody tr")).to_have_count(6)
    page.keyboard.press("Escape")
    ok("History filters and snapshot-detail dialogs work with the saved results")
    nav(page, "reports", "Reports")
    titles = {
        "summary": "Overall eligibility summary",
        "student": "Student-wise eligibility report",
        "eligible": "Company-wise eligible students",
        "ineligible": "Company-wise ineligible students",
        "failures": "Failed-criterion analysis",
        "history": "Evaluation history report",
    }
    for typ, title in titles.items():
        page.locator(f'[data-action="report-type"][data-type="{typ}"]').click()
        expect(page.locator(".report-meta h2")).to_have_text(title)
        with page.expect_download() as result:
            page.locator('[data-action="export-report"]').click()
        file = ART / (typ + ".csv")
        result.value.save_as(file)
        assert title in file.read_text(encoding="utf-8-sig")
    ok(
        "All six report types render and export CSV with the correct titles and actual rows"
    )
    page.locator("#report-company").select_option("1")
    page.wait_for_load_state("networkidle")
    page.locator("#report-student").select_option("1")
    page.wait_for_load_state("networkidle")
    page.locator("#report-department").select_option("CSE")
    page.wait_for_load_state("networkidle")
    expect(page.locator(".report-table")).to_contain_text("Aarav Sharma")
    assert "Filters: companyId=1" in page.locator(".report-scope").inner_text()
    ok("Report company, student, and department filters share the server query model")
    with page.expect_popup() as popup:
        page.locator('[data-action="print-report"]').click()
    printed = popup.value
    expect(printed.locator("h1")).to_have_text("Evaluation history report")
    assert printed.locator("tbody tr").count() > 0
    printed.pdf(path=str(ART / "report.pdf"), format="A4")
    printed.close()
    ok("Multi-row reports also have a working printable PDF layout")
    page.evaluate("download('/api/reports/export?type=not-a-report')")
    expect(page.locator(".toast.error").last).to_contain_text("valid report type")
    ok("A failed export gives a readable toast instead of downloading an error file")
    page.keyboard.press("Control+k")
    page.locator("#command-search").fill("Diya")
    page.locator(".command-result").filter(has_text="Diya Menon").click()
    expect(page.locator("#modal-title")).to_have_text("Student profile")
    expect(page.get_by_role("dialog")).to_contain_text("Diya Menon")
    assert page.locator("main").evaluate('(e)=>e.closest(".app-shell").inert')
    page.keyboard.press("Escape")
    expect(page.get_by_role("dialog")).to_have_count(0)
    ok("Keyboard global search opens real records and dialogs manage focus safely")
    nav(page, "settings", "Settings & About")
    expect(page.locator("main")).to_contain_text("Connected")
    page.locator("#page-size-preference").select_option("25")
    nav(page, "students", "Students")
    page.locator("#student-search").fill("")
    expect(page.locator(".student-table tbody tr")).to_have_count(18)
    assert page.evaluate('localStorage.getItem("placesure-page-size")') == "25"
    ok("Settings show database health and persist the page-size preference")
    # Complete page matrix with a fresh document for each viewport/route.
    routes = [
        ("overview", "Overview"),
        ("students", "Students"),
        ("companies", "Companies"),
        ("checker?student=1&company=1", "Eligibility Checker"),
        ("history", "Evaluation History"),
        ("reports", "Reports"),
        ("settings", "Settings & About"),
    ]
    for label, width, height in [
        ("desktop", 1440, 1000),
        ("tablet", 768, 1024),
        ("mobile", 390, 844),
    ]:
        for route, title in routes:
            test = browser.new_page(viewport={"width": width, "height": height})
            test.on("pageerror", lambda e: errors.append(str(e)))
            test.goto(BASE + "/#/" + route, wait_until="networkidle")
            expect(test.locator("main h1")).to_have_text(title)
            assert test.evaluate("document.documentElement.scrollWidth<=innerWidth"), (
                label,
                route,
                "page overflows",
            )
            if label == "mobile" and route == "overview":
                test.locator('[data-action="toggle-nav"]').click()
                expect(test.locator(".sidebar")).to_have_class("sidebar open")
                test.locator('#navigation a[href="#/students"]').click()
                expect(test.locator("main h1")).to_have_text("Students")
                assert "open" not in test.locator(".sidebar").get_attribute("class")
            if label == "mobile" and route in [
                "checker?student=1&company=1",
                "reports",
            ]:
                test.screenshot(
                    path=str(ART / (route.split("?")[0] + "-mobile.png")),
                    full_page=True,
                )
            test.close()
        ok(f"All seven pages render without page overflow at {label} {width}×{height}")
    broken = browser.new_page(viewport={"width": 1280, "height": 900})
    broken.goto("http://127.0.0.1:8082", wait_until="networkidle")
    expect(broken.locator(".error-state h2")).to_have_text(
        "Your data is temporarily out of reach"
    )
    assert "DB_PASSWORD" not in broken.locator(".error-state").inner_text()
    broken.get_by_role("link", name="Connection help", exact=True).click()
    expect(broken.locator("main")).to_contain_text("Connection checklist")
    broken.close()
    ok(
        "An actually unreachable database produces a safe error screen and useful connection help"
    )
    assert not errors, errors
    ok("No uncaught JavaScript errors across all exercised browser workflows")
    browser.close()
print(
    f"\n{len(checks)} browser workflow checks passed. No failed checks. Includes 21 page/viewport combinations."
)
