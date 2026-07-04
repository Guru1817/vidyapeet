# Vidyapeet — Deployment (Render backend + Supabase DB/Storage + Vercel frontend)

## Overview

- **Backend:** Spring Boot on Render (Docker, free plan)
- **Database:** Supabase PostgreSQL
- **File storage:** Supabase Storage (private `vidyapeet-files` bucket)
- **Frontend:** React/Vite on Vercel

The backend runs under the `prod` Spring profile, which uses PostgreSQL, runs
Flyway migrations V1–V4 on first boot, and activates `SupabaseStorageService`.

---

## 1. Supabase — get the database connection

Supabase dashboard → Project Settings → Database → Connection string.

Use the **Session pooler** connection (works on Render's IPv4 network). You need
three values for Render env vars, in **JDBC** form:

- `DATABASE_URL` = `jdbc:postgresql://<pooler-host>:5432/postgres`
  - Example host: `aws-0-<region>.pooler.supabase.com`
  - Note: use the JDBC scheme (`jdbc:postgresql://`), not the raw `postgresql://` string Supabase shows.
- `DATABASE_USERNAME` = `postgres.<project-ref>` (the pooler username shown in the dashboard)
- `DATABASE_PASSWORD` = your database password

> If Flyway has trouble on the Transaction pooler, use the **Session** pooler
> (port 5432). Flyway needs a session-capable connection for migration locks.

---

## 2. Supabase — storage bucket

Already done if you followed the storage step:
- Bucket `vidyapeet-files` exists and is **Private**.
- You have the **`service_role`** key (Settings → API). Rotate it if it was ever shared.

---

## 3. Generate a JWT secret

The prod app requires `VIDYAPEET_JWT_SECRET` — a Base64-encoded 256-bit key.
Generate one (any of these):

```bash
# OpenSSL
openssl rand -base64 32
```
```powershell
# PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
```

---

## 4. Render — deploy via Blueprint

1. Push this repo to GitHub (see git note below).
2. Render Dashboard → **New** → **Blueprint** → connect the repo.
3. Render reads `render.yaml` and creates the `vidyapeet-backend` web service
   (Docker, free plan, health check at `/actuator/health`).
4. When prompted, fill in the `sync: false` environment variables:

   | Env var | Value |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://<pooler-host>:5432/postgres` |
   | `DATABASE_USERNAME` | `postgres.<project-ref>` |
   | `DATABASE_PASSWORD` | your Supabase DB password |
   | `VIDYAPEET_JWT_SECRET` | the Base64 secret from step 3 |
   | `VIDYAPEET_CORS_ORIGINS` | your Vercel URL, e.g. `https://vidyapeet.vercel.app` |
   | `SUPABASE_URL` | `https://<project-ref>.supabase.co` |
   | `SUPABASE_SERVICE_KEY` | your **service_role** key |

   (`SPRING_PROFILES_ACTIVE=prod`, `SUPABASE_BUCKET=vidyapeet-files`,
   `VIDYAPEET_SEED_ENABLED=false`, and `JAVA_OPTS` are preset in `render.yaml`.)

5. Deploy. First boot runs Flyway V1–V4 against Supabase Postgres.
6. Verify: open `https://<your-service>.onrender.com/actuator/health` → `{"status":"UP"}`.

> **Cold starts:** Render's free plan spins the service down after 15 minutes idle
> and takes ~1 minute to wake. The first request after idle will be slow. Optional:
> add a cron/uptime ping during active hours to keep it warm.

---

## 5. Vercel — frontend

1. Vercel → New Project → import the repo → set **Root Directory** to `frontend`.
2. Build command `npm run build`, output directory `dist` (Vite defaults).
3. Environment variable:
   - `VITE_API_BASE_URL` = your Render URL, e.g. `https://vidyapeet-backend.onrender.com`
4. Deploy, then copy the Vercel URL back into Render's `VIDYAPEET_CORS_ORIGINS`
   and redeploy the backend so CORS allows the frontend.

---

## 6. First admin / data

`VIDYAPEET_SEED_ENABLED=false` in prod means no demo data is created. Create your
first SUPER_ADMIN and institutes through the app / API against the live database.

---

## Git note

The backend `.gitignore` and `.dockerignore` exclude `target/`, `uploads/`, and IDE
files. Never commit real secrets — all secrets are entered in the Render/Vercel
dashboards (they use `sync: false` in `render.yaml`).

---

## Env var reference (backend, prod profile)

| Variable | Required | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes (`prod`) | Activates PostgreSQL + Flyway + Supabase storage |
| `DATABASE_URL` | yes | JDBC URL to Supabase Postgres |
| `DATABASE_USERNAME` | yes | DB user |
| `DATABASE_PASSWORD` | yes | DB password |
| `VIDYAPEET_JWT_SECRET` | yes | Base64 256-bit JWT signing key |
| `VIDYAPEET_CORS_ORIGINS` | yes | Comma-separated allowed frontend origins |
| `SUPABASE_URL` | yes | Supabase project URL |
| `SUPABASE_SERVICE_KEY` | yes | `service_role` key (server-side only) |
| `SUPABASE_BUCKET` | no (default `vidyapeet-files`) | Storage bucket name |
| `VIDYAPEET_SEED_ENABLED` | no (default `false`) | Demo data seeding |
| `PORT` | auto (Render sets it) | HTTP port |
| `JAVA_OPTS` | no | JVM flags (memory tuning) |
