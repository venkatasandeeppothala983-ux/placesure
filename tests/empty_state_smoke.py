#!/usr/bin/env python3
"""Run after mvn -Pintegration verify has cleaned placesure_test, before re-seeding it."""

import json, urllib.request
from playwright.sync_api import sync_playwright, expect

BASE = "http://127.0.0.1:8081"
with urllib.request.urlopen(BASE + "/api/dashboard") as r:
    d = json.load(r)["data"]
    assert (
        d["totalStudents"] == d["totalCompanies"] == d["totalChecks"] == 0
    ), "Use an EMPTY placesure_test database only."
checks = []
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    for route, title, copy in [
        ("overview", "Overview", "Your first decision starts here"),
        ("students", "Students", "Your next success story starts here"),
        ("companies", "Companies", "No opportunities found"),
        ("checker", "Eligibility Checker", "No matches found"),
        ("history", "Evaluation History", "No evaluations to show"),
        ("reports", "Reports", "No results in this view"),
        ("settings", "Settings & About", "All systems connected"),
    ]:
        page = browser.new_page(viewport={"width": 1280, "height": 900})
        page.goto(BASE + "/#/" + route, wait_until="networkidle")
        expect(page.locator("main h1")).to_have_text(title)
        expect(page.locator("main")).to_contain_text(copy)
        if route == "overview":
            assert page.locator(".stat-number").all_text_contents() == [
                "0",
                "0",
                "0",
                "0",
            ]
        if route == "checker":
            expect(page.locator("#evaluate-button")).to_be_disabled()
        assert page.evaluate("document.documentElement.scrollWidth<=innerWidth")
        page.close()
        checks.append(route)
        print("PASS real empty-database " + route)
    browser.close()
print(
    f"\n{len(checks)} empty-workspace checks passed against real JDBC data. No failed checks."
)
