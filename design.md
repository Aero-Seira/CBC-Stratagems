# CBC-Stratgems 技术设计文档

## 0. 状态与范围

- 当前状态：已完成 CBC 源码调研，见 `research/cbc-source-map.md`。本文进入技术设计阶段，不包含实现代码。
- 支持版本：第一版只支持 NeoForge 1.21.1，目标 CBC `v5.11.6+mc.1.21.1`。
- 设计目标：实现手持呼叫装置输入指令、生成可投掷信标、落点倒计时提示、倒计时结束后从高处生成并发射 CBC 炮弹。
- 第一阶段非目标：Forge 1.20.1 兼容层、支援炮台、装备补给、游戏内完整战备编辑 UI。
- 第二阶段目标：创造玩家专用的战备编辑器，支持导出/复用 JSON 战备配置。
- 复杂度判断：火力核心较薄，主要风险在交互状态、客户端显示和附属兼容边界。CBC 调研显示炮管最终发射也是 `setPos`、`setChargePower`、`shoot`、`addFreshEntity`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/cannon_control/contraption/MountedBigCannonContraption.java:455-464`。

## 1. 炮弹生成与发射

### 设计决策

- **可直接实现**：服务端封装 `CbcProjectileLauncher`，输入 `ServerLevel`、炮弹 `ItemStack`、发射点、目标点、power、spread、可选 owner，输出是否成功生成炮弹。
- **需要适配**：`CbcProjectileLauncher` 只接受 `BlockItem` 且其 `Block` 是 CBC `ProjectileBlock<?>` 的物品栈。适配器内部调用 `ProjectileBlock#getProjectile(level, stack)`，然后执行 `setPos`、`setChargePower`、`shoot`、`level.addFreshEntity`。
- **未找到**：CBC 没有公开的“任意位置发射炮弹”高级 API；上述适配器来自 CBC 炮管发射链路归纳，不是 CBC 稳定 API。
- **高风险隔离**：不要调用 `MountedBigCannonContraption#fireShot` 或复用整段炮管扫描逻辑。它包含装填结构、故障、squib、后坐、contraption 细节，作为参考即可。

### 参数模型

每次炮击由战备配置决定这些参数：

- `projectile`：炮弹 `ItemStack`，包含 item id 和可选 data components。
- `count`：发射次数。
- `interval_ticks`：同一轮炮击间隔。
- `countdown_ticks`：信标落地到第一发开火的延迟。
- `cooldown_ticks`：投掷后该战备进入冷却的时长。
- `spawn_height`：相对目标点的生成高度。
- `spawn_scatter`：发射点水平随机偏移。
- `target_scatter`：目标点水平随机偏移。
- `power`：传给 `projectile.shoot(..., power, spread)` 的速度/装药标量。
- `spread`：传给 CBC/vanilla projectile shoot 的散布。
- `max_obstruction_blocks`：信标落点到本次火力发射点之间允许阻隔的最大方块数。

CBC 自身继续决定这些内容：

- 炮弹 tick、重力、阻力、连续碰撞、穿透、反弹、爆炸和实体伤害。证据：`AbstractCannonProjectile.java:97-190`、`:218-345`、`:607-628`。
- tracer、fuze、fluid 等炮弹状态从 `ItemStack` 迁移到实体。证据：`ProjectileBlock.java:106-116`、`FuzedProjectileBlock.java:63-67`、`FluidShellBlock.java:81-88`。
- 炮弹数值属性来自 CBC `munition_properties/projectiles` JSON 和 handler。证据：`MunitionPropertiesHandler.java:37-66`、`he_shell.json:1-21`。

### 发射计算

