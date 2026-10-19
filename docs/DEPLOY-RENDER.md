# Deploy PlaceSure on Render

## Current status

**Prepared and locally validated — not yet deployed to a Render account.** No Render credentials, source repository URL, or approval for billable resources have been supplied. No Render services or charges have been created by the assistant.

The temporary Cloudflare preview is separate from this permanent hosting setup. A temporary tunnel is not a Render deployment.

## Files provided

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage Java 17 build; non-root JRE runtime; locally bundled UI and dependencies |
| `.dockerignore` | Excludes local credentials, caches, reports, private keys, and generated runtime files |
| `render.yaml` | **Selected default:** free Java web service using your already-provisioned external MySQL |
| `deploy/render-with-private-mysql.yaml` | Optional **paid** Java + private MySQL 8.4 + persistent disk; not selected |
| `deploy/render-external-mysql.yaml` | Explicit alias of the selected external-MySQL configuration |
| `deploy/start.sh` | Validates environment, constructs a JDBC URL, waits for private MySQL TCP readiness, starts Java on Render's PORT |
| `deploy/mysql/Dockerfile` | MySQL 8.4 with schema and fictional sample inputs initialized on a fresh disk |
| `deploy/mysql/20-app-permissions.sql` | Restricts the application account to SELECT/INSERT/UPDATE/DELETE after schema initialization |
| `deploy/render.env.example` | Optional API authorization template; copy privately to ignored `.env.deploy` only if API deployment is desired |

## Important: costs and access

Render runs this Java application using Docker. For MySQL on Render, the supported setup is a **private service with a persistent disk**. Private-service compute and persistent disks are paid resources; this is not an all-free MySQL deployment.

The optional **paid** `deploy/render-with-private-mysql.yaml` blueprint (not selected) requests:
- Java: `0.5c-512mb` compute.
- MySQL: `1c-2g` compute, with a 10 GB persistent disk at `/var/lib/mysql`.
- Both services in Singapore so they can communicate privately.

**Review the actual billing estimate in your Render account before approving deployment.** No pricing total is promised here. You can choose a different suitable plan after reviewing its memory requirements and costs.

You selected an existing reachable MySQL database, so the **root `render.yaml`** provisions only a free Render web service. The database is **not included** in that option. Its costs, availability, TLS requirements, network permissions, and backups are your responsibility. A free web instance is not an always-on compute guarantee; consult Render's current free-tier rules.

## Selected deployment: free web service + existing MySQL

1. Extract the project archive.
2. Put the **contents of `PlaceSure/` at the root of a Git repository**. `render.yaml`, `Dockerfile`, `pom.xml`, `src/`, and `frontend/` must be top-level entries. Do not upload only the ZIP or HTML folder.
3. Keep `.env`, `.env.deploy`, API keys, SQL backups, and runtime artifacts out of Git. Existing `.gitignore` and `.dockerignore` protect these files.
4. Push the source to a Git provider accessible to your Render account. A private repository can be used if you authorize Render to access it.
5. Sign in at https://dashboard.render.com/.
6. Choose **New → Blueprint** and select your repository and branch.
7. Use the root `render.yaml`: it creates **one free Docker web service**, not a paid MySQL service. Supply `DB_URL`, `DB_USER`, and `DB_PASSWORD` privately in Render.
8. Render builds the Java image. The existing external database must already be reachable and initialized. No actual password belongs in YAML or source code.
9. Wait for the web service to be Live. Its health-check path is `/api/health` and requires a working database connection.
10. Open the actual `onrender.com` URL assigned by Render. No URL is fabricated or reserved by these local files.

Once you have supplied your repository URL, a correctly scoped Deploy-to-Render link can be generated for that repository. Do not use a placeholder link as proof of deployment.

### Source repository commands (run with your own repository URL)

```bash
cd PlaceSure
git init
git add .
git status                  # confirm credentials and local runtime files are NOT staged
git commit -m "Prepare PlaceSure for Render"
git branch -M main
git remote add origin YOUR_ACTUAL_REPOSITORY_URL
git push -u origin main
```

Configure your own Git author and provider authorization normally. Do not put provider tokens in remote URLs or paste them into chat.

## Existing external MySQL option

1. Use a **dedicated empty MySQL 8-compatible database** on your existing server, and import `database/database.sql` **once** using an authorized admin account. Do not run it against unrelated existing tables or re-import duplicate seed data.
2. Allow connections from the Render web service according to your database provider's networking policy.
3. In Blueprint setup, use root `render.yaml` (the selected default). `deploy/render-external-mysql.yaml` is an equivalent explicit alias.
4. Supply these secrets in Render's dashboard:
   - `DB_URL`: the complete MySQL JDBC URL with the provider's required TLS settings (prefer `sslMode=VERIFY_IDENTITY` for remote databases).
   - `DB_USER`: a dedicated non-root database account.
   - `DB_PASSWORD`: its password.
5. Do not simultaneously deploy the default paid database unless you actually intend to use it.

Never substitute Render Postgres without changing the application and schema: this project deliberately uses MySQL/JDBC.

