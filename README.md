# Tiyu Problem Isle

Tiyu Problem Isle is a teaching-oriented C++ online judge for classroom contests.
It provides teacher workflows for classes, problems, testcases, hints and contests,
plus student workflows for contest solving, timed hint unlocks, submissions and rankings.

## Stack

- Backend: Spring Boot 3, JDK 17, MyBatis-Plus, MySQL, Redis
- Frontend: Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, Axios, Monaco Editor
- Judge runtime: Docker with `gcc:13`

## Project Structure

```text
backend/   Spring Boot API and Docker-based C++17 judge worker
frontend/  Vue/Vite web application
```

## Start Backend

```bash
cd backend
mysql -uroot -p < sql/schema.sql
mvn spring-boot:run
```

The backend listens on `http://localhost:8080`.

Before judging submissions, make sure Redis is running and Docker can run the judge image:

```bash
docker run --rm gcc:13 g++ --version
```

## Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend listens on `http://127.0.0.1:5173`.

The development API URL is configured in `frontend/.env.development`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## Windows One-Command Dev Startup

This repository includes PowerShell startup scripts so the project does not depend
on whatever JDK, Maven or Node happens to be first in the global `PATH`.

1. Configure local tool paths once:

```powershell
Copy-Item .\scripts\local.env.example.ps1 .\scripts\local.env.ps1
notepad .\scripts\local.env.ps1
```

2. Start Redis, backend and frontend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

3. Stop the started backend/frontend processes:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

## Notes

- Teacher-created problems must have at least one testcase before they can be used in contests.
- Submissions with no configured testcases are rejected by the judge instead of being marked accepted.
- Local database data, build outputs, logs and IDE files are intentionally ignored by Git.

## Production Deployment

Production server configuration has been split from local development:

- Backend production profile: `backend/src/main/resources/application-prod.yml`
- Frontend production API URL: `frontend/.env.production`
- Server env file template and service files: `deploy/`

Target production platform:

```text
Ubuntu 22.04 LTS 64-bit
```

The configured public entrypoint is:

```text
http://120.26.185.124/
```

See `deploy/README.md` for the server upload, MySQL, build, systemd and Nginx steps.
