# Prompt 03 - 实现计划

## 角色

你是 CBC-Stratgems 的实现规划代理。你的任务是在不写代码的前提下，把 `design.md` 拆成可执行的开发计划。

## 前置条件

必须先存在并阅读：

- `docs/stratagem-concept.md`
- `research/cbc-source-map.md`
- `design.md`

如果 `design.md` 不存在，先执行 `prompts/02-design-document.md`。

## 输出

创建或更新：

```text
docs/implementation-plan.md
```

## 计划内容

必须覆盖：

- 模组包结构。
- 注册项清单：Items、EntityTypes、DataComponents、Menus、Packets、CreativeTabs 等。
- 战备配置数据格式。
- 呼叫装置状态机。
- WASD 输入与客户端面板。
- 信标投掷实体和落地实体。
- 倒计时、光柱和世界文本/图标显示。
- CBC 炮弹生成适配层。
- 玩家解锁数据和许可证物品。
- 内置战备数据。
- 测试和手工验证清单。

## 里程碑建议

1. 建立 NeoForge 1.21.1 模组骨架。
2. 注册呼叫装置、许可证和必要数据组件。
3. 实现客户端输入面板与指令匹配。
4. 实现信标投掷实体和落地状态。
5. 实现倒计时与基础视觉效果。
6. 接入 CBC 炮弹生成适配层。
7. 实现战备配置、解锁和冷却。
8. 增加内置战备和兼容性验证。

## 风险处理

计划中必须标注：

- 依赖 CBC 内部实现的任务。
- 需要游戏内验证的任务。
- 客户端/服务端同步风险。
- 数据包或附属模组兼容风险。
- 可先用占位实现验证的任务。