## Startup and data behavior

- Render injects `PORT`; the application honors it and binds to `0.0.0.0`.
- The wrapper accepts a complete `DB_URL`, or builds one from `DB_HOST`, `DB_PORT`, `DB_NAME`, and `DB_SSL_MODE`.
- `DB_SSL_MODE=DISABLED` is used only for the supplied private-network database. Use appropriate verified TLS settings for an internet-facing database.
- When `DB_HOST` is supplied, the wrapper waits up to `DB_WAIT_SECONDS` for its TCP listener before Java starts. MySQL's initial temporary server is socket-only, avoiding a race with fresh-disk schema import.
- `SEED_DEMO=true` explicitly enables the Java demo seeder. It computes results only if evaluation history is empty. With the bundled fictional inputs this gives 40 actual evaluations; it does not insert hardcoded outcomes.
- A new Render database starts with the bundled fictional sample inputs, not an automatic copy of changes made in the temporary preview. Request a separate reviewed data migration if needed.
- The MySQL image imports its initial SQL files **only on a fresh data directory**. Redeploying does not erase or reinsert existing profiles/evaluations.
- Keep `/var/lib/mysql` attached to the persistent disk. App-instance filesystem changes are not a database persistence strategy.
- The database/user names are fixed to `placesure` / `placesure_app` in the blueprint and permissions SQL. If renaming them, update both places together.
- App images contain no local `.env`; all deployment secrets are supplied at runtime.
- MySQL root credentials are generated and are **not** passed to the Java service.
- Automatic redeploys are initially disabled. Use Render's manual deploy action after reviewing a new version.

## Verification after a real deployment

Use the URL actually assigned to your service:

```text
GET /api/health       → database Connected
GET /api/dashboard   → actual counts
GET /api/students    → stored profiles
GET /api/companies   → configured opportunities
```

Then verify all of these:
1. Open every page and check the browser console for errors.
2. Create/edit a fictional student and company.
3. Run eligible and ineligible checks; confirm all explanations and saved history.
4. Download CSV and use Print / Save PDF.
5. Restart/redeploy the Java web service; confirm records still exist.
6. Restart the MySQL service without deleting its disk; verify persistence again.
7. Record the real service IDs, URL, deployment logs, and test outcome before claiming completion.

## Authentication and real data

This is still a **public fictional-data demonstration**, with no login. Do not enter real student details into this public instance. Institutional production requires proper authentication, authorization, HTTPS, audit/retention policy, privacy controls, and operational backups. This deployment add-on does not claim those features.

## Backups and schema changes

A persistent disk prevents routine redeploy data loss; it is not a complete backup policy. Use logical MySQL backups and test restores. Do not rely on restoring a live database's filesystem snapshot for consistent recovery.

Example from an authorized database shell (credentials stay in environment variables):

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --no-tablespaces \
  --set-gtid-purged=OFF -u "$MYSQL_USER" "$MYSQL_DATABASE" > backup.sql
```

Keep backups private, off the public Git repository, and in a separate durable location. SQL initialization files do not run as migrations against an existing disk; apply reviewed schema changes explicitly.

## Optional assistant-driven API deployment

If you want API automation rather than approving deployment in the dashboard:
- Supply the actual repository URL and explicit approval for the selected paid resources.
- Create an appropriately scoped Render API key in your own account.
- Copy `deploy/render.env.example` to **`.env.deploy`**, fill it privately in the workspace, and tell the assistant when it is ready. **Do not paste the key into chat.**
- The file is ignored by Git and excluded from Docker images and project ZIPs.
- The assistant must list actual Render workspaces/resources through the API, use returned IDs, and confirm the target workspace before provisioning.

No API key is configured now, and neither a Render nor a Git-provider connector is available in this conversation. Preparing Docker/YAML files does **not** mean services have been created.

## Actual validation performed here

- All three blueprint files passed validation against Render's official JSON Schema.
- **17 startup-wrapper unit tests passed** (JDBC URL/TLS construction, supplied PORT, secret handling, seed flags, invalid inputs).
- The real Java application was launched through the new wrapper on a local verification port: JDBC connected and live database-backed endpoints responded correctly. `SEED_DEMO=false` was used, and verification requests were read-only; the public demo may independently receive user edits.
- The restricted-user SQL was tested with an isolated real compatible database: CRUD worked and DDL was denied.
- Original Java business logic was not changed; original executed test evidence remains in `docs/TEST-RESULTS.md`.
- **Not executed:** Docker image builds, actual MySQL 8.4 container initialization, a Render build, or a live Render URL. Docker and account authorization are not available here. These are pending, not claimed as passed.

Raw add-on evidence: `reports/render-startup-tests.txt` and `reports/render-deployment-checks.txt`.

## Official references (checked 18 October 2026)

- https://render.com/docs/deploy-mysql
- https://render.com/docs/docker
- https://render.com/docs/disks
- https://render.com/docs/blueprint-spec
- https://render.com/schema/render.yaml.json
- https://render.com/docs/api
