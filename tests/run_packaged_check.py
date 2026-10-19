#!/usr/bin/env python3
"""Serve the release from a directory with no frontend/source files to verify JAR resources."""

import os
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
config = {}
for line in (ROOT / ".env").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        key, value = line.split("=", 1)
        config[key.strip()] = value.strip().strip("\"'")
config.update(os.environ)
if (
    urlsplit(config.get("TEST_DB_URL", "").removeprefix("jdbc:")).path
    != "/placesure_test"
):
    raise SystemExit("A separate placesure_test database is required.")
env = {
    **os.environ,
    "DB_URL": config["TEST_DB_URL"],
    "DB_USER": config["TEST_DB_USER"],
    "DB_PASSWORD": config["TEST_DB_PASSWORD"],
    "BIND_HOST": "127.0.0.1",
    "PORT": "8083",
}
folder = ROOT / ".runtime" / "packaged-check"
folder.mkdir(parents=True, exist_ok=True)
os.chdir(folder)
os.execvpe("java", ["java", "-jar", str(ROOT / "releases" / "placesure.jar")], env)
