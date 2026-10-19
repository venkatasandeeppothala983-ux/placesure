#!/usr/bin/env python3
"""Non-destructive fresh local setup for Unix MySQL/MariaDB root socket authentication.
For Windows, managed databases, or password-based admin access, follow README's manual SQL.
"""

import argparse, os, pathlib, re, secrets, shutil, subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
os.chdir(ROOT)
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--database", default="placesure")
p.add_argument("--app-user", default="placesure_app")
p.add_argument("--env-file", default=".env")
p.add_argument("--empty", action="store_true")
p.add_argument("--with-tests", action="store_true")
a = p.parse_args()
for value in [a.database, a.app_user]:
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,47}", value):
        raise SystemExit(
            "Database/user names must contain only letters, digits, and underscores (max 48 characters)."
        )
config = pathlib.Path(a.env_file)
if config.exists():
    raise SystemExit(
        f"{config} already exists. Nothing changed. Use your existing configuration or follow the manual setup guide."
    )
client = shutil.which("mariadb") or shutil.which("mysql")
if not client:
    raise SystemExit(
        "Install MySQL 8+ or MariaDB 10.6+ and start the database server first."
    )
admin = ([] if os.geteuid() == 0 else ["sudo"]) + [
    client,
    "--batch",
    "--skip-column-names",
]


def sql(text, database=None):
    result = subprocess.run(
        admin + ([database] if database else []),
        input=text,
        text=True,
        capture_output=True,
    )
    if result.returncode:
        raise SystemExit(
            "Database setup failed. Check that the server is running and local root socket access works. No credentials or SQL statements have been printed. Check the setup instructions and any partially created fresh objects before retrying."
        )
    return result.stdout.strip()


test = a.database + "_test"
for database in ([a.database, test] if a.with_tests else [a.database]):
    if (
        sql(
            f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{database}'"
        )
        != "0"
    ):
        raise SystemExit(
            f"Database {database} already exists. Refusing to overwrite existing data. Follow README for manual configuration."
        )
if sql(f"SELECT COUNT(*) FROM mysql.user WHERE User='{a.app_user}'") != "0":
    raise SystemExit(
        "That application user already exists. Use --app-user with a new name or configure the existing account manually."
    )
password = secrets.token_hex(24)
statements = f"CREATE DATABASE `{a.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
if a.with_tests:
    statements += (
        f"CREATE DATABASE `{test}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
    )
for host in ["127.0.0.1", "localhost"]:
    statements += f"CREATE USER '{a.app_user}'@'{host}' IDENTIFIED BY '{password}';\nGRANT SELECT, INSERT, UPDATE, DELETE ON `{a.database}`.* TO '{a.app_user}'@'{host}';\n"
    if a.with_tests:
        statements += (
            f"GRANT ALL PRIVILEGES ON `{test}`.* TO '{a.app_user}'@'{host}';\n"
        )
sql(statements)
sql(
    (ROOT / "database" / ("schema.sql" if a.empty else "database.sql")).read_text(),
    a.database,
)
url = (
    lambda db: "jdbc:mysql://127.0.0.1:3306/"
    + db
    + "?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8"
)
content = f"# Generated local credentials. Keep this file private and out of version control.\nDB_URL={url(a.database)}\nDB_USER={a.app_user}\nDB_PASSWORD={password}\nPORT=8080\nBIND_HOST=0.0.0.0\n"
if a.with_tests:
    content += f"TEST_DB_URL={url(test)}\nTEST_DB_USER={a.app_user}\nTEST_DB_PASSWORD={password}\n"
config.parent.mkdir(parents=True, exist_ok=True)
fd = os.open(config, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, "w") as f:
    f.write(content)
print(f"Created {a.database}; saved private configuration to {config}.")
print(
    "Imported "
    + (
        "an empty schema."
        if a.empty
        else "18 fictional students and 5 fictional placement partners."
    )
)
print("Next: ./scripts/run.sh --seed-demo")
