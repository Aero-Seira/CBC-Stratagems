# CBC-Stratagems 实现计划

## 0. 当前前提

- 目标版本：NeoForge 1.21.1。
- 当前仓库还没有 Gradle/源码骨架，第一步需要创建模组工程。
- 已完成输入文档：
  - `docs/stratagem-concept.md`
  - `research/cbc-source-map.md`
  - `design.md`
- 第一版目标：先实现自定义/数据驱动战备和简单调试战备，验证完整链路；内置战备列表后续通过 JSON 增加。

## 1. 包结构建议

建议 Java 根包：

```text
com.aeroseira.cbcstratagems
```

建议模块划分：

```text
com.aeroseira.cbcstratagems
  CBCStratagems.java
  CBCStratagemsClient.java
  registry/
    ModItems.java
    ModEntityTypes.java
    ModDataComponents.java
    ModAttachments.java
    ModCreativeTabs.java
    ModPackets.java
    ModSoundEvents.java
    ModKeyMappings.java
  item/
    StratagemDeviceItem.java
    StratagemLicenseItem.java
    StratagemEditorItem.java
  entity/
    StratagemBeaconProjectile.java
    StratagemMarkerEntity.java
  client/
    StratagemInputOverlay.java
    StratagemMarkerRenderer.java
    StratagemClientState.java
    StratagemUiTheme.java
    StratagemUiThemeReloadListener.java
    StratagemEditorScreen.java
  stratagem/
    StratagemDefinition.java
    StratagemCommand.java
    StratagemReloadListener.java
    StratagemRegistry.java
    StratagemValidator.java
  player/
    PlayerStratagemData.java
    PlayerStratagemDataManager.java
  network/
    ServerboundStratagemInputPacket.java
    ClientboundStratagemDefinitionsPacket.java
    ClientboundPlayerStratagemDataPacket.java
    ClientboundStratagemInputStatePacket.java
  artillery/
    ArtilleryStrikeScheduler.java
    ArtilleryShotPlan.java
    ObstructionScanner.java
    StrikeTimeline.java
  compat/cbc/
    CbcProjectileLauncher.java
    CbcProjectileLaunchResult.java
    CbcProjectileCatalog.java
```

所有直接引用 CBC 内部类的代码集中在 `compat/cbc`，尤其是 `ProjectileBlock`。

## 2. 注册项清单

### Items

- `stratagem_device`
  - 同一个物品承担呼叫装置和信标两种状态。
  - 主手右键时优先检查副手是否为许可证；如果是，则执行解锁。
  - `mode=caller` 时默认通过主手右键长按进入输入。
  - `mode=beacon` 时右键投掷信标。
- `stratagem_license`
  - 通过 DataComponent 绑定 `stratagem_id`。
  - 正常使用路径由主手 `stratagem_device` 读取副手许可证处理。
- `stratagem_editor`
  - 第二阶段物品。
  - 创造玩家专用，使用后打开战备编辑 GUI。
  - 第一阶段可先不注册，或注册但只提示“未实现”。

### EntityTypes

- `stratagem_beacon`
  - 类似雪球/末影珍珠的投掷实体。
  - 保存 `stratagem_id`、owner UUID、投掷起点等必要数据。
- `stratagem_marker`
  - 信标落地后的倒计时实体。
  - 服务端 tick 负责倒计时、阻隔校验、火力调度。
  - 客户端 renderer 负责红光柱、名称、倒计时、图标占位。

### DataComponents

- `device_mode`
  - enum/string：`caller` 或 `beacon`。
- `selected_stratagem`
  - `ResourceLocation`，仅 `beacon` 状态有效。
- `license_stratagem`
  - `ResourceLocation`，绑定许可证解锁目标。

可选：

- `debug_input`
  - 仅调试时保留当前输入，正式版可不用持久化。

### Player Data

第一版建议实现 `PlayerStratagemDataManager` 抽象，底层优先用 NeoForge player attachment；如果实现时 API 摩擦较大，退回 `SavedData` 按玩家 UUID 保存。

保存内容：

- `unlocked_stratagems: Set<ResourceLocation>`
- `cooldowns: Map<ResourceLocation, long>`
- 当前输入 session：只存在内存，不持久化。

### Packets