- 服务端在信标 marker 倒计时结束后执行发射。
- 对每一发炮弹，先计算目标点：`markerPos + randomHorizontal(target_scatter)`。
- 再计算生成点：`markerPos + randomHorizontal(spawn_scatter) + up(spawn_height)`，Y 值需要夹在世界 build height 内。
- 输入开始和投掷前都执行环境校验：如果玩家所在维度有天花板，或玩家当前位置不能看到天空，则直接提示无法使用，不生成 beacon，不进入冷却。
- 信标落地后，按本次计算出的预设火力发射点到信标落点做阻隔统计；阻隔方块数超过 `max_obstruction_blocks` 时取消 marker，提示玩家“火力路径受阻”，不进入冷却。
- 发射方向为 `targetPoint - spawnPoint` 的单位向量，传入 `projectile.shoot(dir.x, dir.y, dir.z, power, spread)`。
- 默认策略是“从高处向落点俯射”，不模拟真实炮管结构和后坐。CBC 的实际炮弹物理仍会处理落点前的碰撞、穿透和爆炸。
- 阻隔统计建议实现为服务端方块步进或 ray clip 的封装，具体采样规则在实现计划中拆出验证任务。

### 服务端与客户端边界

- 服务端权威：信标落地、marker 生成、环境校验、倒计时结束、炮弹生成、冷却开始、解锁校验、指令校验。
- 客户端展示：呼叫面板 overlay、输入反馈、marker 光柱/文字/图标、倒计时显示。
- 客户端不得直接触发炮弹生成，只能发送输入/投掷请求；服务端重算并校验当前物品状态、玩家解锁和冷却。

## 2. 炮弹类型系统

### 战备如何引用 CBC 炮弹

- **设计决策**：战备配置引用炮弹 `ItemStack`，不直接引用 CBC `EntityType`。
- 理由：CBC 的炮弹物品/方块负责把 tracer、fuze、fluid 等组件迁移到实体；直接引用实体会绕过这条路径。
- 证据：`ProjectileBlock#getProjectile(Level, ItemStack)` 存在于 `.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlock.java:110-112`；带引信和流体炮弹重写该方法迁移组件：`FuzedProjectileBlock.java:63-67`、`FluidShellBlock.java:81-88`。

### 配置草案

第一版优先实现自定义/数据驱动战备加载，先用简单调试战备验证完整链路；后续添加内置战备只需要补充本模组自带 JSON。配置示例：

```json
{
  "name": { "translate": "stratagem.cbc_stratgems.he_barrage" },
  "icon": "cbc_stratgems:textures/gui/stratagem/he_barrage.png",
  "command": ["up", "right", "down", "down"],
  "cooldown_ticks": 1800,
  "countdown_ticks": 100,
  "artillery": [
    {
      "projectile": {
        "id": "createbigcannons:he_shell",
        "count": 1
      },
      "count": 4,
      "interval_ticks": 12,
      "spawn_height": 96,
      "spawn_scatter": 18.0,
      "target_scatter": 9.0,
      "power": 4.0,
      "spread": 0.75,
      "max_obstruction_blocks": 8
    }
  ]
}
```

实现计划阶段需要确认 Minecraft 1.21.1 中 `ItemStack` Codec 的精确 JSON 形状；如果直接 codec 不适合用户编辑，则使用本模组自己的 `item + components` 结构，再转换为 `ItemStack`。

### 附属炮弹兼容

- **可直接实现**：任何已注册物品，只要是 `ProjectileBlock` 的 `BlockItem`，就可被战备配置引用。
- **需要适配**：配置加载时只做 registry id 解析；真正发射时再检查 item/block 类型，失败则记录日志并跳过该发炮击。
- **未找到**：CBC 没有从任意 `EntityType` 反查炮弹物品、引信和组件的通用 API。
- **第一版边界**：不支持只注册实体、不提供 `ProjectileBlock`/炮弹物品模型的附属炮弹。

### 属性归属

- CBC 炮弹自身决定：实体伤害、爆炸半径/威力、穿深、韧性、偏转、重力、阻力、fuze 行为、fluid 内容、命中效果。
- CBC-Stratgems 战备配置决定：何时开火、从哪里开火、瞄准哪里、发几发、间隔、散布、power、冷却、倒计时、显示文本和图标。

## 3. 视觉效果

### 资源包可替换资源

