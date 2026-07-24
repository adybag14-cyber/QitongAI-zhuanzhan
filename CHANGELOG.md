# 📋 更新日志

## v1.0.0-25 (2026-07-24) — 当前版本

### ✨ 豆包专属适配（独立分支，不影响元宝）
- 🎯 **原型 setter 填充方案** — 绕过 React 受控组件劫持，`HTMLTextAreaElement.prototype.value` setter + `input`/`change` 事件双触发
- ⌨️ **Enter 优先 + 按钮兜底双保险** — 先触发 `KeyboardEvent('Enter')`，350ms 后兜底点 `#flow-end-msg-send` 按钮
- 👀 **MutationObserver 回复监听** — 检测 `.loading-spinner` 消失 + 文本连续 3 次稳定 → 判定回复完成
- 🔀 **分支隔离** — `fillAndSend(tag)` 统一分发，豆包/元宝 JS 完全不共享选择器、事件类型、探测逻辑
- 📄 **selectors.json 配置外置** — 平台选择器独立配置文件，不改代码适配新站点

### 🐛 修复
- 多标签页切换时 WebView 不显示 — 修复双重创建问题，`LaunchedEffect` 不再调 `initWebView`，统一在 `AndroidView` factory 创建
- 多标签页列表溢出叠加 — 添加 `heightIn(max=500.dp).verticalScroll`
- 底部导航栏按钮换行 — 改回 `Row` + `horizontalScroll`
- 顶部标题栏被左右按钮遮挡 — 缩小按钮至 32dp，图标 18dp，标题 `titleMedium`
- URL 栏点击编辑 — 点击文字直接进入编辑模式
- 关于页收藏管理 — 完整添加/编辑/删除/恢复默认功能
- 底部收藏按钮 toggle — 点击展开，再点击关闭

---

## v1.0.0-14 至 v1.0.0-24 (2026-07-24)

### ✨ 核心功能
- 🍪 **Cookie 持久化** — `PersistentCookieJar.kt`，7 个平台登录态重启不丢
- ⌨️ **真人输入模拟** — `HumanLikeInput.kt`，逐字打字（30~180ms 随机间隔）、快速输入、真人化点击
- 📋 **任务队列调度** — `AutoChatTaskQueue.kt`，多任务串行 + 回调
- 🔧 **四级输入降级** — Selection+TextEvent → InputEvent → execCommand → 直接赋值
- 🔍 **Shadow DOM 穿透** — 递归遍历所有 shadow root 查找元素
- 📝 **Slate 编辑器适配** — `[data-slate-editor]` + Fiber 反查
- 🖱️ **完整点击事件链** — mousedown → mouseup → click → form.submit
- 👀 **MutationObserver 监听** — 检测 AI 回复完成

### 🐛 修复
- 多标签页关闭时索引错乱 — 关闭前面标签时 `currentTabIndex--`
- 浏览器界面被挤压 — WebView 用 `Box` 直接填充
- 覆盖层叠加 — `TabSwitcherOverlay` 和 `BookmarkOverlay` 互斥显示
- 编译报错 `CookieManager` 未导入 — 添加 `import android.webkit.*`

---

## v1.0.0-1 至 v1.0.0-13 (2026-07-24)

### ✨ 基础框架
- 🏗️ **项目骨架** — Kotlin + Jetpack Compose + Material3
- 🌐 **多语言支持** — 简中/繁中台湾/繁中香港/英文，42 个字符串×6 个目录
- 🎨 **苹果水晶玻璃 UI** — `GlassCard` 组件
- 📑 **多标签页管理** — WebView 多实例池
- ⭐ **收藏夹预置** — 豆包/元宝/通义/DeepSeek/Kimi/Google/GitHub
- 🔐 **Cookie 持久化** — 登录态保存/恢复
- 💻 **桌面 UA** — 骗过移动端限制
- 🔍 **字体缩放** + User-Agent 自定义

### 🐛 修复
- 元宝 JS 注入不稳定 — 增加 Shadow DOM 穿透、四级输入降级
- 输入框无内容 — 原型 setter 绕过 React 受控组件
- 发送失败 — Enter + 按钮双保险
- `illegal invocation` 报错 — 正确绑定 `this` 到 textarea 实例

---

## v1.0.0 (2026-07-24)

### ✨ 初始版本
- 🎯 **项目初始化** — 基于 Kotlin + Jetpack Compose + Material3 构建
- 🌐 **多语言支持** — 简中/繁中(台湾)/繁中(香港)/英文，系统自动适配
- 🎨 **自定义图标** — 全新视觉标识
- 🔧 **ARM64 构建适配** — 内置 aapt2 替换脚本，Operit 环境一键编译
- 📦 **包名** — `com.qtwl.YitongAIzhuanzhan`
- 🔐 **签名证书** — `qitong.jks` 已就位

---

## 🚀 技术栈
| 组件 | 版本 |
|------|------|
| Kotlin | 2.3.10 |
| AGP | 9.0.0 |
| Compose BOM | 2026.01.01 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| ABI | arm64-v8a |

---

> **当前版本：** v1.0.0-25 (versionCode=26)  
> **状态：** 测试版（等待各大平台大佬更新后继续优化）  
> **下次更新：** 真机实测确认 textarea testid 和按钮 ID 后补充