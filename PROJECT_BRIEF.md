# Tiyu Problem Isle 项目交接说明

## 项目基本信息

- 项目名：Tiyu Problem Isle
- 中文名：题屿
- GitHub：<https://github.com/ozonevistlip/Tiyu-Problem-Isle>
- 本地路径：`E:\PROJECT_SET\cppkid`
- 当前分支：`main`
- 当前用途：少儿 / 课堂 C++ 在线判题与比赛练习系统

## 技术栈

### 后端

- Spring Boot 3.3.5
- JDK 17
- MyBatis-Plus
- MySQL
- Redis
- Docker 判题环境，镜像：`gcc:13`

### 前端

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- Monaco Editor

## 项目结构

```text
E:\PROJECT_SET\cppkid
├─ backend   Spring Boot 后端与 Docker 判题 worker
├─ frontend  Vue/Vite 前端
├─ README.md
├─ PROJECT_BRIEF.md
└─ .gitignore
```

已通过 `.gitignore` 排除：

- `frontend/node_modules/`
- `frontend/dist/`
- `backend/target/`
- `backend/judge-work/`
- `mysql-data/`
- 运行日志
- IDE 配置
- 临时交接文件

## 当前 GitHub 状态

项目已经推送到：

```text
https://github.com/ozonevistlip/Tiyu-Problem-Isle
```

最近远端提交：

```text
6d50146 Merge remote initial README
```

本地仓库已设置远端：

```text
origin https://github.com/ozonevistlip/Tiyu-Problem-Isle.git
```

## 本地运行状态

之前已成功启动过：

- 后端：`http://localhost:8080`
- 后端健康检查：`http://localhost:8080/api/health`
- 前端：`http://127.0.0.1:5173/`
- MySQL：`3306`
- Redis：`6379`
- Docker Desktop：已恢复并升级到 `4.80.0`
- Docker Engine：正常
- 判题镜像：`gcc:13` 已拉取成功

Docker 验证命令：

```powershell
docker version
docker ps
docker run --rm gcc:13 g++ --version
```

已确认 `gcc:13` 中 `g++` 可用。

## 启动方式

### 1. 启动 MySQL

当前后端默认连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cppkid_oj?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123
```

初始化数据库：

```powershell
Get-Content -Raw E:\PROJECT_SET\cppkid\backend\sql\schema.sql |
  & D:\D\tools\mysql-8.0.32-winx64\bin\mysql.exe --host=127.0.0.1 --port=3306 --user=root --password
```

### 2. 启动 Redis

```powershell
powershell -ExecutionPolicy Bypass -File D:\D\tools\dockerData\redis\start-redis.ps1
```

验证：

```powershell
& D:\D\tools\dockerData\redis\bin\redis-cli.exe -h 127.0.0.1 -p 6379 PING
```

期望返回：

```text
PONG
```

### 3. 启动后端

推荐使用 JDK 17：

```powershell
cd E:\PROJECT_SET\cppkid\backend
$env:JAVA_HOME="C:\Users\Lenovo\.jdks\corretto-17.0.8.1"
$env:Path="$env:JAVA_HOME\bin;D:\D\tools\apache\apache-maven-3.8.6\bin;$env:Path"
mvn.cmd spring-boot:run
```

也可以先打包：

```powershell
cd E:\PROJECT_SET\cppkid\backend
$env:JAVA_HOME="C:\Users\Lenovo\.jdks\corretto-17.0.8.1"
$env:Path="$env:JAVA_HOME\bin;D:\D\tools\apache\apache-maven-3.8.6\bin;$env:Path"
mvn.cmd -DskipTests package
java -jar target\cppkid-oj-backend-1.0.0.jar
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

### 4. 启动前端

```powershell
cd E:\PROJECT_SET\cppkid\frontend
npm install
npm run dev
```

前端地址：

```text
http://127.0.0.1:5173/
```

前端 API 配置：

```text
frontend/.env.development
VITE_API_BASE_URL=http://localhost:8080/api
```

## 主要功能

### 老师端

- 注册 / 登录
- 班级管理
- 学生加入班级
- 题目管理
- 测试点管理
- 提示管理
- 比赛创建、编辑、发布
- 比赛题目配置
- 排名查看
- 提交记录查看

### 学生端

- 注册 / 登录
- 查看比赛
- 查看题目
- Monaco C++17 代码编辑
- 提交代码
- 查看判题状态
- 查看测试点结果
- 查看提示解锁倒计时
- 查看排名

