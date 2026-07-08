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

## Notes

- Teacher-created problems must have at least one testcase before they can be used in contests.
- Submissions with no configured testcases are rejected by the judge instead of being marked accepted.
- Local database data, build outputs, logs and IDE files are intentionally ignored by Git.
