# 拾味 · Android 菜谱导入原型

项目共识与新对话入口：[PROJECT.md](PROJECT.md)。质量验收：[25 条手工 eval](docs/EVALS.md)。能力或决策变化时同步更新，未完成项明确标记。

将小红书 / Bilibili / TikTok / Instagram 分享链接整理成食材与做法的 Android 工程框架。当前支持链接识别，内容获取与 VLM 分析仍为演示。

**当前是演示原型**：按 Figma 实现主菜单、添加方式、菜谱详情。支持三个创建入口、会话内手动添加、模拟导入和模拟 AI 生成；没有真实抓取、AI 分析、媒体上传或持久化。

## 开发 Prompt 与待办
- [交付里程碑](docs/MILESTONES.md)：V1 截止 2026-10-31 的周目标、状态与验收；V2/V3 阶段目标。
- [Python 导入服务与逐步运行说明](backend/README.md)：独立三个接口、固定示例、可替换模型与 HTTP 入口；当前默认演示，尚未接 Android。
- [完整开发 Prompt](docs/DEVELOPMENT_PROMPT.md)：可复制给后续开发助手。
- [TODO](docs/TODO.md)：按 P0–P3 分阶段推进。
- [分层架构与实施路线](docs/ARCHITECTURE.md)：分层图、职责与接口边界、V1–V3 和 A–G 分段验收。

## 本地运行
1. 安装 Android Studio，准备 JDK 17、Android SDK Platform 35 和 SDK Build Tools。
2. 用 Android Studio 打开此根目录。为工程配置 SDK 路径（通过 IDE 或被 git 忽略的 local.properties）。
3. 本仓库已包含 Gradle Wrapper（8.11.1，并配置官方 SHA-256 校验）。在根目录运行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

4. 在 Android Studio 选择 Android 8.0 / API 26 或更高版本设备，运行 app。
5. 点击任意菜谱查看详情；点击「增加菜谱」选择导入、自己输入或 AI 生成。导入弹窗内可点击「填入示例链接」体验模拟分析，也可从其他 App 分享链接至「拾味」。手动添加的菜谱只保留到本次应用进程结束。

依赖固定为 AGP 8.9.2 / Gradle 8.11.1 / Kotlin 2.1.20 / Compose BOM 2025.04.01，采用固定版本作为原型起点。AGP 8.9 的 Gradle/JDK 要求见 [Android 官方兼容说明](https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes)；Compose 编译插件使用与 Kotlin 相同的版本，见 [官方配置说明](https://developer.android.com/develop/ui/compose/compiler)。

## 目录
```text
app/src/main/java/com/example/recipeimport/
  MainActivity.kt          系统窗口与分享接收
  RecipeViewModel.kt       导航、导入状态、创建与取消
  RecipeRepository.kt      分析接口与演示实现
  Recipe.kt                数据模型与 URL 校验
  DemoRecipes.kt           示例菜谱与手动输入解析
  importrecipe/            独立导入流程、混合媒体模型、可替换接口与演示实现
  ui/                      页面、主题、卡片/食材/步骤组件
app/src/main/res/          Figma 原图、矢量图标、Inter 字体
app/src/test/              链接解析、导航与创建流程测试
app/src/androidTest/       真机导航、表单与截图测试
docs/                     Prompt、架构、TODO
```

本机环境配置和验证结果见 [本地开发环境](docs/LOCAL_SETUP.md)。

设计映射和适配说明见 [Figma 实现记录](docs/FIGMA_IMPLEMENTATION.md)。连接安卓设备后可运行 `.\gradlew.bat connectedDebugAndroidTest`；静态检查运行 `.\gradlew.bat lintDebug`。

初学者可先看 [导入流程逐步指南](docs/IMPORT_FLOW_GUIDE.md)，再看 [独立模块说明](app/src/main/java/com/example/recipeimport/importrecipe/README.md)。Android Studio 界面预览入口是 `RecipePreviews.kt`，不是 MainActivity。
