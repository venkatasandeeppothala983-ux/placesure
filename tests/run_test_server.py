#!/usr/bin/env python3
"""Start an isolated HTTP test instance. Never points at the application database."""

import argparse, os, pathlib, shutil, subprocess, urllib.parse

ROOT = pathlib.Path(__file__).resolve().parents[1]
os.chdir(ROOT)
parser = argparse.ArgumentParser()
parser.add_argument("--reset-demo", action="store_true")
parser.add_argument("--unavailable", action="store_true")
args = parser.parse_args()
config = {}
if pathlib.Path(".env").exists():
    for line in pathlib.Path(".env").read_text().splitlines():
        if "=" in line and not line.strip().startswith("#"):
            key, value = line.split("=", 1)
            config[key.strip()] = value.strip().strip("\"'")
config.update(os.environ)
url = config.get("TEST_DB_URL", "")
parsed = urllib.parse.urlsplit(url.removeprefix("jdbc:"))
if parsed.path != "/placesure_test":
    raise SystemExit("Refusing to use a database other than placesure_test.")
if args.reset_demo and not args.unavailable:
    cli = (
        "mariadb"
        if subprocess.run(["which", "mariadb"], capture_output=True).returncode == 0
        else "mysql"
    )
    sql = (
        "DELETE FROM evaluations; DELETE FROM students; DELETE FROM companies; DELETE FROM skills;\n"
        + pathlib.Path("database/database.sql").read_text()
    )
    subprocess.run(
        [
            cli,
            "-h",
            parsed.hostname or "127.0.0.1",
            "-P",
            str(parsed.port or 3306),
            "-u",
            config.get("TEST_DB_USER", "placesure_app"),
            "placesure_test",
        ],
        input=sql,
        text=True,
        check=True,
        env={**os.environ, "MYSQL_PWD": config.get("TEST_DB_PASSWORD", "")},
    )
env = {
    **os.environ,
    "DB_URL": url,
    "DB_USER": config.get("TEST_DB_USER", "placesure_app"),
    "DB_PASSWORD": config.get("TEST_DB_PASSWORD", ""),
    "BIND_HOST": "127.0.0.1",
    "PORT": "8081",
}
if args.unavailable:
    env.update(
        DB_URL="jdbc:mysql://127.0.0.1:33308/placesure_test?connectTimeout=1000",
        PORT="8082",
    )
runtime = pathlib.Path(".runtime")
runtime.mkdir(exist_ok=True)
jar = runtime / ("test-server-" + env["PORT"] + ".jar")
shutil.copyfile("target/placesure.jar", jar)
os.execvpe(
    "java",
    ["java", "-jar", str(jar)] + ([] if args.unavailable else ["--seed-demo"]),
    env,
)
