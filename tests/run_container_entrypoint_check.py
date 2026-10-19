#!/usr/bin/env python3
"""Test the Render startup wrapper against the existing local DB; no Docker claims."""

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
config = {}
for line in (ROOT / ".env").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        key, value = line.split("=", 1)
        config[key.strip()] = value.strip().strip("\"'")
env = {
    **os.environ,
    "APP_JAR": str(ROOT / "releases/placesure.jar"),
    "DB_URL": "",
    "DB_HOST": "127.0.0.1",
    "DB_PORT": "3306",
    "DB_NAME": "placesure",
    "DB_USER": config["DB_USER"],
    "DB_PASSWORD": config["DB_PASSWORD"],
    "DB_WAIT_SECONDS": "5",
    "DB_SSL_MODE": "DISABLED",
    "SEED_DEMO": "false",
    "PORT": "8085",
    "BIND_HOST": "127.0.0.1",
}
os.execvpe("bash", ["bash", str(ROOT / "deploy/start.sh")], env)
