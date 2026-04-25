# iam-clj-api

An Identity Access Manager API built with Leiningen.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Setup
1. Run `git clone https://github.com/codewriter3000/identity-access-manager-clj.git`.
2. Run `lein install`.
3. Create a PostgreSQL database. You'll insert connection properties in step 5.
4. Copy `.env.example` to `.env` in the project root.
5. Fill in your values in `.env`. It should look something like this:
```
DATABASE_TYPE=postgresql
DATABASE_NAME=iam_clj_db
DATABASE_USER=postgres
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_PASS=changeme

FRONTEND_BASE_URL=http://localhost:3000

SMTP_HOST=localhost
SMTP_PORT=587
SMTP_USER=smtp-user
SMTP_PASS=smtp-password
SMTP_FROM=no-reply@localhost
SMTP_STARTTLS=true
SMTP_AUTH=true

ENV=dev
```
> **Note:** Do not commit `.env` to source control. OS environment variables take precedence over `.env` values, so production deployments can override any setting via the process environment.
6. Run `lein test` and make sure all of the tests pass. If anything fails, open an issue.

## Authentication Notes

- Login endpoint: `POST /api/user/login` with `{"login_id":"username-or-email","password":"..."}`.
- Public endpoints (no session required):
  - `POST /api/user/login`
  - `POST /api/user/password/forgot`
  - `POST /api/user/password/reset`
  - Swagger docs (`/swagger-ui`, `/swagger.json`)
- All other `/api/*` endpoints require a valid session cookie.
- Forgot-password tokens are one-time and expire after 15 minutes.

## Running

To start a web server for the application, run:

    lein run

## Database setup

Use the DB migration namespace for schema and seed setup:

  lein run -m iam-clj-api.db.migrate init

Other useful commands:

  lein run -m iam-clj-api.db.migrate create-tables
  lein run -m iam-clj-api.db.migrate drop-tables
  lein run -m iam-clj-api.db.migrate add-core-perms-and-users
  lein run -m iam-clj-api.db.migrate add-oauth-defaults

## License

Copyright © 2025 FIXME
