# CppKid OJ Backend

Spring Boot 3 + JDK 17 + MySQL + MyBatis-Plus + Redis teaching OJ backend.

## Features

- JWT login with teacher/student roles.
- Teacher class, student, problem, testcase, hint and contest management.
- Student contest list, contest problem detail, auto hint unlock, submission and rank APIs.
- Redis judge queue: `StringRedisTemplate.opsForList().leftPush("judge_queue", submissionId)`.
- Single scheduled `JudgeWorker` that atomically claims `PENDING` submissions and runs C++17 in Docker.
- Super admin class types, shared lesson materials, teacher-owned student accounts, and tier-restricted after-class videos.

## Start

1. Create MySQL schema:

```bash
mysql -uroot -p < sql/schema.sql
```

For an existing database, back it up and run `sql/teaching_migration.sql` and then `sql/lesson_student_tier_migration.sql` once each. If the first migration was already applied, run only the second. The first closes self-registration while preserving the admin setting; the second moves class-wide tiers to lesson-specific choices. Configure `VIDEO_DIR` as a persistent, backed-up directory for uploaded videos.

2. Start Redis and Docker.

3. Edit `src/main/resources/application.yml` for your MySQL/Redis/JWT settings.

4. Run:

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## Example Requests

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"t1","password":"123456"}'

curl -X POST http://localhost:8080/api/teacher/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-token>" \
  -d '{"className":"C++ Beginner A","classTypeId":1,"description":"First class"}'

curl -X GET http://localhost:8080/api/student/contests/1/problems/1/hint \
  -H "Authorization: Bearer <student-token>"

curl -X POST http://localhost:8080/api/student/contests/1/problems/1/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <student-token>" \
  -d '{"language":"cpp17","code":"#include <bits/stdc++.h>\nusing namespace std;\nint main(){cout<<5;return 0;}"}'
```