- `ServerboundStratagemInputPacket`
  - 客户端按下 WASD 方向时发送。
  - 字段：direction、client sequence id 可选。
- `ClientboundStratagemDefinitionsPacket`
  - 登录和 datapack reload 后同步战备定义摘要。
  - 客户端 overlay 需要名称、指令、图标路径、冷却信息。
- `ClientboundPlayerStratagemDataPacket`
  - 同步玩家解锁和冷却。
- `ClientboundStratagemInputStatePacket`
  - 同步当前输入、成功、失败原因、环境不可用提示。
- `ServerboundOpenStratagemEditorPacket`
  - 第二阶段使用，创造玩家请求打开编辑器。
- `ServerboundSaveStratagemPacket`
  - 第二阶段使用，提交编辑器生成的 JSON/definition。

第一版不需要 Menu。呼叫面板用 client overlay，不打开 container。第二阶段编辑器需要 Screen，是否需要 Menu 取决于是否允许从玩家物品栏选择/修改炮弹 ItemStack。

### KeyMappings

- `open_stratagem_panel`
  - 可选辅助键位，用于后续允许玩家改用键盘打开/维持战备输入界面。
  - 用户可在 Minecraft 控制设置中更改。
  - 默认不绑定，第一版默认入口保持为手持呼叫装置右键长按。

### SoundEvents

注册但允许资源包替换具体音频：

- `ui_open`
- `ui_close`
- `input`
- `input_failed`
- `input_complete`
- `beacon_throw`
- `beacon_land`
- `strike_denied`
- `strike_start`

### Creative Tab

- 可注册 `cbc_stratagems` 创造标签页，包含 `stratagem_device` 和未绑定的 `stratagem_license`。
- 第二阶段加入 `stratagem_editor`。
- 已绑定许可证的创造变体可后续通过 creative tab display entries 动态加入；第一版也可先用命令写组件。

## 3. 客户端资源与主题

### 资源路径

默认资源建议：

```text
assets/cbc_stratagems/textures/gui/stratagem/panel.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_up.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_down.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_left.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_right.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_success.png
assets/cbc_stratagems/textures/gui/stratagem/arrow_failed.png
assets/cbc_stratagems/textures/gui/stratagem/icon_placeholder.png
assets/cbc_stratagems/cbc_stratagems/ui/default.json
assets/cbc_stratagems/sounds.json
```

### UI Theme JSON

`default.json` 包含：

```json
{
  "panel": "cbc_stratagems:textures/gui/stratagem/panel.png",
  "arrow_up": "cbc_stratagems:textures/gui/stratagem/arrow_up.png",
  "arrow_down": "cbc_stratagems:textures/gui/stratagem/arrow_down.png",
  "arrow_left": "cbc_stratagems:textures/gui/stratagem/arrow_left.png",
  "arrow_right": "cbc_stratagems:textures/gui/stratagem/arrow_right.png",
  "arrow_success": "cbc_stratagems:textures/gui/stratagem/arrow_success.png",
  "arrow_failed": "cbc_stratagems:textures/gui/stratagem/arrow_failed.png",
  "icon_placeholder": "cbc_stratagems:textures/gui/stratagem/icon_placeholder.png",
  "text_color": "#FFFFFF",
  "accent_color": "#FF4040"
}
```

第一版可以固定读取 `cbc_stratagems:default` 主题；后续再允许客户端配置选择主题。资源包可覆盖默认 JSON 和纹理。

### 音效调用点

- 打开界面：`ui_open`
- 关闭界面：`ui_close`
- 输入方向：`input`
- 输入失败：`input_failed`
- 指令通过：`input_complete`
- 投掷信标：`beacon_throw`
- 信标落地：`beacon_land`
- 环境/阻隔取消：`strike_denied`
- 火力开始：`strike_start`

## 4. 战备配置数据格式

### 数据路径

建议 reload listener 读取：

```text
data/<namespace>/cbc_stratagems/stratagems/*.json
```

资源 ID 由 namespace + 文件路径决定，例如：

```text
data/cbc_stratagems/cbc_stratagems/stratagems/debug_he.json
=> cbc_stratagems:debug_he
```

### JSON 字段

```json
{
  "name": { "translate": "stratagem.cbc_stratagems.debug_he" },
  "icon": "cbc_stratagems:textures/gui/stratagem/placeholder.png",
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
  ],
  "timeline": []
}
```

