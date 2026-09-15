# 框架与后续接口

当前代码路径：MainActivity → RecipeViewModel → RecipeRepository → DemoRecipeRepository。
Recipe.kt 定义来源、食材、步骤、菜谱模型与链接校验。当前没有网络请求、媒体上传或持久化，退出进程后结果不保留。

目标链路：Android 导入 → 自有后端创建任务 → 内容适配器 → 文字/OCR/ASR/视频关键帧 → 证据融合 → 模型结构化提取 → Schema 校验 → 用户确认 → 本地保存。

## API 草案（尚未实现）

- `POST /v1/imports`：提交 `{ "url": "...", "clientRequestId": "UUID" }`，或用户授权上传的 `assetIds` / `text`。返回 HTTP 202 与 `{ "jobId": "...", "status": "queued" }`。clientRequestId 用于幂等。
- `GET /v1/imports/{id}`：返回 status、stage、progress、recipe 或 error。status 为 queued / running / succeeded / failed / cancelled。
- `DELETE /v1/imports/{id}`：取消任务；服务端验证归属并停止后续处理。
- `POST /v1/assets`：受限媒体上传，返回 assetId；后续加入过期和删除策略。

失败码：UNSUPPORTED_SOURCE、CONTENT_UNAVAILABLE、AUTH_REQUIRED、NOT_A_RECIPE、MEDIA_TOO_LARGE、RATE_LIMITED、ANALYSIS_FAILED。获取失败时 UI 提供粘贴正文或手动上传入口。

## 目标结果示例（比当前 Android 演示模型更完整）

```json
{
  "schemaVersion": "1.0",
  "title": "番茄炒蛋",
  "servings": null,
  "source": { "platform": "xiaohongshu", "url": "https://www.xiaohongshu.com/explore/example" },
  "ingredients": [
    { "name": "番茄", "quantity": 2, "unit": "个", "inferred": false, "confidence": 0.95, "evidenceIds": ["text-1"] }
  ],
  "steps": [
    { "order": 1, "instruction": "番茄切块", "durationSeconds": null, "temperatureCelsius": null, "inferred": false, "evidenceIds": ["frame-1"] }
  ],
  "evidence": [
    { "id": "text-1", "type": "text", "text": "番茄两个", "timestampSeconds": null },
    { "id": "frame-1", "type": "video_frame", "text": "画面显示番茄切块", "timestampSeconds": 12 }
  ],
  "warnings": ["份数、时间和温度未提供，待确认"]
}
```

后端实施时补充严格 JSON Schema（必需字段、nullable、枚举、范围、长度及 additionalProperties），并校验 evidenceIds 引用与步骤顺序。模型返回不等于校验通过。
