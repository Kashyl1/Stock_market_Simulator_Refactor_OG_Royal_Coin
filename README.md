# Trading Simulator

A virtual trading platform: sign up, get a play-money wallet, trade crypto, stocks and ETFs,
and follow how your portfolios perform. No real money, no real brokerage.

## Stack

- **Backend** (`backend/`): Java 21, Spring Boot 4.1, Gradle multi-module (`:app`,
  `:process-engine`), PostgreSQL, Flyway.
- **Frontend** (`frontend/`): Angular 22 (standalone, zoneless, signals), Vitest.

## Getting started

Prerequisites: JDK 21, Node.js LTS + npm, PostgreSQL on `localhost:5432`, OpenSSL.

### 1. Database

```bash
psql -U postgres -f backend/db/init.sql
```

Flyway creates the schema on the first backend start.

### 2. JWT signing keys

Never committed (`backend/.gitignore` ignores `keys/`).

```bash
mkdir -p backend/app/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out backend/app/keys/jwt-private.pem
openssl pkey -in backend/app/keys/jwt-private.pem -pubout -out backend/app/keys/jwt-public.pem
```

The backend reads `file:keys/jwt-*.pem` relative to its working directory, which is
`backend/app` under `bootRun`. When running from an IDE, use that working directory. Anywhere
else, set `APP_AUTH_RSA_PRIVATE_KEY` / `APP_AUTH_RSA_PUBLIC_KEY` to a `file:` location or to
the PEM text itself.

### 3. Run

```bash
cd backend && ./gradlew :app:bootRun
```

```bash
cd frontend && npm ci && npm start
```

The frontend runs on http://localhost:4200 and proxies `/api` to the backend on port 8080.

## Build and test

```bash
cd backend && ./gradlew build
```

```bash
cd frontend && npx ng build && npx ng test --watch=false
```

Backend tests need neither a database nor the signing keys.
