#!/usr/bin/env python3
"""Unit tests for the container startup wrapper; no cloud account or real secret needed."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
ENTRYPOINT = ROOT / "deploy" / "start.sh"


class ContainerStartupTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.folder = Path(self.tmp.name)
        self.jar = self.folder / "app.jar"
        self.jar.touch()
        self.capture = self.folder / "capture.json"
        java = self.folder / "java"
        java.write_text("""#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
Path(os.environ['MOCK_CAPTURE']).write_text(json.dumps({'arguments':sys.argv[1:],'url':os.environ.get('DB_URL'),'port':os.environ.get('PORT'),'bind':os.environ.get('BIND_HOST'),'passwordPresent':bool(os.environ.get('DB_PASSWORD'))}))
""")
        java.chmod(0o755)
        self.env = os.environ.copy()
        for name in [
            "DB_HOST",
            "DB_PORT",
            "DB_NAME",
            "DB_USER",
            "DB_PASSWORD",
            "DB_SSL_MODE",
            "DB_URL",
            "DB_WAIT_SECONDS",
            "SEED_DEMO",
            "PORT",
            "BIND_HOST",
        ]:
            self.env.pop(name, None)
        self.env.update(
            PATH=str(self.folder) + os.pathsep + os.environ["PATH"],
            APP_JAR=str(self.jar),
            DB_HOST="mysql.internal",
            DB_PASSWORD="UNIT_TEST_SECRET",
            DB_WAIT_SECONDS="0",
            MOCK_CAPTURE=str(self.capture),
        )

    def run_start(self, **updates):
        env = self.env.copy()
        for key, value in updates.items():
            if value is None:
                env.pop(key, None)
            else:
                env[key] = value
        result = subprocess.run(
            ["bash", str(ENTRYPOINT)],
            env=env,
            text=True,
            capture_output=True,
            timeout=5,
        )
        self.assertNotIn("UNIT_TEST_SECRET", result.stdout + result.stderr)
        return result

    def reject(self, **updates):
        result = self.run_start(**updates)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(self.capture.exists())
        return result

    def test_builds_mysql_url_and_honors_render_port(self):
        result = self.run_start(PORT="10000")
        self.assertEqual(0, result.returncode, result.stderr)
        data = json.loads(self.capture.read_text())
        self.assertTrue(
            data["url"].startswith("jdbc:mysql://mysql.internal:3306/placesure?")
        )
        self.assertIn("sslMode=DISABLED", data["url"])
        self.assertEqual("10000", data["port"])
        self.assertEqual("0.0.0.0", data["bind"])
        self.assertTrue(data["passwordPresent"])

    def test_preserves_explicit_external_jdbc_url(self):
        url = "jdbc:mysql://managed.example.test:3306/placesure?sslMode=VERIFY_IDENTITY"
        self.assertEqual(0, self.run_start(DB_URL=url, DB_HOST=None).returncode)
        self.assertEqual(url, json.loads(self.capture.read_text())["url"])

    def test_explicit_tls_mode_is_supported(self):
        self.assertEqual(0, self.run_start(DB_SSL_MODE="VERIFY_IDENTITY").returncode)
        self.assertIn(
            "sslMode=VERIFY_IDENTITY", json.loads(self.capture.read_text())["url"]
        )

    def test_demo_seed_flag_is_explicit(self):
        self.assertEqual(0, self.run_start(SEED_DEMO="true").returncode)
        self.assertEqual(
            ["-jar", str(self.jar), "--seed-demo"],
            json.loads(self.capture.read_text())["arguments"],
        )

    def test_no_seed_flag_by_default(self):
        self.assertEqual(0, self.run_start().returncode)
        self.assertEqual(
            ["-jar", str(self.jar)], json.loads(self.capture.read_text())["arguments"]
        )

    def test_password_required(self):
        self.reject(DB_PASSWORD=None)

    def test_invalid_port_rejected(self):
        self.reject(PORT="65536")

    def test_fractional_port_rejected(self):
        self.reject(DB_PORT="3306.5")

    def test_missing_host_and_url_rejected(self):
        self.reject(DB_HOST=None)

    def test_host_cannot_inject_query_parameters(self):
        self.reject(DB_HOST="mysql.test/other?user=root")

    def test_invalid_database_name_rejected(self):
        self.reject(DB_NAME="placesure;DROP")

    def test_invalid_tls_mode_rejected(self):
        self.reject(DB_SSL_MODE="INSECURE_UNKNOWN")

    def test_wait_time_is_bounded(self):
        self.reject(DB_WAIT_SECONDS="301")

    def test_non_mysql_url_rejected(self):
        self.reject(DB_URL="jdbc:postgresql://managed.test/db")

    def test_seed_flag_must_be_boolean(self):
        self.reject(SEED_DEMO="yes")

    def test_unreachable_mysql_startup_wait_exits(self):
        result = self.reject(DB_HOST="127.0.0.1", DB_PORT="9", DB_WAIT_SECONDS="1")
        self.assertIn("did not become reachable", result.stderr)

    def test_missing_jar_rejected(self):
        self.reject(APP_JAR=str(self.folder / "missing.jar"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