### 校验规则

- `command` 只允许 `up/down/left/right`，不能为空。
- 全部已加载战备满足“无前缀重复”规则。
- `cooldown_ticks >= 0`，`countdown_ticks >= 0`。
- `count >= 1`，`interval_ticks >= 0`。
- `spawn_height > 0`，`spawn_scatter >= 0`，`target_scatter >= 0`。
- `power > 0`，`spread >= 0`。
- `max_obstruction_blocks >= 0`，调试默认值为 `8`。
- `projectile.id` 必须能解析为 item；是否为 CBC `ProjectileBlock` 在发射时再次检查。
- `timeline` 第一阶段可为空；第二阶段编辑器用于保存高级发射时间/位置编排。

### 调试战备

第一版提供一个调试数据：

- `cbc_stratagems:debug_he`
- 炮弹：`createbigcannons:he_shell`
- 指令：短序列，便于频繁测试。
- 图标：placeholder。

这不是最终内置列表，只用于验证数据加载、输入、信标、marker、CBC 发射和冷却。

## 5. 呼叫装置状态机

### 状态

```text
CALLER_IDLE
CALLER_USING
BEACON_READY
BEACON_THROWN
```

### 转换

- `CALLER_IDLE -> CALLER_USING`
  - 主手 `stratagem_device`，无副手许可证，右键长按开始持续使用。
  - `open_stratagem_panel` KeyMapping 只作为可选辅助入口，默认不绑定。
  - 服务端先检查维度/天空：维度有天花板或 `!level.canSeeSky(player.blockPosition())` 时拒绝并提示。
- `CALLER_USING -> BEACON_READY`
  - 服务端收到方向输入后匹配完整战备。
  - 玩家已解锁，战备不在冷却。
  - 服务端写入 `device_mode=beacon` 和 `selected_stratagem`。
- `CALLER_USING -> CALLER_IDLE`
  - 玩家释放打开键或被其他操作打断，且未完成输入。
  - 清空输入 session。
- `BEACON_READY -> BEACON_THROWN`
  - 右键投掷信标实体。
  - 服务端再次检查维度/天空；失败时提示不可用，保持或重置为 `CALLER`，不进入冷却。
  - 物品立即重置为 `device_mode=caller`。
  - 此时不最终开始冷却。
- `BEACON_THROWN -> CALLER_IDLE`
  - 信标落地生成 marker。
  - marker 通过环境/阻隔校验后进入火力调度并开始冷却。
  - 校验失败时提示玩家，不进入冷却。

### 许可证优先级

主手装置右键时：

1. 如果副手是 `stratagem_license`，先处理解锁。
2. 解锁成功后按生存/创造规则消耗或保留许可证。
3. 不进入呼叫输入或信标投掷逻辑。

## 6. WASD 输入与客户端面板

### 输入采集

- 客户端只在玩家右键长按装置或激活可选 `open_stratagem_panel`、正在使用 `stratagem_device` 且 `device_mode=caller` 时采集。
- 使用 `ClientTickEvent` 检测方向键边沿，不按住重复刷输入。
- 方向映射：W=`up`，S=`down`，A=`left`，D=`right`。
- 每个方向边沿发送 `ServerboundStratagemInputPacket`。
- 战备界面活跃时，WASD 输入由本模组优先消费，并在同 tick 抑制玩家移动/其他 WASD 相关处理。
- 其他非 WASD 操作可以打断界面，例如切换物品、打开其他 screen、死亡、取消键位、服务端拒绝。

### 服务端匹配

- 服务端维护 `PlayerInputSession`：
  - 当前输入序列。
  - 开始 game time。
  - 当前候选战备集合。
- 每次输入后：
  - 如果没有候选，失败并清空。
  - 如果命中完整战备，检查解锁和冷却，成功则切换物品状态。
  - 如果仍为前缀，继续等待。

### Overlay

- 显示当前可用战备、当前输入序列、失败反馈、冷却状态。
- 第一版可使用简单 HUD 绘制，不做复杂动画，但所有面板、箭头和占位图标都来自 UI theme 资源。
- 客户端显示只是反馈，不能作为服务端判断依据。

## 7. 信标投掷实体和落地实体

