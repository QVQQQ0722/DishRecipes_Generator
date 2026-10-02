# W1.1 · 输入分类与菜谱结果契约

更新：2026-09-28。**产品规则已收集，数据契约为待审阅草案；未修改运行代码。** 当前 Python schemas.py 仍是旧演示结构。依据：[三个用户样例](RECIPE_EXAMPLES.md)。

## 已确定的产品规则

- 按需提取或补全食材、用量和步骤；补全必须标为“预估”，不能冒充来源事实。
- 菜谱必须显示名称、预计烹饪时间、份数、分组食材与数量、逐步操作及该步使用的食材/数量。
- **输出语言：英文**，包含菜谱正文、预估说明及AI推荐理由（Estimated / AI recommendation）；输入可以是其他语言，内部原始证据保留原文。项目文档可继续中文，既有App界面整体翻译不属于本次修改。
- 用户界面统一 **US customary units + °F**，不同时附加公制；分钟/小时正常使用。个、瓣、枝等计数保留。
- 冲突必须展示各来源及差异，再给出有理由的 **AI推荐**；不能静默选择或取平均。
- V1 生成菜谱所需材料；V3 再做库存材料、健身/素食等用户偏好适配。“Fit”等来源标题不等于已实现营养个性化。

## 1. 输入分类与信息获取

先获取可用内容，再判完整性；保留“仅输入内容”和“补充来源后”的两次判断。分类不要求描述和字幕同时存在。

| 场景 | 分类与行为 |
| --- | --- |
| 描述/字幕等已给出食材与步骤 | extractable：以原文为主；个别份数/时间未知可单独补全并标记 |
| 视频缺信息，描述给出完整菜谱 | 合并描述后可为 extractable，不因视频无字幕就判不完整 |
| 描述指向作者个人网站的菜谱 | 先 incomplete；尝试读取明确关联的菜谱页，合并证据后重新分类；获取失败不能声称已读 |
| 纯音乐烹饪视频，没有食材/步骤文本 | incomplete：利用画面、菜名和菜谱知识生成参考结果，推断字段标预估 |
| 非烹饪、与菜谱无关 | irrelevant：返回“ERROR: 请输入菜谱”，不生成菜谱 |
| 空输入、损坏文件、链接无法访问 | 返回输入/访问错误；不能误判成 irrelevant |

个人网站获取属于 PostExtractor 的扩展，优先明确关联的菜谱页；不自动无限爬取个人网站或绕过权限。链接提取为 V1/P2，分类规则同样适用于 P0 文字+图片。

**关键区别**：整体 incomplete 不代表每个字段都要生成；已知食材照实提取，未知用量才补全。纯画面能支持可见操作，不一定能支持精确克数、时间或温度。

## 2. 建议的输入与响应外壳

| 结构 | 字段 / 含义 |
| --- | --- |
| ImportRequest | text、asset_ids、url 按输入方式提供；output_language 固定 en，output_units 固定 us_customary，temperature_unit 固定 F；客户端不提供服务器文件路径 |
| ImportResponse | schema_version、run_id、status、classification、sources、recipe、conflicts、warnings、error |
| status | succeeded / needs_review / rejected / failed；冲突未确认或关键资料不足时 needs_review；这是结果状态，不是后台任务运行状态 |
| classification | initial_type、final_type（extractable / incomplete / irrelevant）、reason、missing_fields；无法读取时允许 null |
| sources | id、kind（description / subtitle / video_frame / author_recipe_page / knowledge_reference 等）、URL或素材引用、获取状态、片段/时间戳；模型知识需记录模型/提示词版本，不能伪造网页引用 |
| error | code、message；失败不返回伪成功菜谱；非相关输入 recipe=null |

请求暂不包含用户库存、饮食类型、健身目标等个性化字段。知识方案已确定为知识库优先，缺失时外部API补充并更新；具体来源/API、入库校验和作者网站抓取策略在W1.3确定，均未实现。

