# CppKid 网页互动宠物：新对话交接文档

## 1. 新对话建议开场

请把下面这段直接发给新的 Codex 对话：

```text
项目位于 E:\PROJECT_SET\cppkid。
请先完整阅读 E:\PROJECT_SET\cppkid\PET_DRAG_ANIMATION_NEXT_CHAT.md，
然后在现有 Vue 3 + TypeScript + Pinia + PixiJS 宠物架构上，继续实现高级拖拽、扑翼、速度反馈、滑翔、飞向安全边缘和落地动画。
请保留当前工作区所有未提交改动，不要推翻已有架构；完成后运行 npm run build，并用 /pet-preview 验证桌面和移动端交互。
```

---

## 2. 项目基本信息

- 项目根目录：`E:\PROJECT_SET\cppkid`
- 前端目录：`E:\PROJECT_SET\cppkid\frontend`
- 前端技术栈：Vue 3、TypeScript、Composition API、Pinia、Vue Router、Element Plus、PixiJS 8、Vite、SCSS、Monaco Editor
- 后端：Spring Boot，但宠物功能当前完全是前端功能，不需要修改后端
- 安装依赖：`cd frontend && npm install`
- 启动前端：`cd frontend && npm run dev`
- 生产构建：`cd frontend && npm run build`
- 开发预览页：`http://127.0.0.1:5173/pet-preview`

注意：`/pet-preview` 只在 Vite 开发环境中注册，不进入生产路由。

---

## 3. 当前工作区状态

当前宠物功能仍是未提交改动。开始工作前先执行：

```powershell
cd E:\PROJECT_SET\cppkid
git status --short
```

不要覆盖、回退或清理现有改动，也不要使用 `git reset --hard`、`git checkout --` 等破坏性命令。

仓库根目录已有其他用户文件或未跟踪归档，例如：

```text
CODE_VISUALIZER_NEXT_CHAT.md
PROJECT_NEXT_CHAT.md
cppkid.zip
frontend-dist.tar.gz
```

这些文件不属于宠物拖拽任务，不要修改或删除。

---

## 4. 已完成的宠物功能

### 4.1 项目接入

- 宠物已挂载在学生端布局：`frontend/src/layouts/StudentLayout.vue`
- 宠物已挂载在教师端布局：`frontend/src/layouts/TeacherLayout.vue`
- 学生与教师进入布局后都会收到欢迎气泡
- 学生题目页已接入输入代码、运行、自测、错误、通过和完成练习等业务事件
- 教师和学生共用的代码可视化页已接入思考、成功和错误事件
- 页面通过 `data-pet-exclusion` 标记编辑器、导航栏、提交按钮和底部控制栏等禁入区域

### 4.2 当前模块结构

```text
frontend/src/
├── assets/pet/
│   └── pet.png
├── components/pet/
│   ├── PetWidget.vue
│   ├── PetCanvas.vue
│   ├── PetBubble.vue
│   └── PetControlMenu.vue
├── pet/
│   ├── PetRenderer.ts
│   ├── PetAnimator.ts
│   ├── PetStateMachine.ts
│   ├── PetParticleManager.ts
│   ├── PetEventBus.ts
│   ├── petConfig.ts
│   ├── petDialogues.ts
│   └── petTypes.ts
├── stores/
│   └── petStore.ts
├── composables/
│   └── usePet.ts
└── views/common/
    └── PetPreviewView.vue
```

### 4.3 当前状态与动画

现有状态：

```text
idle
happy
error
success
sleeping
surprised
thinking
speaking
dragging
hidden
```

已经实现：

- Idle 呼吸、漂浮和轻微摇摆
- Happy 双跳和星星粒子
- Error 摇头和问号粒子
- Success 跳跃、旋转、星星和彩纸
- Sleeping 下沉、缩小和 Z 粒子
- Surprised 放大、跳动和感叹号
- Thinking 倾斜、漂浮和思考点
- Dragging 缩小并根据横向拖动方向轻微旋转
- 状态优先级、打断控制、动画结束后返回 Idle
- 页面后台时暂停 Pixi Ticker，回到前台时恢复
- `prefers-reduced-motion` 和低性能模式

### 4.4 当前拖拽行为

当前已有一个基础可用版本：

- 使用统一 Pointer Events 接收鼠标、触摸和触控笔输入
- 移动超过 `PET_DRAG_THRESHOLD` 后才进入 `dragging`
- 宠物 DOM 容器直接跟随指针移动，没有追赶延迟
- 拖动期间不把每一帧位置写入 Pinia/localStorage
- 松手后才更新并保存最终位置
- 区分点击与拖拽，拖拽结束不会再次触发点击
- 移动端长按打开设置菜单
- 松手后计算安全位置并吸附到候选边缘
- 浏览器尺寸变化后重新约束宠物位置
- 拖动时临时隐藏气泡和菜单

当前实现主要集中在 `PetWidget.vue`，还不具备完整飞行物理与分层控制。