### `StratagemBeaconProjectile`

- 继承原版 throwable 路径，优先参考 `ThrowableItemProjectile`。
- 构造时设置 owner、`stratagem_id`。
- 右键发射参数先用原版雪球速度：`shootFromRotation(..., 1.5F, 1.0F)`，后续可调。
- 命中方块或实体后，服务端：
  - 计算落点。
  - 创建 `StratagemMarkerEntity`。
  - 传入 owner、`stratagem_id`、落点和倒计时。
  - discard 信标投掷实体。

### `StratagemMarkerEntity`

- 服务端 tick：
  - 第一次 tick 或落地后立即执行阻隔校验。
  - 校验失败：通知 owner，discard，不进入冷却。
  - 校验成功：开始该战备冷却，按 `countdown_ticks` 倒计时。
  - 到点后向 `ArtilleryStrikeScheduler` 提交炮击计划。
- 客户端：
  - 渲染红色光柱。
  - 渲染名称、倒计时和图标占位。

### 阻隔校验

- 玩家投掷前环境校验：
  - 维度有天花板：不可用。
  - `!level.canSeeSky(player.blockPosition())`：不可用。
  - 开始输入和投掷前都执行一次，避免玩家先露天输入、再移动到不可用位置投掷。
- marker 落地后路径校验：
  - 对每个计划炮击先计算预设 fire point。
  - 从 marker 落点到 fire point 统计阻隔方块。
  - 阻隔数量超过 `max_obstruction_blocks` 则取消整次战备。
- 第一版只计算实心阻挡方块；树叶、液体、草等是否计入需要游戏内验证。

## 8. 倒计时、光柱和世界文本/图标

### Renderer

- 注册 `StratagemMarkerRenderer`。
- 光柱：封装调用原版 `BeaconRenderer.renderBeaconBeam`。
- 文字：使用 `Font#drawInBatch` 绘制 billboard。
- 图标：第一版绘制 UI theme 中的占位符或默认纹理；配置字段保留 `icon`。

### 同步

Marker entity 同步字段：

- `stratagem_id`
- `owner_uuid`
- `created_game_time`
- `countdown_ticks`

客户端从 `StratagemClientState` 查 `stratagem_id` 对应名称和图标。

### 验证重点

- 光柱在远近距离是否可见。
- 倒计时是否与服务端发射时机一致。
- marker 在服务端取消时客户端是否及时消失。
- 图标占位是否不遮挡名称和倒计时。

## 9. CBC 炮弹生成适配层

### `CbcProjectileLauncher`

输入：

- `ServerLevel level`
- `ItemStack projectileStack`
- `Vec3 spawnPos`
- `Vec3 targetPos`
- `float power`
- `float spread`
- `@Nullable Entity owner`

步骤：

1. 检查 `projectileStack.getItem()` 是否为 `BlockItem`。
2. 检查 `BlockItem#getBlock()` 是否为 CBC `ProjectileBlock<?>`。
3. 调用 `projBlock.getProjectile(level, projectileStack)`。
4. 如果返回 null，失败。
5. 设置位置、charge power、方向并 `shoot`。
6. 可选：如果 owner 存在，先记录后续归因需求；第一版不强行接入 CBC damage source。
7. `level.addFreshEntity(projectile)`。

返回：

```text
SUCCESS
NOT_BLOCK_ITEM
NOT_CBC_PROJECTILE_BLOCK
CREATE_FAILED
SPAWN_FAILED
```

### 风险

- 依赖 CBC 内部类 `ProjectileBlock`。
- CBC 没有公开任意位置发射 API。
- 需要游戏内验证 fuzed/fluid/tracer 炮弹组件是否完整迁移。

## 10. 玩家解锁数据和许可证

### 数据保存

- `PlayerStratagemDataManager` 提供统一 API：
  - `isUnlocked(player, stratagemId)`
  - `unlock(player, stratagemId)`
  - `getCooldownEnd(player, stratagemId)`
  - `startCooldown(player, stratagemId, endTime)`
  - `sync(player)`
- 底层优先 NeoForge attachment，必要时 fallback `SavedData`。

### 同步

- 玩家登录后同步解锁和冷却。
- datapack reload 后同步战备定义，并清理不存在战备的冷却显示。
- 解锁或冷却变化时发送增量或全量同步。

