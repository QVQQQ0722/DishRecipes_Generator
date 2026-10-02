# 链接 → 多模态分析 → 菜谱：逐步开发指南

## 0. 先理解当前代码

- Compose：描述界面的 Android UI 工具。输入什么状态，就显示什么内容。
- RecipeUiState：当前页面、输入、加载进度、错误和菜谱列表的一张状态快照。
- RecipeViewModel：接收点击/输入，调用业务接口，再更新状态。
- RecipeRepository：获取菜谱的约定。它不是数据库本身，也不等于模型；当前实现是独立模块里的演示流水线。

现阶段调用关系：UI → ViewModel → Repository → 更新 UiState → UI 重新显示。

## 1. 在 Android Studio 找文件和看预览

MainActivity.kt 存在于 `app/src/main/java/com/example/recipeimport/MainActivity.kt`。Android Studio 的 Android 视图可能把它收在 kotlin+java 中；切换 Project 视图即可按真实目录展开。也可 Ctrl+Shift+N 搜索 MainActivity.kt。

MainActivity 只负责启动和分享接收，页面在 ui/RecipeApp.kt。已增加 ui/RecipePreviews.kt，内有三个 @Preview。Gradle 同步完成后打开它，选择 Split / Design；如果提示 Build & Refresh，执行后再查看。预览依赖已配置，仍需要在 IDE 中确认渲染。预览不是完整应用运行，交互以真机运行结果为准。

参考：[Android Studio 文件视图](https://developer.android.com/studio/intro)、[Compose Preview](https://developer.android.com/develop/ui/compose/tooling/previews)。

## 2. 先跑已接好的演示

手机中点击：增加菜谱 → 导入菜谱 → 粘贴支持的平台链接 → 生成演示菜谱。

依次看到：获取帖子 → 准备媒体 → VLM 分析 → 结果校验。前三步是模拟，最后一步真的检查结果；当前不会下载网页、图片或视频，也不会调用模型。

支持链接识别的平台：小红书（含 xhslink.com）、Bilibili（含 b23.tv）、TikTok（含 vm/vt 子域）、Instagram。其他网站需要增加明确的平台适配，当前不接受任意网站。

## 3. 确定第一版模型输入和输出

选一个模型服务，并查清：能否收图片、是否原生支持视频/音频、单次大小和时长限制、结构化输出格式、价格。不同 VLM 的接口不相同，不能假设把社交平台页面 URL 发过去就能读取全部内容。

先准备一份已知正文、1–3 张截图、一个短视频样例及人工写好的预期菜谱。

输出最低要求：title、servings（可未知）、ingredients（name / amount / evidenceIds）、steps（instruction / evidenceIds）、warnings。没有提供的数量返回 null；不是菜谱返回 NOT_A_RECIPE。

验收：能生成符合约定的数据，缺失值不乱填，非菜谱内容能被识别为失败。模型 API Key 只保存在后端环境变量，不放进 Android APK 或 Git。

## 4. 先把 VLM 接到自有后端

建议第一轮用用户提供的正文和截图，直接验证模型能力。后端接收内容，调用选择的 VLM，校验结果后返回 JSON。

Android 新增 BackendRecipeRepository，请求自有后端，转换结果为 Recipe。RecipeViewModel 通过构造参数接收它。真正切换后，导入界面的按钮/说明必须同步改为真实模式；当前不要删除演示标签。

验收：两份不同内容得到各自对应的结果；网络失败显示错误；取消后不跳到结果页。此时可以先不处理平台链接。

## 5. 实现平台内容获取

为每个平台分别实现适配器，输入链接，输出 ResolvedPost 对应的正文、图片引用和视频引用。按平台允许的接口和用户授权获取；短链接需展开，失效/登录/无权限需给明确错误，无法读取时引导粘贴正文或上传媒体。

这里需要实际平台接口/访问方式，VLM 不能替代内容获取。不要把用户登录 Cookie 或密码写进代码。

服务端需要校验目标域名、每次跳转和实际 IP，避免访问内网；给下载设置大小、超时及类型限制。客户端的 LinkParser 只是输入检查，不能代替这些服务端检查。

验收：选定一个平台和一种帖子类型，能够取得真实内容；其他类型明确提示不支持，而不是返回虚假的成功结果。

## 6. 加上完整的视频处理

文字：保留正文及证据 ID。
图片：读取真实图像，按模型要求缩放/编码；OCR 是否单独做取决于模型能力。
视频：如果模型支持原生视频，按它的 API 上传；否则提取带时间戳的关键帧。声音单独转写或使用明确支持音频的模型，再与正文/画面一起提交。

不能只看视频封面，也不能把所有 VLM 都当作支持音频。保留时间戳，避免步骤顺序错乱，处理字幕与正文的重复信息。

验收：能够解释每项食材/步骤来自哪段内容；只在声音里出现的数量也有机会提取；内容矛盾时提示用户确认。

## 7. 最后接完整用户流程

长视频改为后端异步任务：创建 → 查询进度 → 完成或失败 → 取消。API 草案见 ARCHITECTURE.md。手机上的协程取消并不自动取消服务器任务，需要专门的后端取消接口。

结果展示后让用户编辑确认，再写入本地数据库；目前 finish 仍只写内存。补充任务恢复、重试去重、媒体清理和模型费用限制。

## 推荐本轮与下轮分工

本轮已完成：独立 importrecipe 包、四阶段编排、混合媒体数据结构、接口、演示实现、结果校验、已有按钮接入、四平台链接识别、Compose 预览入口。

下一步需要确定：模型服务名称、先支持的平台，以及内容获取方式。先实现“正文 + 图片 → 真实模型 → 菜谱”，再接视频和平台获取。这样能分别判断是取内容失败，还是模型分析不准确。

不必同时实现三家平台、所有媒体形式和完整后台系统。每一阶段都用固定样例和验收标准验证后，再扩大范围。

## 本轮验证

22 个单元测试通过；3 个 Android 16 真机界面测试通过（包含 Bilibili 链接进入独立演示流程）；assembleDebug 和 lintDebug 通过。Compose 预览代码已编译，Android Studio 编辑器内的预览渲染需打开 RecipePreviews.kt 查看。
