# CppKid OJ

面向 C++ 入门课堂的在线评测与教学平台。教师可以组织班级、题库和比赛，学生可以在线编写、运行和提交 C++17 代码，超级管理员可以管理教师账号、课程内容、安全策略和站点运行状态；项目同时提供浏览器端代码执行可视化和 PixiJS 互动学习宠物，让抽象的程序执行过程与判题反馈更直观。

## 本轮增加与修改的功能

- 新增多宠物系统：超级管理员统一创建宠物、设置独立性格提示词、上传静态预览图和动画图集，并在预览后发布；老师可使用全部已发布宠物，并针对自己创建的每名学生逐只发放或收回。学生未获授权时只能看预览图。
- 获授权的学生可将宠物设为可拖动的悬浮宠物，并通过 DeepSeek 与宠物聊天。每只宠物的会话只在当前浏览器内存中保留最近 20 条消息，不写入后端数据库或缓存。
- 新增麦晓雯、哈基蜂的预览 WebP 与 8×9 动画图集素材，以及素材生成脚本；哈基蜂奔跑帧包含交替迈步和落地姿态。仓库素材不会自动创建数据库记录，需由超级管理员在管理页面上传并发布。
- 超级管理员先创建班级类型（例如 4.0、4.5），再为该类型设置课次及基础、成长、挑战三类课后作业说明和讲解视频；同类型班级共享课后内容。
- 老师可从已有类型创建多个班级、管理自己创建的学生账号，并为每名学生的**每节课**单独选择一个类别。比如同一学生第一节课选基础、第二节课选挑战；选成长时只看成长内容。
- 学生按班级进入课后学习，每节课是独立文件夹，仅显示该课次分配类别的已发布作业和视频；视频访问也按班级、课次和类别校验。
- 账号管理调整为超级管理员新增、查看、修改、删除及重置老师账号，老师对自己创建的学生账号执行相应操作。密码以哈希保存，无法查看原密码，只能重置。
- 自主注册入口和接口保留，默认关闭；超级管理员可在网站设置中重新开启。
- 增加数据库迁移脚本、视频目录及上传相关部署配置。升级已有数据库的顺序见下文“数据库初始化与升级”。

## 当前功能

### 班级类型与课后学习

- 超级管理员先创建班级类型（如 4.0、4.5），并为类型维护课次。每节课可发布基础、成长、挑战三个类别的作业说明和讲解视频，同类型班级共用这些内容。
- 老师从已有类型创建自己的班级，可创建多个班级；老师创建学生账号、将学生加入班级，并为每名学生在每节课分别选择基础、成长或挑战类别。未设置的课次不向该学生展示分类内容。
- 账号按“超级管理员管理老师、老师管理自己创建的学生”分级。管理员仍可查看全站用户，但学生账号的修改、重置密码与删除由所属老师操作；原密码不可查看。
- 学生在“课后学习”按班级进入每节课的独立文件夹，只能查看自己类别的已发布作业和视频。视频下载接口也验证班级成员、课次发布状态与类别。
- 自主注册功能保留，但新安装默认关闭。超级管理员仍可在网站设置中重新开启；关闭时注册接口会拒绝请求。

已有数据库需要在备份后依次执行一次 `backend/sql/teaching_migration.sql` 和 `backend/sql/lesson_student_tier_migration.sql`；已执行前者的数据库只需执行后者。新数据库直接使用 `backend/sql/schema.sql`。第一次迁移会将只属于一位老师班级的旧学生归属给该老师；跨老师班级的旧学生保持无归属，需要人工处理。第二次迁移把原班级类别复制到已有课次，之后的新课次由老师逐课设置。课后视频保存在服务端 `VIDEO_DIR` 指定的目录，未设置时开发环境为 `./teaching-videos`，部署时应设置持久目录并备份。

课后作业目前以文字内容展示，学生提交和批改流程尚未包含在这一模块中。

### 在线评测与课堂管理

- 教师端：班级与学生管理、题目与测试点管理、提示管理、比赛发布、提交记录和排行榜。
- 学生端：比赛列表、题目作答、Monaco C++17 编辑器、提示解锁、代码自测、正式提交、评测详情和排行榜。
- 评测服务：Redis 队列调度提交，Docker `gcc:13` 环境负责编译并运行 C++17 代码。
- 身份认证：JWT + 服务端会话，区分教师、学生和超级管理员角色，支持封禁账号与强制下线。
- 超级管理员端：统一登录后进入 `/super-admin`，管理老师账号与班级类型、查看全站用户，并管理在线会话、注册开关、访问量、服务器状态和全站公告。

### 超级管理员与会话安全