### 许可证流程

- 主手装置检测副手许可证。
- 读取 `license_stratagem`。
- 校验战备存在。
- 写入玩家解锁集合。
- 生存消耗副手许可证 1 个，创造不消耗。
- 发送成功/失败消息。

## 11. 创造模式编辑器规划

### 目标

第二阶段实现 `stratagem_editor` 创造玩家专用物品。编辑结果仍保存为 `StratagemDefinition` JSON，便于导出、复制和放入 datapack。

### GUI 功能

- 选择兼容炮弹：
  - 扫描注册表中 `BlockItem` 且 `Block instanceof ProjectileBlock<?>` 的物品。
  - 显示物品图标和 registry id。
- 配置炮弹组件：
  - 引信、tracer、流体等 CBC 炮弹 ItemStack data components。
- 基础参数：
  - 名称、指令、图标、冷却、倒计时、发射次数、发射间隔、power、spread、发射高度、散布、阻隔阈值。
- 高级属性：
  - 参考 CBC munition properties JSON 的字段展示爆炸威力、范围、破坏、实体伤害、穿深、重力、阻力等。
  - 第一阶段不实现对 CBC 炮弹全局属性的运行时覆盖；第二阶段需要单独设计“临时覆盖”或“生成专用变体”的安全方案。
- 时间/位置编排：
  - 以落点为中心的坐标网格。
  - 玩家点选相对位置，设置每发炮弹的时间偏移、发射角度、弹速、存活时间和目标偏移。

### 导出

- 单人：允许导出到本地配置/导出目录。
- 多人：默认只把 JSON 展示给玩家复制，或由服务端权限控制是否写入世界数据。
- 导出 JSON 不包含玩家解锁、冷却或运行时输入状态。

## 12. 里程碑

### M1. NeoForge 1.21.1 模组骨架

- 创建 Gradle 工程。
- 设置 mod id：`cbc_stratagems`。
- 添加依赖：Minecraft 1.21.1、NeoForge、Create、CBC。
- 验证空模组能启动 client/server。

风险：依赖版本和本地 CBC dev 环境配置。

### M2. 注册基础项

- 注册 `stratagem_device`、`stratagem_license`。
- 注册 data components。
- 注册 entity types。
- 注册 packet channel。
- 注册 sound events。
- 注册 `open_stratagem_panel` key mapping。
- 注册 creative tab。

验证：物品能出现在创造栏，组件能通过命令或调试日志读写。

### M3. 战备配置加载

- 实现 JSON codec 和 reload listener。
- 实现命令前缀校验。
- 实现 `debug_he` 调试战备。
- 登录同步战备定义到客户端。

风险：ItemStack JSON codec 的最终形状。可先用简化 `projectile.id + count` 结构。

### M4. 玩家数据与许可证

- 实现玩家解锁/冷却数据。
- 实现主手装置 + 副手许可证解锁。
- 实现客户端同步。

验证：重进世界后解锁仍存在。

### M5. 呼叫装置输入

- 实现 `StratagemDeviceItem` 持续使用。
- 以主手右键长按作为默认打开/维持战备面板路径。
- 保留可更改但默认未绑定的 `open_stratagem_panel` KeyMapping 作为辅助入口。
- 实现客户端 WASD 边沿检测。
- 实现界面活跃时 WASD 优先消费和移动抑制。
- 实现打开/关闭/输入/失败/完成音效调用点。
- 实现服务端输入 session 和匹配。
- 输入成功后切换为 beacon 状态。

风险：客户端按键、持续使用、releaseUsing 的同步细节。

### M6. 信标投掷与 marker

- 实现 `StratagemBeaconProjectile`。
- 实现命中生成 `StratagemMarkerEntity`。
- 实现环境校验和阻隔校验。
- 校验失败提示且不进入冷却。

验证：洞穴/下界不可用，露天可投掷，阻隔过多取消。

### M7. 基础视觉

- 实现 UI theme reload listener。
- 实现 marker renderer。
- 红色光柱。
- 名称和倒计时 billboard。
- 图标占位。
- 信标投掷、落地、取消、开火音效调用点。

风险：客户端/服务端 tick 时间差、字体朝向、渲染距离。

### M8. CBC 炮弹适配