- 非物品客户端资源不得用代码硬绘制成固定外观。指令界面背景、边框、输入箭头、成功/失败状态、marker 图标占位等都以 `ResourceLocation` 引用纹理。
- 第一版提供默认 UI 主题资源，例如 `assets/cbc_stratgems/cbc_stratgems/ui/default.json`，字段包含 panel texture、箭头 idle/active/success/fail 纹理、默认图标占位、颜色和布局参数。
- 资源包可以替换默认纹理，也可以覆盖 UI 主题 JSON。战备自身的 `icon` 字段继续由战备 JSON 指向资源包纹理。
- 不把箭头符号写成硬编码字符；即使第一版视觉简单，也使用箭头纹理或主题资源中的 glyph/texture 配置。

### 音效接入点

- 预留并注册本模组 sound events：
  - `ui_open`
  - `ui_close`
  - `input`
  - `input_failed`
  - `input_complete`
  - `beacon_throw`
  - `beacon_land`
  - `strike_denied`
  - `strike_start`
- 第一版可以使用占位或静音资源，但调用点必须存在。资源包可通过 `sounds.json` 替换具体音频。
- 客户端本地反馈音用于界面打开、关闭和输入；服务端确认音用于输入通过、信标落地、取消发射和火力开始，避免客户端误报成功。

### 信标与 marker 实体

- 投掷中的信标使用自定义 `StratagemBeaconProjectileEntity`，行为参考原版雪球/末影珍珠：右键创建 projectile、`shootFromRotation`、`addFreshEntity`。证据：NeoForm sources zip `SnowballItem.java:23-46`、`EnderpearlItem.java:20-44`。
- 信标命中后，服务端创建 `StratagemMarkerEntity`，记录战备 ID、owner UUID、落点、开始 tick、倒计时 tick，并移除投掷实体。
- `StratagemMarkerEntity` 负责服务端倒计时；到点后调用炮击调度器并 discard。
- 选择实体而不是方块：不破坏地形，不需要处理方块替换/掉落/权限，移动维度和客户端渲染同步也更直接。

### 红色光柱

- **设计决策**：自定义 marker entity renderer，在 marker 坐标系中调用或封装原版 `BeaconRenderer.renderBeaconBeam`，使用红色 ARGB/RGB、较短半径和固定高度。
- 证据：原版 `BeaconRenderer.renderBeaconBeam(...)` 是 public static，参数包含 texture、gameTime、height、color、beamRadius、glowRadius：NeoForm sources zip `BeaconRenderer.java:52-64`；实际使用 `RenderType.beaconBeam`：`BeaconRenderer.java:65-131`。
- **需要适配**：原版信标光柱不是服务端世界对象；它是客户端渲染函数。marker renderer 需要自己处理 culling、距离、光照和可见性。

### 战备名、图标、倒计时

- 第一版使用自定义 marker entity renderer 绘制 billboard，而不是依赖 `Display.TextDisplay`。
- 理由：`Display.TextDisplay` 的关键 setter 在源码中是 private，Java 代码动态更新文本不直接；用自定义 renderer 可同时画文字和图标。证据：NeoForm sources zip `Display.java:796-839`、`:849-887`。
- 倒计时显示客户端按 `activationGameTime - clientLevel.getGameTime()` 计算，减少每 tick 同步。
- 图标优先使用战备配置中的 texture resource。若资源缺失，客户端降级显示默认图标；是否用炮弹物品图标作为 fallback 在实现计划中确认。
- 第一版不要求实现真实图标绘制；renderer 保留固定尺寸占位符或默认纹理入口，避免后续补图标时改数据模型。

### 同步数据

Marker entity 需要同步：

- `stratagem_id`
- `owner_uuid`
- `target_pos`
- `created_game_time`
- `countdown_ticks`
- `display_name`
- `icon`

其中 `display_name` 和 `icon` 也可以只同步 `stratagem_id`，客户端从已同步的战备 registry/cache 查表。第一版建议同步 `stratagem_id + created_game_time + countdown_ticks`，显示数据由客户端配置缓存查表。

## 4. 物品与交互

### 呼叫装置与信标

