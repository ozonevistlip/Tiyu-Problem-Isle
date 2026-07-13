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

## C++ 代码执行可视化

教师端和学生端均可进入“代码可视化”页面。该页面不依赖后端：代码会在浏览器的 Web Worker 中完成语法校验、教学 AST 转换、解释执行和执行轨迹生成，主线程只负责 Monaco 编辑器、SVG/DOM 渲染和 GSAP 动画。

执行链路：

```text
C++ 源码 → Tree-sitter C++ 语法校验 → 教学 AST → 解释器 → TraceEvent + 快照 → 动画工作台
```

当前支持的教学子集包括：基础变量、算术/比较/逻辑运算、`if / else`、`for / while`、`break / continue`、一维和二维数组、字符串和下标访问、`cin / cout`、简单函数与 `return`。运行控制支持单步、自动播放、暂停、上一步、重置和速度调整。

为保证课堂演示稳定，执行器默认限制数组大小、矩阵单元数、循环次数、调用深度、总步骤和执行时间；超出范围时会展示中文提示，而不会输出难懂的错误堆栈。