### 4.5 资源处理

原始图片：

```text
D:\F\codex-pets\runs\winged-kuriboh\decoded\base.png
```

原图实际是洋红背景的 RGB PNG，不带 Alpha。已经只做了确定性的洋红背景透明化与裁边，没有重绘角色，生成：

```text
frontend/src/assets/pet/pet.png
```

当前资源是身体和左右翅膀合成在同一张透明 PNG 中。

---

## 5. 对外业务事件接口

其他页面不要直接操作 Pixi Sprite，统一使用：

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

底层事件总线支持：

```text
PET_WELCOME
PET_CLICK
PET_CODE_START
PET_CODE_ERROR
PET_CODE_SUCCESS
PET_EXERCISE_COMPLETE
PET_THINKING
PET_IDLE
PET_SLEEP
PET_WAKE_UP
PET_SHOW_MESSAGE
PET_HIDE
PET_SHOW
```

---

## 6. 新一轮任务：高级拖拽与飞行动画

附件中的完整需求位于：

```text
C:\Users\Lenovo\.codex\attachments\5435639c-bcda-448a-9904-fce819e6887f\pasted-text.txt
```

新对话必须先完整读取该文件。下面是核心目标摘要。

### 6.1 状态流程

需要把当前基础流程扩展为：

```text
idle
→ pointerDown
→ dragging
→ released
→ gliding / flyingToEdge
→ landing
→ idle
```

新增或完善状态：

```text
pointerDown
dragging
released
gliding
flyingToEdge
landing
```

推荐优先级：

```text
hidden
success
error
dragging
released
gliding
flyingToEdge
landing
surprised
speaking
thinking
happy
sleeping
idle
```

### 6.2 必须实现的交互

- 用户真正抓住宠物，宠物直接跟随指针，不做寻路追赶
- 保存抓取偏移 `grabOffsetX/grabOffsetY`，按住翅膀或身体边缘时不能瞬移到指针中心
- 桌面和移动端使用不同拖动阈值
- 使用 Pointer Capture，并在松开、取消和卸载时释放
- 固定长度速度样本或环形缓冲区，按时间差计算平滑速度
- 限制最大速度、最大倾角和粒子数量
- 低速拖动为轻拍悬停，中速正常扑翼，高速快速扑翼并产生少量速度线/残影
- 拖动中突然停止时切换为悬停式扑翼，禁止睡眠
- 松开时根据释放速度选择滑翔或直接飞向安全边缘
- 高速释放先短暂滑翔，不能飞出屏幕
- 飞向边缘必须避开 `data-pet-exclusion` 区域
- 到达后播放减速、下落、压缩和回弹，再回到 Idle
- `prefers-reduced-motion` 下关闭惯性、残影和速度线，改为直接平滑吸附

### 6.3 动画分层

不要让同一组 scale/rotation 被多个动画直接覆盖。建议分层维护：

```text
PetRoot                 // 页面位置、滑翔、飞向边缘
├── Shadow              // 独立阴影，不跟身体一起拉伸
├── DirectionContainer  // 水平朝向翻转
│   └── BodyMotion      // 倾斜、压缩、回弹、呼吸
│       └── PetSprite
├── WingEffectLayer     // 单图兼容的翅膀残影、光晕
├── SpeedLineLayer
├── ParticleLayer
└── InteractionHitArea
```

分别维护并最终合成：

```text
baseScale
directionScale
animationScale
dragScale
baseRotation
dragRotation
```

### 6.4 翅膀方案

当前项目只有一张合成宠物图。原图左右翼与身体边缘有连接和重叠，不建议自动强拆后直接投入生产，否则容易出现断边或翅膀根部穿帮。

本轮优先采用需求中的“单图兼容方案”：

- 通过身体高频小幅旋转、轻微横纵压缩模拟扑翼
- 高速时在翅膀附近生成有限的局部残影或光晕
- 使用柔和速度线和空气粒子增强飞行感
- 滑翔时减少扑动、保持展开感
- 落地时通过回弹模拟翅膀收拢
- 代码层预留 `leftWing/rightWing` 可选 Sprite 接口

如果以后美术提供独立透明翼图，再让 `PetWingAnimator` 自动切换到独立双翼模式。

### 6.5 建议新增模块

在现有架构上扩展，不要推翻：

```text
frontend/src/pet/
├── PetDragController.ts
├── PetWingAnimator.ts
├── PetMovementController.ts
├── PetSafeAreaManager.ts
├── PetRenderer.ts           // 调整容器层级和暴露接口
├── PetAnimator.ts           // 身体动画及落地回弹
├── PetStateMachine.ts       // 新增状态与生命周期
├── PetParticleManager.ts    // 增加速度线和有限残影
├── petConfig.ts             // 集中参数
└── petTypes.ts              // 新增速度、拖拽和飞行类型
```

职责：