- **设计决策**：使用同一个 Item，通过数据组件区分 `CALLER` 和 `BEACON` 状态。
- 理由：玩法上“呼叫装置切换为信标，投掷后变回呼叫装置”更像同一物品状态切换；同一个 Item 可避免替换物品栈导致附魔、名称、耐久或组件丢失。
- 需要注册的本模组数据组件：
  - `mode`：`caller` 或 `beacon`。
  - `selected_stratagem`：已输入成功并锁定的战备 ID，仅 beacon 状态有效。
  - `active_input`：可选，当前输入序列；也可以只存在客户端 session，不持久化。
- 证据参考：CBC 在 1.21.1 中用 `DataComponentType` 存 fuze/tracer/projectile 等物品状态，并设置 persistent/network sync：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCDataComponents.java:47-99`。

### 界面键位与输入

- 战备界面打开由可更改键位控制，注册 `open_stratagem_panel` KeyMapping；默认键位在实现阶段选择，用户可在 Minecraft 控制设置中修改。
- `CALLER` 状态下，玩家按下 `open_stratagem_panel` 且主手持装置时，客户端向服务端请求开始输入；服务端通过后开始 use/session 并打开 overlay。
- 保留右键物品使用作为服务端能力入口，但不把“打开战备界面”硬绑定到固定鼠标键。原版持续使用入口见 NeoForm sources zip `Item.java:162-185`、`ItemUtils.java:9-13`。
- 客户端在 use 持续期间显示 overlay，并在 `ClientTickEvent` 读取 WASD 输入。CBC 读取 `mc.player.input.left/right/up/down` 的路径可参考 `.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCClientCommon.java:149-164`。
- 战备界面可以被其他操作打断：切换物品、打开其他 screen、受控移动状态变化、死亡、松开/取消键位或服务端拒绝都会关闭 session。
- 当战备界面处于活跃状态时，WASD 输入优先级属于本模组：客户端消费方向键边沿并阻止同一 tick 的移动/其他 WASD 相关逻辑覆盖本次指令。非 WASD 操作仍可打断界面。
- 客户端每次方向输入发送 server packet。服务端维护玩家当前输入 session，并校验：
  - 玩家手中仍是呼叫装置。
  - 玩家已解锁候选战备。
  - 战备不在冷却。
  - 输入序列仍匹配某个战备前缀。
  - 完整命中某个战备时，该战备没有被其他战备命令前缀歧义覆盖。
- 输入成功后，服务端把物品组件切到 `mode=beacon`，写入 `selected_stratagem`。客户端 overlay 显示完成状态，玩家释放打开键或触发打断后关闭面板。
- 释放打开键或被打断且未完成输入：服务端清除 input session，物品保持 `CALLER`。

### 投掷信标

- `BEACON` 状态右键：不进入长按输入，直接按雪球/末影珍珠模式生成 `StratagemBeaconProjectileEntity`。
- 投掷不消耗物品数量；服务端生成投掷实体后立即把物品组件重置为 `mode=caller` 并清空 `selected_stratagem`。
- 冷却不在投掷瞬间最终确认；只有信标落地后通过环境/阻隔校验并正式进入火力调度时，服务端才开始该战备冷却。
- 如果玩家在投掷前失去解锁、战备被 datapack 移除或进入冷却，服务端拒绝投掷并把物品重置为 `CALLER`。

### 冷却

- 不使用 vanilla `player.getCooldowns().addCooldown(item, ticks)` 作为主要机制，因为它只能按 item 冷却，不能表达每个战备独立冷却。
- 使用服务端玩家数据保存 `stratagem_id -> cooldown_end_game_time`。
- 客户端 overlay 从同步数据中显示每个战备是否可用。

## 5. 解锁与数据存储

### 战备配置

- 第一版使用 datapack JSON 定义所有战备。先实现自定义/调试战备加载和执行链路；内置战备后续通过本模组资源包自带 JSON 增加，不需要改核心代码。
- 用户和整合包可通过 datapack 覆盖或新增战备。
- 配置 reload 时校验：
  - command 非空，只包含 `up/down/left/right`。
  - 全局命令满足“无前缀重复”规则。
  - cooldown/countdown/count/interval/power/spread 等数值在合理范围内。
  - projectile item id 能解析；是否为 CBC `ProjectileBlock` 在发射前再次检查。

### 战备 JSON 导出与复用

- 自定义战备的权威存储格式仍然是 JSON，便于导出、复制、放入 datapack 或在服务器间复用。
- 游戏内编辑器保存时生成同一份 `StratagemDefinition` 数据模型；本地单人可写入配置/导出目录，多人服务器需要由服务端决定是否允许写入世界数据或仅导出给玩家。
- 编辑器生成的 JSON 不应包含运行时玩家数据，如解锁状态、冷却和当前输入。

### 许可证物品

- 许可证使用单独 Item，数据组件 `stratagem_id` 绑定一个战备 ID。
- 许可证使用规则固定为：主手持呼叫装置/信标，副手持许可证。主手装置处理本次右键，读取副手许可证上的 `stratagem_id`，服务端检查该 ID 存在，并把它加入玩家解锁集合。
- 不扫描背包内其他呼叫装置，避免玩家有多个装置时引入归属判断规则。
- 生存模式消耗许可证，创造模式不消耗。
- 创造模式或命令可通过给许可证写入 `stratagem_id` 来解锁自定义 datapack 战备。

### 玩家数据

- 服务端保存：
  - `unlocked_stratagems: Set<ResourceLocation>`
  - `cooldowns: Map<ResourceLocation, long>`
  - 当前输入 session 可只放内存，不持久化。
- 第一版建议用服务端持久数据按玩家 UUID 保存，登录时同步给客户端。实现计划阶段再定具体用 NeoForge attachment 还是 `SavedData`；二者都不影响 CBC 适配层。
- 客户端缓存只用于 overlay 和预测显示，服务端始终重新校验。

### 创造模式自定义

- 第一版先通过 datapack JSON 创建自定义战备。
- 第二阶段提供创造玩家专用物品 `stratagem_editor`，使用后打开编辑 GUI。
- 编辑 GUI 能选择游戏内所有兼容炮弹物品：以注册表中 `BlockItem` 且 `Block instanceof ProjectileBlock<?>` 的物品作为候选。
- 编辑 GUI 支持设置基础字段：名称、指令、图标、冷却、倒计时、发射次数、发射间隔、发射点高度、目标散布、发射点散布、power、spread、阻隔阈值。
- 编辑 GUI 支持炮弹配置：选择炮弹物品、配置可用 data components，如引信、tracer、流体内容等。
- 编辑 GUI 可以暴露“高级参数”区域，参考 CBC 炮弹属性 JSON 的概念，如爆炸威力、范围、破坏、实体伤害、穿深、阻力、重力等；这些不直接改 CBC 全局炮弹定义，而是作为本模组自定义炮击/临时覆盖的扩展字段。第一阶段不实现该覆盖机制。
- 编辑 GUI 支持高级发射时间/位置编排：以信标落点为中心显示坐标网格，玩家点选相对位置、设置每个发射点的时间偏移、发射角度、弹速、存活时间和目标偏移。
- 自定义图标由 resource pack/datapack 提供 texture resource；许可证通过命令、创造物品变体或编辑器导出流程绑定对应战备 ID。

## 6. 兼容性策略

### CBC 兼容入口

- 首选入口：注册表中的炮弹物品 `ItemStack`。
- 发射入口：`ProjectileBlock#getProjectile(Level, ItemStack)`，再执行 `setPos`、`setChargePower`、`shoot`、`addFreshEntity`。
- 属性入口：CBC 自己的 munition properties JSON/handler，不在本模组复制炮弹伤害、爆炸、穿透等数值。

