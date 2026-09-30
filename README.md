# CppKid OJ

面向 C++ 入门课堂的在线评测与教学平台。教师可以组织班级、题库和比赛，学生可以在线编写、运行和提交 C++17 代码，超级管理员可以集中管理账号、安全策略和站点运行状态；项目同时提供浏览器端代码执行可视化和 PixiJS 互动学习宠物，让抽象的程序执行过程与判题反馈更直观。

## 当前功能

### 在线评测与课堂管理

- 教师端：班级与学生管理、题目与测试点管理、提示管理、比赛发布、提交记录和排行榜。
- 学生端：比赛列表、题目作答、Monaco C++17 编辑器、提示解锁、代码自测、正式提交、评测详情和排行榜。
- 评测服务：Redis 队列调度提交，Docker `gcc:13` 环境负责编译并运行 C++17 代码。
- 身份认证：JWT + 服务端会话，区分教师、学生和超级管理员角色，支持封禁账号与强制下线。
- 超级管理员端：统一登录后进入 `/super-admin`，管理全站账号、在线会话、注册开关、访问量、服务器状态和全站公告。

### 超级管理员与会话安全

超级管理员与老师、学生共用 `/login` 登录入口。登录成功后，前端根据 `SUPER_ADMIN` 角色跳转到 `/super-admin`，所有 `/api/super-admin/**` 接口仍会在后端执行独立权限校验。

管理端支持：

- 查看、搜索和筛选全部账号；
- 查看用户详细资料，新增或修改老师、学生账号；
- 禁用、解封、重置密码和删除无效账号；
- 按账号强制退出，或终止指定设备会话；
- 查看最近 15 分钟活跃的在线用户；
- 开启或关闭自主注册；
- 查看访问量、JVM 内存、系统负载和运行时间；
- 创建、编辑、发布和删除全站公告。

登录 Token 包含服务端会话 ID。每次访问受保护接口时，后端同时检查 JWT、用户状态、角色和会话状态。因此账号被禁用、密码被重置或会话被强制终止后，已有 Token 会立即失效。

### C++ 代码执行可视化

代码可视化在浏览器内完成，不依赖后端执行用户代码：

```text
C++ 源码
→ Tree-sitter C++ 语法校验
→ 教学 AST
→ 浏览器端解释器
→ TraceEvent 与状态快照
→ GSAP + SVG/DOM 动画
```

当前教学子集支持：

- 基础变量和赋值；
- 算术、比较与逻辑运算；
- `if / else`、`for / while`、`break / continue`；
- 一维数组、二维数组和字符串；
- `cin / cout`；
- 简单函数、参数、调用和 `return`。

工作台支持单步执行、自动播放、暂停、上一步、重置和速度调节，并对数组规模、循环次数、调用深度、总步骤和执行时间设置安全上限。学生在题目编辑器中修改的代码可以同步到可视化页面。

### 网页互动宠物

教师端和学生端页面均挂载了 Vue 3 + Pinia + PixiJS 互动宠物，主要能力包括：

- 待机、开心、错误、成功、睡眠、惊讶、思考和说话等状态；
- 星星、彩纸、问号、感叹号、睡眠符号、速度线和空气粒子；
- HTML 气泡、设置菜单、静音、自动行为、低性能模式和本地配置持久化；
- 鼠标、触摸和触控笔统一 Pointer Events 交互；
- 点击与拖拽区分、抓取偏移、Pointer Capture 和速度平滑采样；
- 跟手拖动、方向动画、高速扑翼、短暂滑翔、安全边缘吸附和落地回弹；
- `prefers-reduced-motion`、移动端和低性能设备降级；
- 导航栏、编辑器、按钮和弹窗等 `data-pet-exclusion` 禁入区域避让。

拖拽状态流程：

```text
idle
→ pointerDown
→ dragging
→ released
→ gliding / flyingToEdge
→ landing
→ idle
```

宠物使用透明 WebP 动画图集，包含待机、左右奔跑、挥手、跳跃、失败、等待、运行和检查等帧序列：

```text
frontend/src/assets/pet/winged-kuriboh-spritesheet.webp
```

开发环境可直接访问以下页面测试宠物状态、粒子和拖拽数据：

```text
http://127.0.0.1:5173/pet-preview
```

`/pet-preview` 只在 Vite 开发环境注册，不进入生产路由。

业务页面不要直接操作 Pixi Sprite，请通过 `usePet()` 发送事件：

```ts
import { usePet } from '@/composables/usePet'

const pet = usePet()

pet.welcome()
pet.codeStart()
pet.thinking()
pet.codeError('再检查一下括号和分号吧。')
pet.codeSuccess()
pet.exerciseComplete()
pet.showMessage('先看看输入范围。', { bubbleType: 'tip' })
pet.hide()
pet.show()
```

宠物模块主要位于：