- `PetDragController`：Pointer 事件、Pointer Capture、点击/拖拽判断、抓取偏移、速度采样、释放速度
- `PetWingAnimator`：单图兼容扑翼、悬停、高速扑翼、滑翔展开、落地收拢，以及未来独立双翼 Sprite
- `PetMovementController`：实时位置、惯性滑翔、飞向安全边缘、缓动与落地目标
- `PetSafeAreaManager`：收集禁入区域、候选边缘点、碰撞排除和 Resize 修正

### 6.6 集中配置参数

需要把目前散落的拖拽魔法数字统一迁移到 `petConfig.ts`：

```text
dragThreshold
mobileDragThreshold
clickMaxDuration
clickMaxDistance
longPressDuration
dragCooldown
maxDragSpeed
velocitySampleCount
dragSmoothing
maxTiltAngle
directionFlipThreshold
directionFlipCooldown
wingIdleSpeed
wingHoverSpeed
wingDragMinSpeed
wingDragMaxSpeed
wingMaxAngle
glideDuration
glideDistance
edgeSnapPadding
landingDuration
particleSpeedThreshold
maxTrailCount
```

每项提供合理默认值和简洁中文注释。

---

## 7. 当前代码中需要重点重构的位置

### `PetWidget.vue`

当前文件同时承担：

- PointerDown/Move/Up
- 点击和拖拽判断
- 长按菜单
- DOM 实时位置
- 安全区域计算
- 边缘吸附
- 自动行为
- 业务事件映射

新一轮应把拖拽、移动与安全区域逻辑拆出控制器，`PetWidget` 只保留 Vue 生命周期、HTML UI 和控制器装配。

### `PetRenderer.ts`

当前只有单一 `body` Container、独立 shadow 和粒子容器。需要增加方向容器、身体动画容器、翅膀效果层和速度线层，并提供拖拽帧数据入口。

### `PetAnimator.ts`

当前拖拽只接收横向位移并映射到轻微 rotation。需要改为消费平滑后的速度、方向、加速度或急转弯数据，并分别控制身体、阴影和兼容扑翼表现。

### `PetStateMachine.ts`

当前状态定义支持优先级、打断和完成回退，但没有每帧 `update` 生命周期，也没有新拖拽阶段。应在保留现有 API 的基础上扩展状态生命周期。

### `PetParticleManager.ts`

当前支持星星、爱心、问号、感叹号、彩纸、睡眠和思考粒子。需要增加有限的速度线、空气粒子和局部翅膀残影，并在低性能/减少动态模式下关闭。

---

## 8. 验收清单

至少逐项验证：

1. 普通点击不移动位置，不进入拖拽。
2. 缓慢拖动直接跟手，姿态轻微，松开后飞向最近安全边缘并落地。
3. 快速拖动扑翼加快，有少量速度线，松开后短暂滑翔但不越界。
4. 快速左右甩动不出现镜像闪烁、尺寸累计或坐标异常。
5. 按住但停止移动时进入悬停扑翼，不进入睡眠。
6. `pointercancel` 后正确释放捕获并退出 dragging。
7. 连续拖动不会累计 scale/rotation，不增加多余 Ticker 或泄漏粒子。
8. 手机端拖动时页面不同时滚动；未拖宠物时页面仍可正常滚动。
9. 减少动态效果模式关闭滑翔、残影和速度线。
10. Success/Error/Sleeping/气泡/隐藏/恢复等旧功能不回归。
11. 学生端和教师端均显示宠物。
12. `npm run build` 通过，浏览器控制台无新增错误。

开发预览页已经提供按钮，可继续扩展测试拖拽速度和状态信息：

```text
frontend/src/views/common/PetPreviewView.vue
```

建议增加只在开发页显示的调试信息：当前状态、平滑速度、释放速度、朝向、低性能模式和减少动态效果状态。

---

## 9. 已知构建信息

基础宠物版本曾通过：

```powershell
cd E:\PROJECT_SET\cppkid\frontend
npm run build
```

Vite 构建中的 Tree-sitter `fs/path` externalized、`eval`、既有 CSS 嵌套以及大 chunk 警告来自项目原有依赖/样式，不是宠物功能的新增编译错误。新对话完成高级拖拽后仍需重新执行完整构建。

---

## 10. 重要实现原则

- 不重绘或改变宠物脸、颜色和整体形象。
- 不把所有逻辑继续堆进 `PetWidget.vue`。
- 所有动作经过有限状态机。
- PointerMove 只写入轻量目标数据，复杂计算放入统一 Pixi Ticker。
- 拖动中不逐帧写 Pinia/localStorage；最终停靠后再保存。
- 不为每次动画堆叠无法取消的 `setTimeout`。
- 退出状态后恢复默认 scale、rotation、alpha、阴影和临时效果。
- 页面后台暂停；恢复时清空旧速度样本，防止瞬时速度爆炸。
- 组件卸载时清理 Pointer Capture、事件、Ticker、动画、粒子和 Pixi Application。
- 保留学生端与教师端现有业务接入。
- 保留现有气泡、菜单、主题适配、安全区域与本地设置。