### 避免依赖

- 不直接调用 `MountedBigCannonContraption#fireShot`。
- 不复制 CBC 炮管装填、故障、squib、后坐和 contraption 扫描逻辑。
- 不硬编码 `CBCEntityTypes.HE_SHELL` 等实体作为战备配置主入口；内置战备也用炮弹 item id。
- 不为附属炮弹维护手写白名单。

### 必须依赖但要隔离的边界

- `ProjectileBlock` 位于 CBC 内部包 `rbasamoyai.createbigcannons.munitions.big_cannon`，但没有找到更稳定的公开发射 API。后续实现必须把所有 CBC 直接调用集中在 `compat/cbc` 适配层。
- `BeaconRenderer.renderBeaconBeam` 是原版客户端渲染函数，签名可能随 MC 版本变化。后续渲染代码集中在 marker renderer，便于升级。
- DataComponent 和 ItemStack JSON codec 是 1.21 系 API，Forge 1.20.1 移植时需要重做物品状态和配置序列化。

### 版本升级风险

- CBC 可能调整 `ProjectileBlock#getProjectile`、munition properties handler 或包名。
- CBC 可能改变炮弹构造时属性初始化方式。
- NeoForge 可能改变 attachment/SavedData 推荐做法。
- 原版 `Display`/`BeaconRenderer` 渲染接口可能改变。