## 判题链路

提交代码后：

1. 后端创建 `submission`，状态为 `PENDING`
2. 后端向 Redis 队列 `judge_queue` 写入 submissionId
3. `JudgeWorker` 从 Redis 队列取出 submissionId
4. `JudgeServiceImpl` 读取题目和测试点
5. `DockerJudgeRunner` 使用 Docker `gcc:13` 编译和运行 C++17
6. 保存 `submission_case`
7. 更新 `submission`
8. 更新题目统计、比赛答题状态和排名统计

关键文件：

```text
backend/src/main/java/com/example/oj/judge/JudgeWorker.java
backend/src/main/java/com/example/oj/service/impl/JudgeServiceImpl.java
backend/src/main/java/com/example/oj/judge/DockerJudgeRunner.java
backend/src/main/java/com/example/oj/judge/OutputComparator.java
```

## 最近修复

### Docker 判题环境

之前 Docker Desktop Engine 无法启动，导致提交被误判为编译错误或系统错误。

已处理：

- Docker Desktop 升级到 `4.80.0`
- Docker Engine 恢复正常
- `gcc:13` 镜像已拉取
- `docker run --rm gcc:13 g++ --version` 已验证成功

### 无测试点误判 AC

问题：

题目没有测试点时，`DockerJudgeRunner` 的默认最终状态是 `ACCEPTED`，循环不执行就会误判通过。

已修复：

- `DockerJudgeRunner` 中，如果测试点为空，直接返回 `SYSTEM_ERROR`
- 老师将题目加入比赛时，会检查题目是否至少有一个测试点
- 发布比赛时，会逐题检查测试点

关键文件：

```text
backend/src/main/java/com/example/oj/judge/DockerJudgeRunner.java
backend/src/main/java/com/example/oj/service/impl/TeacherServiceImpl.java
```

当前行为：

- 无测试点题目不能加入比赛
- 已在比赛里的题目如果没有测试点，发布比赛时会失败
- 即使有漏网情况，判题器也不会把无测试点提交判为 AC

## 已知注意事项

### 1. application.yml 里有本地数据库密码

当前配置适配本机：

```yaml
username: root
password: 123
```

如果换机器，需要修改：

```text
backend/src/main/resources/application.yml
```

### 2. 历史误判提交不会自动修正

如果之前已经有无测试点题目被误判 AC，数据库中的历史记录不会自动改。

后续可以加一个管理脚本或后台功能：

- 找出无测试点题目的历史 `ACCEPTED` 提交
- 改成 `SYSTEM_ERROR` 或 `JUDGE_INVALID`
- 刷新比赛排名

### 3. 根目录不是 Maven 或 npm 项目

后端和前端分别运行：

```text
backend/
frontend/
```

## 常用验证命令

### 后端打包

```powershell
cd E:\PROJECT_SET\cppkid\backend
$env:JAVA_HOME="C:\Users\Lenovo\.jdks\corretto-17.0.8.1"
$env:Path="$env:JAVA_HOME\bin;D:\D\tools\apache\apache-maven-3.8.6\bin;$env:Path"
mvn.cmd -DskipTests package
```

### 前端构建

```powershell
cd E:\PROJECT_SET\cppkid\frontend
npm run build
```

最近一次前端构建通过，但有 Vite/Monaco 体积警告和少量 CSS 嵌套语法警告，不阻塞构建。

### 端口检查

```powershell
Get-NetTCPConnection -LocalPort 8080,5173,3306,6379 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress,LocalPort,State,OwningProcess
```

### Docker 检查

```powershell
docker version
docker ps
docker run --rm gcc:13 g++ --version
```

## 新对话建议开场

可以把下面这段发给新对话：

```text
我在维护一个项目 Tiyu Problem Isle，中文名题屿，本地路径 E:\PROJECT_SET\cppkid，GitHub 是 https://github.com/ozonevistlip/Tiyu-Problem-Isle。

它是一个少儿 / 课堂 C++ 在线判题与比赛练习系统。
后端是 Spring Boot 3 + JDK 17 + MySQL + Redis + MyBatis-Plus，判题使用 Docker gcc:13。
前端是 Vue 3 + Vite + TypeScript + Element Plus + Pinia + Monaco Editor。

请先阅读 PROJECT_BRIEF.md、README.md、backend/README.md、frontend/README.md，再继续工作。
```