超级管理员与老师、学生共用 `/login` 登录入口。登录成功后，前端根据 `SUPER_ADMIN` 角色跳转到 `/super-admin`，所有 `/api/super-admin/**` 接口仍会在后端执行独立权限校验。

管理端支持：

- 查看、搜索和筛选全部账号；
- 查看全站用户详细资料，新增、修改、禁用、解封、重置密码和删除老师账号；
- 老师端新增、查看、修改、删除自己创建的学生账号，并可重置其密码；
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

教师端和学生端页面均挂载了 Vue 3 + Pinia + PixiJS 互动宠物。超级管理员可在“宠物管理”创建宠物、设置性格提示词、上传预览图与动画图集、预览和发布。老师可使用全部已发布宠物，并在学生账号页面按学生、按宠物分别发放或收回；学生无需兑换，未获授权时只能查看已发布宠物的预览图，无法使用动画或聊天。获授权后可在“宠物伙伴”中选择悬浮宠物并拖动。主要交互能力包括：

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

新上传的预览图和动画图集均为**静态 WebP**，单文件不超过 8 MB；预览图宽高不超过 2048 像素，图集固定 1536×1872 像素（8 列×9 行，每格 192×208）。图集的九行依次为待机、向右奔跑、向左奔跑、挥手、跳跃、失败、等待、奔跑和检查。仓库中的 `frontend/src/assets/pet/mai-xiaowen-*` 与 `frontend/src/assets/pet/haji-bee-*` 是可供管理员上传的素材；生成方法见 `scripts/build-mai-xiaowen-assets.py` 和 `scripts/build-haji-bee-assets.py`。上传后的文件由后端保存在 `PET_DIR`，不随 Git 提交。已有 PNG 资源仍可读取。

宠物聊天使用后端配置的 DeepSeek API 和每只宠物的性格提示词。浏览器按用户及宠物分别维护会话，始终只保留最近 20 条消息；页面重新加载或退出登录后，会话记录不再保留。后端只处理本次请求传入的上下文，不持久化聊天记录。`DEEPSEEK_API_KEY` 应放在本地环境或部署环境中，不要写入仓库。

#### 新宠物上传与发放

1. 超级管理员进入“宠物管理”并点击“新增宠物”，填写名称、简介和独立的性格提示词，同时选择静态预览图与动画图集，然后点击“保存资料并上传图片”。管理列表会分别显示两张图片的上传状态；若只上传成功一张，可在编辑窗口单独补传。
2. 在管理列表点击“预览”，检查静态图、待机动画和试聊效果。两张图片都上传完成后，点击“发布”，宠物才会出现在老师和学生的宠物列表中。
3. 老师在“学生账号”中对某位学生点击“逐只管理宠物”，分别开启或关闭每只已发布宠物的权限。学生获发放后可在“宠物伙伴”选择悬浮宠物、拖动并聊天；收回后该学生仍能看预览图。

仓库提供的 `mai-xiaowen-preview.webp`、`mai-xiaowen-atlas.webp`、`haji-bee-preview.webp` 和 `haji-bee-atlas.webp` 位于 `frontend/src/assets/pet/`，可直接用于上述上传。哈基蜂的奔跑效果可先查看同目录的 `haji-bee-running-demo.gif`。这些源素材随代码提交，但宠物资料与老师的发放记录存于数据库，管理员上传的实际图片存于 `PET_DIR`；更新代码后仍需在目标环境上传并发布。

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

若数据库尚未安装本轮课程班级功能，在完成上述超级管理员迁移后，依次执行以下两个一次性脚本。已经执行过 `teaching_migration.sql` 的数据库只执行第二个脚本：

```bash
mysql -uroot -p cppkid_oj < sql/teaching_migration.sql
mysql -uroot -p cppkid_oj < sql/lesson_student_tier_migration.sql
```

PowerShell 可用 `mysql --database=cppkid_oj --execute="source <脚本绝对路径>"` 逐个执行。迁移前请备份数据库，脚本不可重复执行；课后视频文件还应单独备份。

宠物系统需要在已升级的数据库上再执行一次 `pet_migration.sql`；全新数据库直接使用最新 `schema.sql`：

```powershell
Set-Location .\backend
$petMigration = (Resolve-Path .\sql\pet_migration.sql).Path.Replace('\', '/')
mysql -uroot -p --database=cppkid_oj --execute="source $petMigration"
```

后端还需配置 `PET_DIR` 为可写、需备份的持久目录，并配置 `DEEPSEEK_API_KEY`。Windows 本地开发可把它们写入被 Git 忽略的 `scripts/local.env.ps1`；生产环境可参照 `deploy/cppkid.env.example`。宠物迁移会创建内置宠物记录，但不会自动发放给学生。数据库记录和 `PET_DIR` 文件应一起备份与迁移。

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
