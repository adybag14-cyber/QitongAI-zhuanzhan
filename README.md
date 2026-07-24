# 🔄 綦桐AI转站

> **多AI网页自动化对话工具** — 像真人一样操作免费网页AI，自动串联多平台对话

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-purple)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-2026.01.01-blue)](https://developer.android.com/jetpack/compose)
[![AGP](https://img.shields.io/badge/AGP-9.0.0-green)](https://developer.android.com/build)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com)

---

## 💡 项目理念

**"让安卓设备成为你的AI中转站"**

以 WebView 多实例池模拟真人浏览器，实现：
- 🧑‍💻 **像真人一样操作** — 真实浏览器内核，不越权、不root、不逆向抓包
- 🔄 **多AI串并联** — 豆包→通义→DeepSeek... 自动串联对话
- 🌐 **通用所有网页AI** — 选择器配置外置JSON，不改代码适配新站点
- ⚡ **高效率** — WebView 实例池 + 任务队列 + 并发调度
- 🔐 **安全合规** — 不走无障碍、不越权、仅供个人学习研究

---

## 🚀 当前状态

### ✅ 已完成
- 项目骨架搭建（Kotlin + Compose + Material3）
- 多语言资源（简中/繁中台湾/繁中香港/英文）— 42个字符串×6个目录
- 苹果水晶玻璃 UI（GlassCard 组件）
- 多标签页管理（WebView 多实例池）
- 收藏夹预置 7 个 AI 平台（豆包/元宝/通义/DeepSeek/Kimi/Google/GitHub）
- Cookie 持久化（PersistentCookieJar.kt）— 7个平台登录态不丢
- 桌面 UA 适配（骗过移动端限制）
- 字体缩放 + User-Agent 自定义设置
- JS 注入引擎（JsInjector.kt）— 支持豆包/元宝/通义/Kimi/DeepSeek
- 真人输入模拟（HumanLikeInput.kt）— 逐字打字（30~180ms 随机间隔）
- 任务队列调度（AutoChatTaskQueue.kt）— 多任务串行+回调
- 四级输入降级：Selection+TextEvent → InputEvent → execCommand → 直接赋值
- Shadow DOM 穿透查找
- Slate 编辑器适配（`[data-slate-editor]`）
- 完整点击事件链：mousedown → mouseup → click → form.submit
- MutationObserver 监听回复完成
- 关于页收藏管理（添加/编辑/删除/恢复默认）
- 底部收藏按钮 toggle（点击展开，再点击关闭）
- 多标签列表滚动（超出 500dp 自动滚动）
- 顶部标题栏优化（缩小按钮、减小字体、增加 padding）
- URL 栏点击编辑（点击文字直接进入编辑模式）
- **豆包专属适配** — 原型 setter 绕过 React 受控组件劫持 + Enter 优先 + 按钮兜底
- **元宝/豆包分支隔离** — `fillAndSend(tag)` 分发，JS 完全独立
- **selectors.json 配置外置** — 不改代码适配新站点

### 🛠️ 规划中
- [ ] **多AI流水线串联** — A的回复喂给B
- [ ] **网关代理支持** — WebView 走网关
- [ ] **对话历史保存** — 本地持久化
- [ ] **任务队列 UI** — 可视化队列状态
- [ ] **导出/导入配置** — 备份恢复
- [ ] **夜间模式优化** — 深色主题
- [ ] **性能优化** — WebView 池复用

---

## 🏗️ 项目结构

```
綦桐AI转站/
├── app/
│   ├── src/main/
│   │   ├── java/com/qtwl/YitongAIzhuanzhan/
│   │   │   ├── MainActivity.kt          # 主入口
│   │   │   ├── WebViewManager.kt        # 多实例池 + Cookie 持久化
│   │   │   ├── JsInjector.kt            # JS 注入引擎（豆包/元宝/通用）
│   │   │   ├── HumanLikeInput.kt        # 真人输入模拟
│   │   │   ├── AutoChatTaskQueue.kt     # 任务队列调度
│   │   │   ├── BookmarkManager.kt       # 收藏管理
│   │   │   ├── LocaleManager.kt         # 多语言管理
│   │   │   ├── PersistentCookieJar.kt   # Cookie 保存/恢复
│   │   │   └── ui/
│   │   │       ├── screens/             # 页面
│   │   │       │   ├── BrowserScreen.kt
│   │   │       │   ├── AboutScreen.kt
│   │   │       │   ├── SettingsScreen.kt
│   │   │       │   └── BookmarkEditScreen.kt
│   │   │       ├── components/          # 组件
│   │   │       │   └── GlassCard.kt
│   │   │       └── theme/               # 主题
│   │   ├── res/
│   │   │   ├── values/                  # 默认中文
│   │   │   ├── values-en/               # 英文
│   │   │   ├── values-zh/               # 中文通用
│   │   │   ├── values-zh-rCN/           # 简体
│   │   │   ├── values-zh-rTW/           # 繁体台湾
│   │   │   ├── values-zh-rHK/           # 繁体香港
│   │   │   └── ...                      # 图标/主题
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── app/src/main/assets/
│   ├── selectors.json                   # 平台选择器配置（热更新）
│   └── SlateFiller.js                   # 四级降级输入引擎
├── DEV_GUIDE.md                         # 开发指南（铁律必读）
├── CHANGELOG.md                         # 更新日志
├── qitong.jks                           # 签名证书（本地）
└── setup_android_env.sh                 # ARM64构建环境脚本
```

---

## 🛠️ 快速开始

### 环境要求
- JDK 17+
- Android SDK
- ARM64 Linux 环境（Operit 内置）

### 构建
```bash
# 1. 初始化环境（仅首次）
chmod +x ./setup_android_env.sh
./setup_android_env.sh

# 2. 编译
./gradlew assembleDebug

# 3. 安装
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/綦桐AI转站.apk
cp /sdcard/Download/綦桐AI转站.apk /data/local/tmp/app.apk
chmod 644 /data/local/tmp/app.apk
pm install -r /data/local/tmp/app.apk
```

---

## 📦 技术栈

| 组件 | 版本 |
|------|------|
| Kotlin | 2.3.10 |
| AGP | 9.0.0 |
| Compose BOM | 2026.01.01 |
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 24 |
| ABI | arm64-v8a |
| 签名 | qitong.jks |

---

## 📜 许可

本项目仅供个人学习研究，禁止商用、禁止对外服务。

---

> **当前版本：** v1.0.0-25 (versionCode=26)  
> **发布日期：** 2026-07-24  
> **状态：** 测试版（等待各大平台大佬更新后继续优化）