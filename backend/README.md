# Python 菜谱导入服务

Android 负责输入、进度和菜谱展示；这个独立 Python 项目负责内容获取、媒体整理和模型分析。

当前阶段：固定例子 → 三个可替换接口 → 校验 → 菜谱 JSON。默认 `demo` 不调用模型，不需要 API Key。Android 目前仍运行原有演示流程，尚未连接这里。

## 1. 在本机运行第一个例子

在 PowerShell 逐行执行。当前电脑的虚拟环境和依赖已经安装；无需激活虚拟环境。

```powershell
cd 'C:\Users\QVQQQ\Documents\ChatGPT\菜谱导入小app\backend'
.\.venv\Scripts\python.exe -m app.run_example --provider demo
```

换到另一台电脑，先安装 Python 3.12，再在 backend 内执行：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-lock.txt
```

`requirements.txt` 表达依赖范围；`requirements-lock.txt` 固定本次验证版本。两个人开发时优先用 lock 文件安装。

输入在 `examples/tomato_eggs.json`。输出是番茄炒蛋、四种食材和三个步骤；没有说明的油盐用量和份数保持 null。

每次运行会创建独立的 `outputs/时间-随机ID/`：

| 文件 | 用来理解什么 |
| --- | --- |
| `01_post.json` | 内容获取接口获得了什么 |
| `02_analysis_input.json` | 真正交给分析器的文字和图片字节 |
| `03_model_raw.txt` | 分析器原始返回，错误结果也能查看 |
| `04_result.json` | 校验通过后，准备给 Android 的结果 |

`is_demo: true` 表示固定模型输出；`is_fixture: true` 表示输入来自本地例子。以后即使真实模型分析本地例子，后者仍然为 true。演示模型读取 `examples/demo_response.json`，修改输入不会让它自动生成不同菜谱。

## 2. 逐步阅读代码

```text
app/run_example.py       命令行入口与阶段记录
app/pipeline.py          extract → prepare → analyze → validate
app/ports.py             三个 Python Protocol 接口
app/schemas.py           各阶段数据结构与输出校验
app/extractors/fixture.py 读取固定帖子，拒绝真实链接
app/media/local.py       正文、图片、已抽取视频帧、转写文本的整理
app/models/demo.py       固定演示模型
app/models/ollama.py     可选真实 HTTP 适配器，尚未实测模型推理
app/models/factory.py    读取配置，选择模型实现
app/main.py              给未来 Android 调用的 HTTP 入口
prompts/recipe.txt       真实适配器使用的提取指令
```

Protocol 是一种约定：只要对象具有相同的方法，就能放进流程。不需要每次更换模型都重写流程。

最核心的调用只有三步：

```python
post = await extractor.extract(source)
content = await preparer.prepare(post)
raw_json = await analyzer.analyze(content)
```

流程随后校验 JSON 结构、必要字段和证据 ID。不代表已经证明模型理解正确；真实测试还需要对照原材料检查遗漏和数量。

## 3. 以后怎么换模型

默认使用 demo。复制配置模板（已有 .env 时直接编辑，不要覆盖）：

```powershell
Copy-Item .env.example .env
```

设置 `RECIPE_MODEL_PROVIDER`，然后运行不带 `--provider` 的命令，便会读取 .env。命令行参数优先于系统环境变量，系统环境变量优先于 .env；修改配置后重启 HTTP 服务。

```powershell
.\.venv\Scripts\python.exe -m app.run_example
```

目前可用实现名称只有 `demo` 和 `ollama`。后者是一个可选替换示范，并未安装 Ollama 或下载模型。如果以后选择它，需要先安装、启动本地 Ollama，准备支持图片的模型，然后配置：

```dotenv
RECIPE_MODEL_PROVIDER=ollama
RECIPE_MODEL_NAME=填写你实际安装的视觉模型名称
RECIPE_MODEL_BASE_URL=http://127.0.0.1:11434
RECIPE_MODEL_TIMEOUT_SECONDS=120
```

这个适配器按 [Ollama Chat API](https://docs.ollama.com/api/chat)、[图片输入](https://docs.ollama.com/capabilities/vision) 和 [结构化输出](https://docs.ollama.com/capabilities/structured-outputs) 编写。它提交实际图片 Base64 和文字，不仅是帖子链接；结构化输出面向本地 Ollama，云服务支持情况不同。

如果选择其他供应商：

1. 在 `app/models/` 增加适配器，实现 `analyze(content) -> str`，返回符合 `ExtractedRecipe` 的 JSON 字符串。
2. 定义 `provider`、`model`、`is_demo=False`。
3. 在 `factory.py` 注册供应商名称，读取对应配置。需要的 Key 只读取本地 .env/环境变量。
4. 把供应商的特殊输入格式和响应转换留在适配器内。
5. 用固定材料检查真实结果。模型失败会返回错误，不会自动伪装成 demo 成功。

首次接入新供应商需要写代码；接入后，同供应商的兼容模型通常只需改模型名称。支持的媒体类型仍需核对。

## 4. 加入自己的图片、视频材料

第一份示例只有正文，所以当前演示不是视觉推理测试。可把自己的图片放在 examples 下，在 `tomato_eggs.json` 设置：

```json
"images": [
  {"id": "image-1", "path": "ingredients.jpg", "mime_type": "image/jpeg"}
]
```

已经抽好的视频帧可以放在 `video_frames`：

```json
"video_frames": [
  {"id": "frame-1", "path": "step1.jpg", "mime_type": "image/jpeg", "timestamp_seconds": 12.0}
],
"transcript": "这里填写真实的口述转写"
```

ID 不可重复。支持 PNG/JPEG/WebP，每张不超过 5 MB，路径必须位于 examples 内。`videos` 为将来预留，当前非空会明确报 `VIDEO_NOT_IMPLEMENTED`。自动抽帧、音频转写和网站下载都还没实现。

## 5. 启动 Python HTTP 服务

```powershell
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

