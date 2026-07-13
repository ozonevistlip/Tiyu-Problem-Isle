# Tiyu Problem Isle

面向课堂教学的 C++ 在线评测系统，适合教师组织课程、题库和比赛，也方便学生在线编写、运行和提交代码。

## 功能概览

- 教师端：班级管理、学生管理、题目管理、测试点管理、提示管理、比赛管理、提交记录和排行榜。
- 学生端：参加比赛、查看题目、编写 C++17 代码、解锁提示、提交代码、查看评测结果和排行榜。
- 代码可视化：在教师端和学生端通过侧边栏进入代码可视化功能区，目前支持解析并展示一维 `int` 数组。
- 在线评测：后端通过 Redis 队列调度评测任务，并使用 Docker 中的 `gcc:13` 环境编译运行 C++17 代码。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3、JDK 17、MyBatis-Plus、MySQL、Redis |
| 前端 | Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios、Monaco Editor |
| 评测环境 | Docker、`gcc:13` |

## 项目结构

```text
backend/   Spring Boot API、评测队列和 Docker C++17 评测 worker
frontend/  Vue 3 + Vite 前端应用
deploy/    Ubuntu 生产部署配置和服务文件
scripts/   Windows 本地开发启动、停止脚本
```

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 16+
- npm
- MySQL 8
- Redis
- Docker（运行代码评测时需要）

## 快速启动

### Windows 一键启动（推荐）

仓库提供了 PowerShell 脚本，可以统一使用项目配置的 JDK、Maven 和 Node.js 路径，不依赖它们在系统 `PATH` 中的优先级。

首次使用时，复制并编辑本地工具配置：

```powershell
Copy-Item .\scripts\local.env.example.ps1 .\scripts\local.env.ps1
notepad .\scripts\local.env.ps1
```

启动 Redis、后端和前端：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

停止本次启动的服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

默认访问地址：

- 前端：http://127.0.0.1:5173/
- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/api/health

### 手动启动后端

先创建数据库并导入表结构：

```bash
cd backend
mysql -uroot -p < sql/schema.sql
```

确认 MySQL 和 Redis 已启动，并根据本机环境检查 `backend/src/main/resources/application.yml` 中的数据库、Redis 和 JWT 配置，然后运行：

```bash
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

如果需要执行代码评测，还要确认 Docker 可以运行评测镜像：

```bash
docker run --rm gcc:13 g++ --version
```

### 手动启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://127.0.0.1:5173/`。开发环境的后端 API 地址配置在 `frontend/.env.development`：

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## 代码可视化

登录后，可以在教师端或学生端左侧导航栏进入“代码可视化”功能区。当前版本支持识别代码中的第一个一维 `int` 数组，例如：

```cpp
int arr[10] = {5, 9, 5, 3, 3, -1};
```

可视化页面会展示数组名称、元素值和下标；未显式初始化的位置会按 C++ 规则显示为 `0`。学生在题目编辑器中修改的代码也会同步到代码可视化页面。

## 开发与构建

构建前端：

```bash
cd frontend
npm run build
```

后端运行前会自动编译 Java 源码；也可以在 `backend` 目录执行：

```bash
mvn test
```

## 开发注意事项

- 教师创建的题目至少需要配置一个测试点，才能用于比赛。
- 没有测试点的提交会被评测器拒绝，不会被标记为通过。
- 评测任务依赖 Redis 队列，实际编译运行依赖 Docker。
- 本地数据库数据、构建产物、运行日志和 IDE 文件已加入 Git 忽略规则。

## 生产部署

生产环境配置与本地开发配置分离：

- 后端生产配置：`backend/src/main/resources/application-prod.yml`
- 前端生产 API 地址：`frontend/.env.production`
- 服务器环境模板和服务文件：`deploy/`

目标平台为 Ubuntu 22.04 LTS 64 位。部署时请参考 [`deploy/README.md`](deploy/README.md) 中的服务器上传、MySQL、构建、systemd 和 Nginx 配置说明。