## 3. 字段来源与单位

对名称、数量、份数、时间和操作等分别记录来源；不能只给整道菜标一个来源。

| 字段 | 含义 |
| --- | --- |
| value | 规范化后的值；未知为 null |
| origin | extracted / estimated / unknown |
| evidence_ids | 支持提取结论的原始证据；未知/预估不得伪造此引用 |
| basis | 预估依据、知识引用或计算说明；用到的原始线索与知识来源分开记录 |
| transformation | 单位换算、份数运算、显示舍入；确定性换算不自动变为 AI 预估 |
| conflict_id | 若该字段有冲突，关联完整候选项与AI推荐 |

单位规则：
- 重量用 oz/lb，体积用 tsp/tbsp/fl oz/cup/pt/qt/gal，长度用 in；oz 与 fl oz 不混用。
- 内部数量用数值或范围，界面可显示 1/2、1 1/2、6–8；可选/适量单独标记，不假造精确数字。
- g→oz 是重量换算；g→cup 需要食材密度，不能直接换。不知道密度则保留重量制美制单位。
- 公制原文仅保存在内部证据；界面规范化为美制，保留换算/舍入说明。温度以 F 为唯一展示单位。
- 示例里约等号两边不一定是精确换算，不把排版样例当成换算标准；换算与AI推荐规则分开。

## 4. 菜谱和步骤结构

| Recipe字段 | 内容与约束 |
| --- | --- |
| title | 菜谱名称及来源 |
| times | prep、cook、rest、total（分钟数或范围），注明预估与前提；允许附 bake/marinate 标签 |
| servings | min、max、说明（如 as a side）；范围不能被静默缩成一个数 |
| ingredient_groups | 组件分组，如 chicken / salad / bowl_base |
| ingredients | 稳定 id、group_id、名称、整份菜谱数量、单位、可选状态、处理方式；名称与数量分别标来源 |
| steps | 连续 order、标题、instruction、ingredients_used、duration、temperature_f、outputs |
| equipment / notes | 由三个例子提出的可选内容；不替代必需食材和步骤 |

**步骤使用量**：ingredients_used 区分三种引用：ingredient_id 原材料、来自前一步的 output_id 半成品、已有材料的处理/复用。记录数量、单位和基准（整道菜 / 每份 / 全部半成品），以及是否首次投入。

- 备料切鸡肉 → 调味 → 煎制是同一批鸡肉，不算三份原材料。
- 高汤 6 cup 可分为 1/2 cup + 5 1/2 cup；总量必须对齐，按调味/炖煮分组的同名食材不能错配。
- “步骤2的奶油混合液”“一半鸡肉”引用半成品，不重复添加到采购清单。未知半成品体积可显示“全部/一半”，不编造杯数。
- “每碗1/2 cup米饭”需要明确两碗共1 cup；装盘引用不能再次消耗已经使用的原材料。
- 无新增食材的烘烤/静置步骤允许 ingredients_used 为空，并显示“无新增食材”。
- 份数是6–8时，食材量对应整份菜谱；不能在未选基准时自动按单人份换算。
- 总时间考虑并行操作和前提。例如“使用已煮熟米饭”不包含煮饭耗时；炖肉同时做土豆泥不重复相加。

## 5. 冲突与 AI 推荐

冲突指**同一道菜、同一份数基准、同一食材用途或操作阶段存在不兼容的明确说法**。以下类型及处理边界为细化建议，待审阅：

