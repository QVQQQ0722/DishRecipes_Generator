# Figma 实现记录

设计文件：`YfvHnaYgyZCpCcJjM4DCkQ`。实现前已读取三个节点的设计上下文和截图。

| Figma 节点 | Compose 实现 |
| --- | --- |
| 2:340 主菜单 | RecipeApp.kt / MenuScreen |
| 2:383 添加方式 | RecipeApp.kt / AddMethodsScreen |
| 2:421 菜谱详情 | RecipeApp.kt / DetailScreen |

沿用 Kotlin、Compose、ViewModel/StateFlow、协程和 Repository，不引入 Web 页面或第二套 UI 框架。

## 视觉与资源

- 背景 `#F6F4EE`，卡片 `#FFFEFA`，正文 `#243127`，次要文字 `#758077`，绿色 `#2F6B4F`，边框 `#E3E5DE`。
- 页面横向留白 22dp；标题 30sp / 34.5sp 行高；分区标题 22sp；正文 14sp。
- 菜谱卡片圆角 16dp、最小高 112dp、照片 96dp；添加方式卡片圆角 24dp、最小高 132dp。
- 食材行圆角 10dp、最小高 54dp；食材图标底圆 34dp；步骤序号 28dp。
- 图片下载自 Figma 导出的资产，存于 res/drawable-nodpi。SVG 原件保留在 assets/figma，使用 scripts/convert_figma_icons.py 原样转换路径到 VectorDrawable，不手绘替代图标。
- Inter 字体随 APK 打包；中文使用 Android 系统字体回退，因此不同厂商的中文字形可能略有不同。字体许可见 assets/figma/Inter-OFL.txt。
- 下载来源记录于 figma-assets.json；运行时不访问会过期的 Figma 资源 URL。

## Android 适配

- 使用真实系统状态栏，不绘制设计图的假时间、信号和电量；顶部保留至少 44dp，遇到更大刘海安全区域则增大。
- 使用系统安全区域、键盘避让、可滚动内容和自适应行高；大屏内容最大宽度 600dp。
- 外围 30px 圆角是设计设备框的表现，应用不人为裁剪整个手机窗口。Android 阴影由原生绘制，与 Figma 的 CSS 阴影可能存在细微差异。
- 详情页和添加方式的界面返回键、系统返回键都返回主菜单；表单关闭先回到添加方式。

## 模拟功能边界

- 初始三张卡片各有独立菜谱数据；第一张内容匹配详情节点。
- 导入入口沿用原有模拟 Repository，保留 URL 校验、进度、取消和警告。
- 手动输入支持名称、逐行食材/数量和逐行步骤，添加后进入详情并出现在主菜单；数据只保留在内存。
- AI 入口接收偏好输入，但明确提示返回固定演示菜谱。
- Saved 页筛选示例数据中 isSaved 的菜谱，尚未实现持久收藏管理。
- 照片/文件识别、真实 AI、结果编辑及持久化留待 API 阶段；三个入口表单是设计范围之外的功能补充。

## 验证

2026-09-14 运行 `testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest`：13 个单元测试、2 个真机测试通过，构建与 Lint 通过。设备为 PLK110 / Android 16。真机测试覆盖菜单/详情/添加方式导航、详情滚动、手动创建与缺失数量显示。

三个页面的真机截图位于 `artifacts/figma/`（不提交 Git），已人工核对布局、图标、图片和安全区域。可使用 `python scripts/capture_device_screens.py` 在已安装且打开主菜单的连接设备上重新捕获；脚本最后回到主菜单。

仍存在依赖更新提示、原始复杂矢量路径和工程原有应用图标/备份配置的非阻塞 Lint 警告，没有 Lint 错误。不为本次 UI 改动升级现有技术栈。
