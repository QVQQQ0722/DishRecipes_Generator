# 独立菜谱导入模块

这是 Kotlin package / 源码文件夹，属于现有 app 模块，不是独立 APK 或已部署的后端。

目前真实实现了链接识别、流程编排、取消、结果校验和结果转换。内容获取、图片/视频处理、模型调用使用 `demo/` 中的固定演示实现。

## 从哪里开始读

1. `LinkParser.kt`：从分享文字中找到链接，识别小红书 / Bilibili / TikTok / Instagram。识别成功不代表帖子可获取。
2. `ImportModels.kt`：正文、图片、视频、关键帧、转写、菜谱结果的数据结构。
3. `ImportPorts.kt`：需要实现的三个能力接口。
4. `ImportRecipePipeline.kt`：按顺序调用接口，接收进度、处理取消，返回现有 Recipe。
5. `RecipeResultMapper.kt`：检查结果非空、份数有效、证据 ID 存在，再转换成详情页需要的数据。
6. `demo/DemoAdapters.kt`：用于学习和测试的固定实现；demo:// 只是占位引用，没有真实媒体文件。
7. `ImportFeature.kt`：组装依赖，是后续切换真实服务的入口。

## 如何调用

```kotlin
// 在协程中调用；默认不联网、不消耗模型费用。
val repository = ImportFeature.createDemoRepository()
val source = LinkParser.parse("https://www.bilibili.com/video/example")
val recipe = repository.analyze(source) { stage ->
    // 将 stage 显示到 UI，例如 mutableState.update { it.copy(progress = stage) }
}
// recipe.title / recipe.ingredients / recipe.steps 可以直接供现有详情页使用。
```

现有调用链：CreationDialog → RecipeViewModel.analyze → SourceParser（兼容入口）→ LinkParser → ImportRecipePipeline → RecipeResultMapper → RecipeViewModel.finish → DetailScreen。

## 如何接真实实现

三个接口可以单独替换，方便测试。但生产系统应在后端处理媒体和模型密钥，Android 通过新的 BackendRecipeRepository 调用自有后端。

后端内部流程对应为：平台适配器 → 媒体准备 → VLM 适配器 → JSON Schema / 业务校验。完成后将 RecipeViewModel 的 Repository 依赖切换为 BackendRecipeRepository；不用重写三个页面。

模型返回字段的有效性检查不能证明内容准确，还需要实际样例和人工评估。当前没有真实网络客户端、后端或 VLM 连接。

完整逐步计划见项目根目录 `docs/IMPORT_FLOW_GUIDE.md`。