| 类型 | 假设示例 | 判定前要排除 |
| --- | --- | --- |
| 食材身份/明确要求 | 描述写heavy cream，字幕明确写milk；或明确禁止某食材却在另一处要求加入 | “milk or cream”是允许替代；某处未提及不等于明确否定 |
| 食材数量 | 同样2份，描述盐1 tsp，字幕2 tsp | 不同份数、不同用途、总量和分次用量 |
| 方法/步骤顺序 | 同一阶段要求加盖，另一来源明确要求不加盖 | 先加盖后揭盖是两阶段，不冲突 |
| 温度/时长 | 同一烘烤阶段，一处350°F/30 min，另一处425°F/15 min | 烤箱温度与食物内部温度不同；不同设备/阶段可能是不同条件 |
| 份数/计量基准 | 同一整份配方明确标serves 2与serves 4 | 主菜与配菜份数、每份量与整份量可能可解释 |
| 来源内部不一致 | 配料表蜂蜜共2 tbsp，但步骤首次投入合计3 tbsp | 重复引用/半成品复用不重复消耗；若仅模型生成结果不一致，应校验修复而非伪装成来源冲突 |

缺失信息是补全问题；模糊画面是识别不确定性；已声明的可选版本、等价换算不是冲突。AI常识与明确原文不同本身不能推翻原文；可提出单独的建议并说明依据。不要让用户为这些非冲突逐项确认。

建议结构：conflicts[] 包含 id、field_path、options[]（各自值和来源）、recommended_option_id、recommendation_reason、user_selected_option_id。

- 先统一单位排除等价表达，再判断事实冲突；合理的单位舍入不是自动新增冲突。
- AI依据来源明确程度、与具体菜谱的匹配和上下文一致性推荐，不把推荐说成已证实正确。
- 用户看到全部候选值、各来源、差异和“AI推荐＋理由”；user_selected_option_id 初始为 null。
- **已认可交互，待实现**：未确认时显示推荐草稿并标 needs_review；存在影响步骤用量的冲突时，明确整份草稿采用哪个推荐量，用户确认后才作为确认版保存。
- 推荐不覆盖原候选项；证据不足时允许无推荐并解释原因；绝不取平均解决冲突。

## 6. W1.1 验收与剩余决策

字段响应片段（仅说明拟定格式，不是已实现API，也不是上述视频的分析结果）：

```json
{
  "name": {
    "value": "Chicken breast",
    "origin": "extracted",
    "evidence_ids": ["frame-1"]
  },
  "quantity": {
    "value": {"min": 8, "max": 8, "unit": "oz"},
    "origin": "estimated",
    "evidence_ids": [],
    "basis": "Estimated for 2 servings; the implementation must record the supporting knowledge reference."
  }
}
```

冲突响应片段（假设描述写1 tsp盐，字幕写2 tsp；不涉及真实视频）：

```json
{
  "status": "needs_review",
  "conflicts": [{
    "id": "conflict-1",
    "field_path": "ingredients.salt.quantity",
    "options": [
      {"id": "a", "value": 1, "unit": "tsp", "source_id": "description-1"},
      {"id": "b", "value": 2, "unit": "tsp", "source_id": "subtitle-1"}
    ],
    "recommended_option_id": "a",
    "recommendation_reason": "Illustrative: the description explicitly refers to this batch; the subtitle context is less clear. Please confirm.",
    "user_selected_option_id": null
  }]
}
```

三类响应行为样例：完整正文→extractable并返回来源可核对菜谱；纯画面烹饪→incomplete并返回显式预估草稿；风景视频→irrelevant、rejected、recipe=null。完全读不到视频应failed，不能假定它属于任何一类。

- 已完成：收集三类视频情况、生成范围、冲突行为、必需输出、美制/°F和V3个性化边界；整理本草案及三个样例的结构要求。
- 待审阅：字段命名/来源包装与完整响应样例、Android展示映射；冲突类型/确认保存及英文已认可，equipment/notes按可选。
- 待 W1.3：知识库外部提供方、入库规则、关联作者网站的发现方式与抓取范围。关键菜品完全无法识别时，建议请求补充而不强行生成。
- W1.1 DONE条件：双方认可分类及契约；给出三类输入、预估、冲突的可审阅响应样例；Android确认可展示。当前仅到 WIP，不代表模型或接口实现已完成。
