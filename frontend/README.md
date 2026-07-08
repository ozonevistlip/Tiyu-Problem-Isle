# CppKid OJ Frontend

Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios + Monaco Editor frontend for the CppKid teaching OJ backend.

## Start

```bash
npm install
npm run dev
```

The frontend runs at `http://127.0.0.1:5173`.

Backend API base URL is configured in `.env.development`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## Main Flows

- Teacher: login, class management, student membership, problem/testcase/hint management, contest creation, contest problem settings, publish, rank and submissions.
- Student: login, contest list, contest detail, problem solving, Monaco C++17 editor, hint countdown/unlock, submission polling, rank and submission cases.
