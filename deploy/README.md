# Production Deployment

Target server:

```text
OS: Ubuntu 22.04 LTS 64-bit
Public IP: 120.26.185.124
Install path: /opt/cppkid
Backend profile: prod
Frontend API: http://120.26.185.124/api
```

## Upload Project

From the local machine:

```powershell
scp -r E:\PROJECT_SET\cppkid root@120.26.185.124:/opt/cppkid
```

If `/opt/cppkid` already exists, upload the changed files or pull the latest Git revision on the server instead.

## Create MySQL User

On the server:

```bash
mysql
```

```sql
CREATE DATABASE IF NOT EXISTS cppkid_oj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'cppkid'@'localhost' IDENTIFIED BY 'REPLACE_WITH_SECRET';
ALTER USER 'cppkid'@'localhost' IDENTIFIED BY 'REPLACE_WITH_SECRET';
GRANT ALL PRIVILEGES ON cppkid_oj.* TO 'cppkid'@'localhost';
FLUSH PRIVILEGES;
```

Use the real password from the local ignored `deploy/cppkid.env` file.

Import the current database dump if needed:

```bash
mysql < /opt/cppkid/cppkid_oj.sql
```

## Backend

The production Spring Boot profile is configured in:

```text
backend/src/main/resources/application-prod.yml
```

Runtime secrets and server paths are read from:

```text
/opt/cppkid/deploy/cppkid.env
```

Before deploying the pet feature to an existing database, run `backend/sql/pet_migration.sql` once. Set `PET_DIR` to a persistent, backed-up directory and `DEEPSEEK_API_KEY` in the environment file. Create the pet asset directory before starting the service.

Build the backend:

```bash
cd /opt/cppkid/backend
mvn clean package -DskipTests
```

Install and start the service:

```bash
mkdir -p /opt/cppkid/judge-work
mkdir -p /opt/cppkid/pet-assets
cp /opt/cppkid/deploy/cppkid.service /etc/systemd/system/cppkid.service
systemctl daemon-reload
systemctl enable --now cppkid
systemctl status cppkid
```

Check locally on the server:

```bash
curl http://127.0.0.1:8080/api/health
```

## Frontend

The production build uses:

```text
frontend/.env.production
```

Build the frontend:

```bash
cd /opt/cppkid/frontend
npm install
npm run build
```

## Nginx

Install the provided config:

```bash
cp /opt/cppkid/deploy/nginx-cppkid.conf /etc/nginx/sites-available/cppkid
ln -sfn /etc/nginx/sites-available/cppkid /etc/nginx/sites-enabled/cppkid
nginx -t
systemctl reload nginx
```

Visit:

```text
http://120.26.185.124/
```

Recommended public ports: `22`, `80`, `443`.
Keep `3306`, `6379`, and `8080` closed to the public Internet.