```text
frontend/src/components/pet/  Vue 气泡、菜单、画布和组件装配
frontend/src/pet/             Pixi 渲染、状态机、拖拽、移动、图集和粒子
frontend/src/stores/petStore.ts
frontend/src/composables/usePet.ts
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端基础 | Vue 3、TypeScript、Composition API、Vite、Vue Router、Pinia、Axios |
| 前端界面 | Element Plus、SCSS、Monaco Editor |
| 可视化 | Tree-sitter、Web Worker、GSAP、SVG/DOM |
| 互动宠物 | PixiJS 8、动画图集、Pointer Events |
| 后端 | Spring Boot 3.3、JDK 17、MyBatis-Plus、MySQL、Redis、JWT + 服务端会话 |
| 评测环境 | Docker、`gcc:13` |

## 项目结构

```text
cppkid/
├── backend/                      Spring Boot API、评测队列和 Docker 评测 worker
│   └── sql/                      完整建库脚本与增量迁移脚本
├── frontend/                     Vue 3 前端、代码可视化和互动宠物
├── deploy/                       Ubuntu、systemd 和 Nginx 部署配置
├── scripts/                      Windows 本地开发启动与停止脚本
└── PET_DRAG_ANIMATION_NEXT_CHAT.md 宠物拖拽动画开发交接
```

## 环境要求

- JDK 17；
- Maven 3.8+；
- Node.js 16+ 与 npm；
- MySQL 8；
- Redis；
- Docker（执行代码评测时需要）。

## 快速启动

### Windows 一键启动

首次使用时复制本地工具配置，并按本机安装路径修改：

```powershell
Copy-Item .\scripts\local.env.example.ps1 .\scripts\local.env.ps1
notepad .\scripts\local.env.ps1
```

启动 Redis、后端和前端：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

停止由脚本启动的服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

默认地址：

- 前端：`http://127.0.0.1:5173/`
- 后端：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`
- 宠物预览：`http://127.0.0.1:5173/pet-preview`

### 数据库初始化与升级

#### 新数据库

创建数据库并导入表结构：

```bash
cd backend
mysql -uroot -p < sql/schema.sql
```

PowerShell 不支持上述 Bash 输入重定向时，可以使用 MySQL 的 `source` 命令：

```powershell
Set-Location .\backend
$schema = (Resolve-Path .\sql\schema.sql).Path.Replace('\', '/')
mysql -uroot -p --execute="source $schema"
```

#### 现有数据库升级

升级前先停止后端并备份数据库：

```bash
mysqldump -uroot -p --single-transaction --routines --triggers \
  --databases cppkid_oj --result-file=backup_before_super_admin.sql
```

然后执行一次增量迁移。该脚本不要重复执行：

```bash
mysql -uroot -p cppkid_oj < sql/super_admin_migration.sql
```

PowerShell 可以执行：

```powershell
Set-Location .\backend
$migration = (Resolve-Path .\sql\super_admin_migration.sql).Path.Replace('\', '/')
mysql -uroot -p --database=cppkid_oj --execute="source $migration"
```

迁移完成后验证新增表和字段：

```bash
mysql -uroot -p cppkid_oj -e "SHOW TABLES LIKE 'user_session'; SHOW TABLES LIKE 'site_setting'; SHOW TABLES LIKE 'site_visit'; SHOW TABLES LIKE 'announcement'; SHOW COLUMNS FROM user LIKE 'last_login_at'; SHOW COLUMNS FROM user LIKE 'last_active_at';"
```

#### 初始化超级管理员

开发环境首次启动且数据库中没有超级管理员时，会自动创建以下账号：

- 账号：`superadmin`
- 密码：`CppKid@Admin123`

该默认值只用于本地开发，请勿用于公网环境。生产环境默认不会自动创建管理员，需在部署环境中显式配置：

```env
SUPER_ADMIN_ENABLED=true
SUPER_ADMIN_USERNAME=superadmin
SUPER_ADMIN_PASSWORD=replace-with-a-strong-password
```

初始化完成后，可以将 `SUPER_ADMIN_ENABLED` 改回 `false`。如果数据库中已经存在 `SUPER_ADMIN`，启动引导器不会重复创建。

### 手动启动后端

确认 MySQL、Redis 和 Docker 已启动，并根据本机环境检查 `backend/src/main/resources/application.yml` 中的数据库、Redis 和 JWT 配置，然后启动后端：

```bash
mvn spring-boot:run
```

检查 Docker 评测镜像：

```bash
docker run --rm gcc:13 g++ --version
```

### 手动启动前端

```bash
cd frontend
npm install
npm run dev
```

开发环境 API 地址通过 `frontend/.env.development` 配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 构建与检查

前端生产构建：

```bash
cd frontend
npm run build
```

后端测试：

```bash
cd backend
mvn test
```

当前前端已完成生产构建以及桌面、移动端宠物交互检查。构建时 Tree-sitter 的 `fs/path` externalized、依赖中的 `eval`、既有 CSS 嵌套和大分包提示属于当前依赖链的已知警告。

## 开发说明

- 教师创建的题目至少需要一个测试点，才能用于比赛和判题。
- 没有测试点的提交会被评测器拒绝，不会被标记为通过。
- 评测任务依赖 Redis，实际编译运行依赖 Docker。
- 旧版 JWT 不包含服务端会话 ID，升级后会自动失效，用户需要重新登录。
- `super_admin_migration.sql` 是一次性迁移脚本，重复执行前应先检查 `user` 表是否已有新增字段。
- 宠物拖动过程中不会逐帧写入 Pinia 或 `localStorage`，仅在最终停靠后保存位置。
- 新的宠物禁入区域应添加 `data-pet-exclusion`。
- 修改宠物状态、动画速度和滑翔参数时，优先调整 `frontend/src/pet/petConfig.ts`。
- 开发预览、构建产物、运行日志和 IDE 文件不应提交到仓库。

## 生产部署

生产环境与本地开发配置分离：

- 后端生产配置：`backend/src/main/resources/application-prod.yml`
- 前端生产 API 地址：`frontend/.env.production`
- systemd、Nginx 和服务器环境模板：`deploy/`

目标平台为 Ubuntu 22.04 LTS 64 位。完整部署步骤请参阅 [deploy/README.md](deploy/README.md)。
