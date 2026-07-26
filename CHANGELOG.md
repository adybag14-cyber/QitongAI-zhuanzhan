# 📋 更新日志

## v1.0.0 (2026-07-26) — 正式版 🎉

### ✨ 新增（来自 adybag14-cyber 工程师 PR #1）
- 🔗 **端到端多 AI 流水线串联** — `MultiAiPipeline.kt` + `MultiAiPipelineRunner.kt`
- 📋 **AI 平台注册中心** — `AiPlatformRegistry.kt`
- 💬 **多 AI 流水线对话框** — `MultiAiPipelineDialog.kt`
- 🧪 **完整测试覆盖** — 单元测试 + 集成测试
- 🇬🇧 **完整英文本地化** — `README_EN.md` + 英文资源
- 🔧 **Windows 构建修复** — 跨平台兼容

### ✨ 新增（豆包专属适配）
- 🎯 **原型 setter 填充方案** — 绕过 React 受控组件劫持
- ⌨️ **Enter 优先 + 按钮兜底双保险发送**
- 👀 **MutationObserver 回复监听**
- 🔀 **分支隔离** — `fillAndSend(tag)` 统一分发
- 📄 **selectors.json 配置外置**

### 🐛 修复
- 多标签页切换时 WebView 不显示
- 多标签页列表溢出叠加
- 底部导航栏按钮换行
- 顶部标题栏被遮挡
- URL 栏点击编辑
- 关于页收藏管理
- 底部收藏按钮 toggle
- JVM 内存不足导致编译失败
- Android WebView 输入框焦点问题
- Windows `assembleDebug` 和 `testDebugUnitTest` 兼容性

---

## v1.0.0-25 至 v1.0.0-26 (2026-07-24)

### ✨ 核心功能
- 🍪 **Cookie 持久化** — `PersistentCookieJar.kt`
- ⌨️ **真人输入模拟** — `HumanLikeInput.kt`
- 📋 **任务队列调度** — `AutoChatTaskQueue.kt`
- 🔧 **四级输入降级**
- 🔍 **Shadow DOM 穿透**
- 📝 **Slate 编辑器适配**
- 🖱️ **完整点击事件链**
- 👀 **MutationObserver 监听**

### 🐛 修复
- 多标签页关闭时索引错乱
- 浏览器界面被挤压
- 覆盖层叠加
- 编译报错 `CookieManager` 未导入

---

## v1.0.0-1 至 v1.0.0-24 (2026-07-24)

### ✨ 基础框架
- 🏗️ **项目骨架** — Kotlin + Jetpack Compose + Material3
- 🌐 **多语言支持** — 简中/繁中台湾/繁中香港/英文
- 🎨 **苹果水晶玻璃 UI** — `GlassCard` 组件
- 📑 **多标签页管理** — WebView 多实例池
- ⭐ **收藏夹预置** — 豆包/元宝/通义/DeepSeek/Kimi/Google/GitHub
- 🔐 **Cookie 持久化**
- 💻 **桌面 UA** — 骗过移动端限制
- 🔍 **字体缩放** + User-Agent 自定义

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

> **当前版本：** v1.0.0 (versionCode=28)  
> **状态：** 正式版 🎉  
> **发布日期：** 2026-07-26