## 7. 代码索引

- 源码调研总览：`research/cbc-source-map.md`
- 玩法设想：`docs/stratagem-concept.md`
- CBC 炮弹总基类：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/AbstractCannonProjectile.java`
- CBC big cannon 炮弹基类：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile.java`
- CBC 带引信炮弹基类：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/FuzedBigCannonProjectile.java`
- CBC 炮弹方块入口：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlock.java`
- CBC 炮管发射参考：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/cannon_control/contraption/MountedBigCannonContraption.java`
- CBC 实体注册：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCEntityTypes.java`
- CBC 数据组件参考：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCDataComponents.java`
- CBC 弹药属性 handler：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/MunitionPropertiesHandler.java`
- 原版信标光柱参考：NeoForm sources zip `net/minecraft/client/renderer/blockentity/BeaconRenderer.java`
- 原版投掷物参考：NeoForm sources zip `net/minecraft/world/item/SnowballItem.java`、`EnderpearlItem.java`、`net/minecraft/world/entity/projectile/ThrowableItemProjectile.java`
- 原版持续使用参考：NeoForm sources zip `net/minecraft/world/item/Item.java`、`ItemUtils.java`、`BowItem.java`、`SpyglassItem.java`

## 8. 开放问题

### 已确认

- 第一版先实现自定义/数据驱动战备和简单调试实现，不把内置战备列表作为前置条件。
- 许可证使用条件：主手呼叫装置/信标，副手许可证，不扫描背包。
- 洞穴或有天花板维度直接不可用；信标落点到预设火力发射点阻隔过多时取消发射并提示，不进入冷却。
- 第一版 marker 不要求真实图标；红色光柱 + 名称 + 倒计时 + 图标占位即可。
- UI 非物品资源、箭头符号和图标占位都走资源包可替换资源，不写死为代码绘制。
- 预留界面打开/关闭、输入、输入失败、输入通过、信标投掷/落地、拒绝打击和火力开始音效。
- 战备界面打开键位可更改；界面活跃时 WASD 指令输入优先级最高，其他操作仍可打断界面。
- 游戏内战备编辑器作为第二阶段能力，使用创造玩家专用物品打开，输出可导出复用的 JSON。

### 阻塞实现计划前需要决定

- 当前无阻塞实现计划的问题。

### 需要游戏内验证

- `max_obstruction_blocks` 先用调试默认值 `8`，后续按炮弹从高处落下的实际体验调参。
- 阻隔统计先只计算实心阻挡方块，是否忽略树叶、草、液体等非完整阻挡方块需要实测。
- 环境校验先使用“维度有天花板或 `!level.canSeeSky(player.blockPosition())`”判定洞穴/地狱不可用；该规则会把室内、树下和玩家头顶有方块的情况都视为不可用，需要实测确认是否过严。

### 不阻塞第一版实现

- 是否支持 entity-only 的 CBC 附属炮弹。
- 是否把炮弹 owner 接入 CBC 伤害归因。CBC 炮管路径没有显式设置 owner，只设置短时间 untouchable：`MountedBigCannonContraption.java:459-462`、`AbstractCannonProjectile.java:641-650`。
- 是否在后续加入游戏内创造模式编辑 UI。
- Forge 1.20.1 移植时的数据组件/NBT 兼容设计。
