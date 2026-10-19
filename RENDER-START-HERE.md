# Your selected Render deployment

**One FREE Docker web service, connected to your EXISTING MySQL database.**
The root `render.yaml` does not create a paid database or persistent disk.

## Next actions

1. Create a Git repository in your own account (for GitHub: https://github.com/new).
2. Extract `PlaceSure-Render-Upload.zip` into an empty folder. Upload its **contents**, including folders and dotfiles, to the repository root. Do not upload the ZIP as the application. `render.yaml`, `Dockerfile`, and `pom.xml` must be top-level files.
3. Send the repository URL to the assistant. It can then verify the source and generate the correct Deploy-to-Render link. You authorize the launch by signing into your own Render account.
4. Before launch, import `database/database.sql` **once into a dedicated empty database** on your MySQL server using an authorized account. Do not import it into unrelated existing tables.
5. In Render's Blueprint setup, use root `render.yaml` and enter `DB_URL`, `DB_USER`, and `DB_PASSWORD` privately. Do not send passwords or API keys in chat.

`DB_URL` must be a **JDBC** URL, with TLS configured according to your MySQL provider. Template only:

```text
jdbc:mysql://YOUR_DB_HOST:YOUR_DB_PORT/YOUR_DB_NAME?sslMode=VERIFY_IDENTITY
```

Use a non-root application user, and permit Render connections according to your database provider's networking rules.

Render assigns the real public URL after deploying. No Render service has been created yet, and no URL is fabricated here. Free-tier availability limits apply; your existing database is managed separately.

This remains a fictional-data demo without application login. Do not use real student data until appropriate authentication and privacy controls are added.

## About this source-upload bundle

It intentionally omits prebuilt JARs, generated runtime files, and test-evidence exports. Render builds the app from source. All application code, SQL inputs, local UI assets, tests, Docker setup, and documentation are included. The complete project/evidence archive is `PlaceSure-Render.zip`.

See `docs/DEPLOY-RENDER.md` for full guidance and the optional paid alternative. The paid alternative is NOT the default you selected.
