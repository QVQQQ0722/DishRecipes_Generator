# Agents API 接入评估

2026-10-02。结论：优先做 Python 最小验证，有条件可行；未调用真实 API，未验证账号权限、图片路径或模型质量。用户选择从此方案开始，尚未替换现有 demo。

## 官方能力与项目适配

- [Quickstart](https://developers.openai.com/api/docs/guides/agents-api/quickstart)：Python SDK 的 beta.agents.sessions；需要 API key 与相应权限。这里是 Agents API，不是 Agent Builder，也不是把当前 Codex 对话直接作为服务。
- [Architecture](https://developers.openai.com/api/docs/guides/agents-api/architecture)：OpenAI 托管 Agent 的模型/工具循环；Python 应用执行自定义函数。建议从 environment.type=none 开始，不需要运行命令的沙箱。
- [Functions](https://developers.openai.com/api/docs/guides/agents-api/tools/functions)：Python 处理 required_actions，执行工具后按 turn_id/call_id 回传 tool_result。
- [Sessions](https://developers.openai.com/api/docs/guides/agents-api/sessions)：会话与事件需要单独适配，不能只替换原来的模型 HTTP URL。
- 待验证：所选模型的图片输入形式/限制、最终菜谱的结构化输出配置、取消行为、账号可用模型与实际费用。文本示例成功不代表 P0 文字+图片验收通过；其他 API 的图片/JSON 参数不能直接照搬。

## 目标职责（待实现）

Android → Python 接收/整理素材 → Agents API 分析并请求工具 → Python 检索知识/补充外部来源 → Agent 生成候选菜谱 → Python 校验 → Android 核对并本地保存。

- 保留 PostExtractor、MediaPreparer、VlmRecipeAnalyzer 边界。新增 Agents API 适配器，内部委托 session runner；供应商事件不泄漏到 Android。
- Python 控制权限、总超时、工具次数、日志和入库；Agent 决定何时请求可用工具。每次导入独立会话，关联 run_id/session_id/turn_id/call_id；断流不当作取消成功。
- 最少两个工具：search_knowledge、fetch_external_reference（建议名）。外部资料先记录出处、去重、核对冲突，再进入可用知识库；不让模型直接把生成内容写成可信事实。
- 现有 schemas.py 尚缺时间、来源标记、冲突、逐步用量等字段，须同步 W1.1 契约。JSON 格式正确不等于菜谱准确。
- 视频仍需抽帧/转写；TikTok/Bilibili 仍需获取适配器，不能因有 Agent 就视为支持。
- 手机本地保存不代表素材不上传：选中素材会经 Python 提交供应商。会话/素材保留与删除政策须核对；不上传未选择的设备数据。
- 模型名可配置，但仅限该 API 支持且账号有权限的模型；跨供应商仍需新适配器。

## 验证顺序与通过标准（建议）

1. 账号与文本：固定文本产生可校验菜谱；记录 SDK/模型/提示词版本，真实调用不回退 demo 冒充成功。
2. 图片：用只在图片中出现用量的固定样本，核对识别结果和证据；覆盖模糊图片，不能靠附带正文猜中。
3. 知识工具：覆盖命中、缺失后外部补充、来源冲突、获取失败；保留来源，重复事件不重复入库。具体网站 API 与入库门槛仍待定。
4. 契约与可靠性：英文、美制、°F、提取/预估区分；坏 JSON、超时、取消、迟到响应、会话隔离均正确处理，再接 Android。

先复用人工 eval 草案评测准确性，成本/延迟记录即可。若图片或契约验证阻塞，优先保留现有接口并评估直接 VLM API 适配器作为备选，不为托管 Agent 重写整个项目。