保持终端打开。浏览器访问 http://127.0.0.1:8000/docs，展开 `POST /v1/imports/example`，点 Try it out → Execute。

请求体：

```json
{"example_id":"tomato_eggs"}
```

也可以在另一个 PowerShell 调用：

```powershell
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8000/v1/imports/example' -ContentType 'application/json' -Body '{"example_id":"tomato_eggs"}' | ConvertTo-Json -Depth 10
```

`GET /health` 仅说明 Python 服务活着，不验证模型是否可用。HTTP 请求不保存阶段文件；命令行才保存。字段使用 snake_case，未来由 Android 的数据转换层映射到现有 Kotlin 模型。

当前服务只用于本机开发，默认绑定回环地址。尚无认证、限流或部署配置。API 不接受任意路径、网页 URL 或模型地址。

## 6. 测试与下一步

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

已覆盖演示流程、缺失数量、错误结构、非菜谱、虚构证据、混合媒体、路径越界、视频未实现、HTTP 请求、模型配置和适配器请求/失败处理。适配器测试使用模拟 HTTP，不是实际推理。

本机验证：Python 3.12.10，14 项测试通过，pip check 通过；命令行示例成功输出菜谱。实际启动 Uvicorn 后，GET /health 与 POST /v1/imports/example 均成功，检查后已停止服务。尚未执行真实模型推理或 Android 联调。

- [x] 独立 Python 服务、三个接口、固定示例和输出记录。
- [x] 可配置模型工厂与可选 Ollama 适配器。
- [ ] 选择 VLM，运行同一固定例子的真实推理，检查输出质量。
- [ ] 使用真实图片/视频帧验证视觉理解。
- [ ] Android 接 HTTP、映射菜谱和错误、展示模型/示例标记。
- [ ] USB 真机通过 adb reverse 联调，再验证网络失败和超时。
- [ ] 选择一个网站实现真实内容获取，再加入视频抽帧和转写。
- [ ] 部署、认证、限流、任务状态、持久化和真实场景测试。