- 实现 `CbcProjectileLauncher`。
- 实现 `ArtilleryStrikeScheduler`。
- marker 倒计时结束后按配置发射多发炮弹。
- 验证 HE shell 调试战备。

风险：依赖 CBC 内部类；需要验证炮弹实体生成、引信、爆炸、碰撞行为。

### M9. 兼容性和错误处理

- 非 CBC 炮弹物品配置时给出日志。
- 附属 `ProjectileBlock` 炮弹手工验证。
- datapack reload 后同步和清理错误状态。

### M10. 第一轮整理

- 写使用说明。
- 整理 debug 战备参数。
- 列出后续内置战备清单。
- 准备进入实际代码实现阶段。

### M11. 创造模式编辑器原型

- 注册 `stratagem_editor`。
- 创造玩家使用后打开编辑 screen。
- 扫描兼容 CBC 炮弹物品。
- 编辑基础参数并导出 JSON。
- 先不实现高级运行时炮弹属性覆盖。

### M12. 高级编排编辑器

- 实现落点坐标网格。
- 支持每发炮弹时间/位置/角度/速度编排。
- 导出 `timeline` JSON。
- 游戏内加载并执行 timeline。

## 13. 测试与手工验证清单

### 配置/数据

- 有效 JSON 能加载。
- 无效 direction 被拒绝并打印日志。
- 前缀重复命令被拒绝。
- 缺失 projectile item 被拒绝。
- 非 CBC projectile block 只在发射时失败，不崩溃。

### 物品/输入

- 主手装置 + 副手许可证能解锁。
- 背包里多个装置不影响解锁目标。
- 主手持装置右键长按显示 overlay。
- `open_stratagem_panel` 键位可在控制设置中更改，但默认未绑定。
- 战备界面活跃时 WASD 不移动玩家，只输入箭头。
- 切换物品、打开其他 screen、死亡等操作会打断界面。
- 松开右键或释放打开键取消未完成输入。
- 输入正确后装置进入 beacon 状态。
- 冷却中不能再次锁定同一战备。

### 环境/阻隔

- 下界直接不可用。
- 洞穴内直接不可用。
- 露天可用。
- 落点上方阻隔过多时取消发射，不进入冷却。
- 阻隔未超过阈值时正常倒计时和开火。

### 信标/marker

- 投掷不消耗装置数量。
- 投掷后物品状态回到 caller。
- 信标命中方块生成 marker。
- marker 倒计时结束后消失。
- marker 取消时客户端同步消失。

### CBC 发射

- `createbigcannons:he_shell` 能从高处生成并飞向落点。
- 多发间隔正确。
- spread/target scatter 生效。
- CBC 爆炸、穿透、碰撞由 CBC 正常处理。
- tracer/fuze/fluid 炮弹组件后续手工验证。

### 客户端显示

- 红光柱可见。
- 名称和倒计时不遮挡过多。
- 图标占位不影响布局。
- 多人环境中其他玩家能看到 marker。
- 资源包替换箭头/面板纹理后 UI 更新。
- 资源包替换 sounds 后音效调用点生效。

### 编辑器

- 创造玩家能打开编辑器。
- 能列出 CBC 本体炮弹和附属 `ProjectileBlock` 炮弹。
- 能导出 JSON 并通过 datapack 重新加载。
- 高级字段未实现时不会误导为已生效。

## 14. 明确风险

- **CBC 内部依赖**：`ProjectileBlock` 没有公开 API 替代，必须集中隔离在 `compat/cbc`。
- **同步风险**：输入 session、物品组件、玩家冷却、marker 倒计时都跨客户端/服务端。
- **数据包风险**：战备定义 reload 后，客户端缓存和玩家冷却可能引用已删除 ID。
- **渲染风险**：Beacon beam 和 billboard 依赖客户端渲染 API，版本升级容易变。
- **玩法调参风险**：`max_obstruction_blocks=8` 和 `canSeeSky` 洞穴判定需要游戏内验证。
- **输入优先级风险**：WASD 消费需要避免和原版移动、其他模组 key handling 产生冲突。
- **编辑器风险**：CBC 炮弹属性 JSON 是全局属性模型，游戏内临时覆盖会影响兼容性，需要单独设计，不能直接假设可安全修改。
