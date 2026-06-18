# AGENT.md - CBC-Stratgems 仓库工作指引

## 仓库定位

CBC-Stratgems 是一个代码实现仓库，目标是在 Minecraft 中实现类似 HELLDIVERS 2 的战略配备呼叫系统，并基于 CreateBigCannons (CBC) 完成火力打击。

当前仓库的第一阶段不是直接写模组代码，而是先完成 CBC 源码调研和技术设计。任何实现工作都必须建立在调研结论之上。

## 阶段门槛

按以下顺序推进，不要跳过前置阶段：

1. **玩法设想确认**
   - 阅读 `docs/stratagem-concept.md`。
   - 不扩展未写明的玩法，不提前加入未来扩展功能。

2. **CBC 源码调研**
   - 使用 `prompts/01-cbc-source-research.md`。
   - 产出 `research/cbc-source-map.md`。
   - 每个结论必须附 CBC 源码位置：文件路径、类名、方法或字段名、行号范围。

3. **技术设计文档**
   - 使用 `prompts/02-design-document.md`。
   - 产出仓库根目录的 `design.md`。
   - `design.md` 必须区分“源码证据”“设计决策”“开放问题”。

4. **实现计划**
   - 使用 `prompts/03-implementation-plan.md`。
   - 产出 `docs/implementation-plan.md`。
   - 计划必须明确模块边界、数据格式、注册项、兼容性策略和验证方式。

5. **代码实现**
   - 使用 `prompts/04-implementation.md`。
   - 只有在 `design.md` 和 `docs/implementation-plan.md` 存在且关键开放问题不阻塞核心功能时，才开始写代码。

## 硬性规则

- 优先目标版本：NeoForge 1.21.1。
- Forge 1.20.1 只作为后续移植目标，不在第一轮实现中混入兼容层。
- 不要直接假设 CBC 的内部实现可用；必须先通过源码定位和验证。
- 如果 CBC 没有直接实现某项能力，明确写“未找到”，再提出替代方案。
- 对 CBC 附属模组的兼容性优先级高于快速调用内部方法。
- 不要修改或复制 CBC 源码，除非后续明确决定以依赖、接口或适配层方式处理。
- 不要实现 `docs/stratagem-concept.md` 中标记为未来扩展的非火力打击功能。

## 关键输入

- 玩法设想：`docs/stratagem-concept.md`
- CBC 上游仓库：https://github.com/Cannoneers-of-Create/CreateBigCannons
- 研究提示：`prompts/01-cbc-source-research.md`
- 设计提示：`prompts/02-design-document.md`
- 实现计划提示：`prompts/03-implementation-plan.md`
- 实现提示：`prompts/04-implementation.md`

