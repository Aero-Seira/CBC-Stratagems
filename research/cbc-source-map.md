# CBC 源码调研记录

## 调研环境

- CBC 来源：本地克隆 `.external/CreateBigCannons`，上游 `https://github.com/Cannoneers-of-Create/CreateBigCannons`。
- 分支/tag/提交：`v5.11.6+mc.1.21.1`，提交 `8ed710affc72932f3e798ed05f8084bc796708c4`。
- 版本参数：`.external/CreateBigCannons/gradle.properties` 声明 Minecraft `1.21.1`、NeoForge `21.1.225`、Create `6.0.10-280`、RPL `2.1.2`。
- 调研日期：2026-06-19。
- 原版/NeoForge 映射源码：`/Users/aeroseira/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_e807be61268e78e8e0cd6595848023661857cb19_output.zip`。

## 阶段结论

- 火力核心不复杂：CBC 已有从炮弹 `ItemStack`/`ProjectileBlock` 创建 `AbstractBigCannonProjectile` 的路径，炮管最终发射动作也是 `setPos`、`setChargePower`、`shoot`、`addFreshEntity`：`MountedBigCannonContraption.java:455-464`。
- 兼容附属炮弹的简单入口是保留“炮弹物品栈”作为配置数据，让 CBC 的 `ProjectileBlock#getProjectile(level, stack)` 负责迁移 tracer/fuze/fluid 等状态：`ProjectileBlock.java:106-116`、`FuzedProjectileBlock.java:63-67`、`FluidShellBlock.java:81-88`。
- 需要本模组自写的主要是呼叫装置输入、信标投掷实体、落点光柱/倒计时渲染和战备数据格式；CBC 未找到这些现成能力，相关结论见第 3、4、6 节。

## 1. 炮弹生成与发射链路

### 炮弹实体基类

- CBC 炮弹总基类是 `AbstractCannonProjectile`，继承 `net.minecraft.world.entity.projectile.Projectile`，并实现 `IEntityWithComplexSpawn`；构造函数从弹药属性读取实体伤害和炮弹质量：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/AbstractCannonProjectile.java:61-83`。
- 核心 tick 不走简单 vanilla throwable 逻辑，而是在 `tick()` 中处理移动、阻力/重力、碰撞、入地、chunkload 等：`AbstractCannonProjectile.java:97-184`。
- 弹道力由 `getForces()` 组合阻力和重力：`AbstractCannonProjectile.java:188-190`。重力/阻力读取弹药属性、维度倍率和流体阻力：`AbstractCannonProjectile.java:607-628`。
- 连续碰撞、穿透、反弹、停止、实体命中和爆炸队列都在 `clipAndDamage()` 中处理：`AbstractCannonProjectile.java:218-345`。
- 大炮炮弹基类是 `AbstractBigCannonProjectile`，增加曳光、炮弹模型朝向、方块穿透/反弹判定、装药附加参数：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile.java:47-112`、`:127-156`、`:155-244`、`:251-256`。
- 带引信的大炮炮弹基类是 `FuzedBigCannonProjectile`，保存 `ItemStack fuze` 和 `explosionCountdown`，在 tick、clip、impact 时调用 `FuzeItem` 钩子：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/FuzedBigCannonProjectile.java:19-39`、`:41-64`。
- 自动炮弹是另一套子类 `AbstractAutocannonProjectile extends AbstractCannonProjectile`，带寿命、位移和独立 trail 逻辑：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/autocannon/AbstractAutocannonProjectile.java:39-105`。第一版空袭炮火更适合先以 big cannon projectile 为主。

### 炮弹实体实例化

- Big cannon 炮弹方块抽象类是 `ProjectileBlock`。它提供三种 `getProjectile` 重载：从装填结构块列表、从 `ItemStack`、从世界方块位置创建关联实体，默认均调用 `getAssociatedEntityType().create(level)`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlock.java:106-116`。
- `ProjectileBlock#getAssociatedEntityType()` 是炮弹方块到实体类型的抽象绑定点：`ProjectileBlock.java:155`。示例：`SolidShotBlock#getAssociatedEntityType()` 返回 `CBCEntityTypes.SHOT`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/solid_shot/SolidShotBlock.java:25-28`；`HEShellBlock#getAssociatedEntityType()` 返回 `CBCEntityTypes.HE_SHELL`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/he_shell/HEShellBlock.java:30-33`。
- 惰性炮弹 `InertProjectileBlock` 在 `getProjectile` 中把 tracer 从结构块、物品或方块实体拷贝到炮弹实体：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/InertProjectileBlock.java:30-49`。
- 带引信炮弹 `FuzedProjectileBlock` 在 `getProjectile` 中设置 tracer 和 fuze：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/FuzedProjectileBlock.java:49-75`。
- 流体炮弹 `FluidShellBlock#getProjectile(Level, ItemStack)` 还从 `CBCDataComponents.FLUID_CONTENT` 读取流体内容：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/fluid_shell/FluidShellBlock.java:81-88`。

### CBC 炮管发射路径

- Mounted big cannon 的开火入口是 `MountedBigCannonContraption#fireShot(ServerLevel, PitchOrientedContraptionEntity)`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/cannon_control/contraption/MountedBigCannonContraption.java:266-270`。
- 开火时 CBC 扫描已装填的炮管方块、推进药和炮弹结构，收集 `projectileBlocks`、累计 `chargesUsed`、`spread`、膛压/后坐等：`MountedBigCannonContraption.java:292-389`。
- 当炮弹结构完整时，CBC 通过 `ProjectileBlock#getProjectile(level, projectileBlocks)` 创建炮弹实体，并把炮弹自带的 `addedChargePower()` 加进装药：`MountedBigCannonContraption.java:367-373`；尾部补齐多方块炮弹时还有同样路径：`MountedBigCannonContraption.java:390-428`。
- CBC 计算炮口生成点和方向：`MountedBigCannonContraption.java:434-437`。
- 实际发射动作是 `projectile.setPos(spawnPos)`、`projectile.setChargePower(chargesUsed)`、`projectile.shoot(vec.x, vec.y, vec.z, chargesUsed, spread)`、`level.addFreshEntity(projectile)`：`MountedBigCannonContraption.java:455-464`。
- CBC 炮管路径没有在这里调用 `setOwner`；它给炮架/载具短时间加入不可命中列表：`MountedBigCannonContraption.java:459-462`。不可命中列表由 `AbstractCannonProjectile#addUntouchableEntity` 管理：`AbstractCannonProjectile.java:641-650`。
- Drop mortar 路径也用同样的 `setPos`、`setChargePower`、`shoot`、`addFreshEntity` 模式：`MountedBigCannonContraption.java:685-704`。

### 参数与散布

- 最小发射参数：炮弹来源 `ItemStack` 或 `ProjectileBlock`、世界 `Level`、生成坐标、发射方向、power/charge、spread。tracer、fuze、fluid 等弹药状态应留在 `ItemStack` 组件里，由 `ProjectileBlock#getProjectile(level, stack)` 迁移到实体。
- `AbstractCannonProjectile#setChargePower(float)` 在总基类中是空实现：`AbstractCannonProjectile.java:630`。Big cannon 子类通过 `AbstractBigCannonProjectile` 暴露 `addedChargePower()`、`minimumChargePower()`、`canSquib()`、`addedRecoil()`：`AbstractBigCannonProjectile.java:251-256`。
- 散布累计发生在 `MountedBigCannonContraption.PropellantContext#addPropellant`，读取推进药 power/recoil/stress/spread：`MountedBigCannonContraption.java:722-740`。炮管材料还能按炮管行程降低 spread：`MountedBigCannonContraption.java:279`、`:313-315`、`:364-365`。
- 未找到 CBC 提供“在任意位置生成并发射 CBC 炮弹”的单独公开 API。基于 CBC 自身发射代码，后续可封装一个很薄的适配器：`ProjectileBlock#getProjectile(level, itemStack)` -> `setPos` -> `setChargePower` -> `shoot` -> `addFreshEntity`。这是从现有代码路径归纳出的可行入口，不是 CBC 现成 API。

## 2. 炮弹类型与数据模型

### 注册类型

- CBC big cannon 实体注册在 `CBCEntityTypes`：`SHOT`、`HE_SHELL`、`SHRAPNEL_SHELL`、`BAG_OF_GRAPESHOT`、`AP_SHOT`、`TRAFFIC_CONE`、`AP_SHELL`、`FLUID_SHELL`、`SMOKE_SHELL`、`MORTAR_STONE`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCEntityTypes.java:66-75`。
- Drop mortar shell 单独注册并接入 munition properties handler：`CBCEntityTypes.java:77-83`。
- 爆裂/子弹片实体包括 `SHRAPNEL_BURST`、`FLAK_BURST`、`GRAPESHOT_BURST`、`FLUID_BLOB_BURST`：`CBCEntityTypes.java:85-111`。
- Autocannon projectile 包括 `AP_AUTOCANNON`、`FLAK_AUTOCANNON`、`MACHINE_GUN_BULLET`：`CBCEntityTypes.java:137-139`。
- Big cannon projectile helper `cannonProjectile` 注册渲染器、RPL 精确运动 tag，并调用 `MunitionPropertiesHandler.registerProjectileHandler(type, handler)`：`CBCEntityTypes.java:152-173`。
- 自带 big cannon 炮弹方块/物品注册在 `CBCBlocks`，并标记 `CBCTags.CBCItemTags.BIG_CANNON_PROJECTILES`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCBlocks.java:1011-1151`。tag 本身定义在 `.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCTags.java:177-182`。
- 基础炮弹物品 `ProjectileBlockItem` 读取 tracer 组件用于 tooltip：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlockItem.java:13-26`；`FuzedProjectileBlockItem` 读取 fuze 组件用于 tooltip：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/FuzedProjectileBlockItem.java:19-43`。

### JSON / DataPack 属性

- 弹药属性 handler 注册在 `CBCMunitionPropertiesHandlers`，big cannon inert/common shell/shrapnel/grapeshot/fluid/smoke/mortar/drop mortar、autocannon 和 burst 都有独立 handler：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCMunitionPropertiesHandlers.java:17-34`。
- `MunitionPropertiesHandler.ReloadListenerProjectiles` 从 datapack 路径 `munition_properties/projectiles` 读取 JSON，按 JSON 的 resource location 匹配 `EntityType`，再调用对应 handler：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/MunitionPropertiesHandler.java:31-66`。
- projectile/block propellant/item propellant handler 注册方法分别是 `registerProjectileHandler`、`registerBlockPropellantHandler`、`registerItemPropellantHandler`：`MunitionPropertiesHandler.java:135-151`。
- 属性同步到客户端通过 `writeBuf/readBuf/syncTo/syncToAll`：`MunitionPropertiesHandler.java:153-188`；reload listener 注册在 `CBCCommonEvents#onAddReloadListeners`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCCommonEvents.java:250-256`；datapack reload/player sync 时会同步 munition properties：`CBCCommonEvents.java:220-247`。
- 通用属性存储容器是 `PropertiesTypeHandler`，内部按 type 保存 properties，支持 JSON load、network load/write、`getPropertiesOf(type)`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/PropertiesTypeHandler.java:17-54`。

### 属性字段

- 弹道属性 `BallisticPropertiesComponent` 包含 `gravity`、`drag`、`quadratic_drag`、`durability_mass`、`penetration`、`toughness`、`deflection`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/components/BallisticPropertiesComponent.java:12-25`。
- 实体伤害属性 `EntityDamagePropertiesComponent` 包含 `entity_damage`、无敌/护甲忽略、击退等：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/components/EntityDamagePropertiesComponent.java:11-22`。
- 爆炸属性 `ExplosionPropertiesComponent` 支持统一 `explosive_power` 或拆分 `block_damaging_explosive_power` / `entity_damaging_explosive_power`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/components/ExplosionPropertiesComponent.java:11-23`。
- Big cannon 发射属性 `BigCannonProjectilePropertiesComponent` 包含 `added_charge_power`、`minimum_charge_power`、`can_squib`、`added_recoil`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/config/BigCannonProjectilePropertiesComponent.java:8-17`。
- `he_shell.json` 示例同时定义实体伤害、弹道、装药、引信、爆炸、穿透/韧性/偏转：`.external/CreateBigCannons/src/main/resources/data/createbigcannons/munition_properties/projectiles/he_shell.json:1-21`。

### 行为代码

- 实体通用命中伤害使用 `AbstractCannonProjectile#getEntityDamage`，默认返回 `indirectArtilleryFire(...)`：`AbstractCannonProjectile.java:445-447`；`indirectArtilleryFire` 构造 `CannonDamageSource`：`AbstractCannonProjectile.java:660-662`。
- Big cannon 方块穿透/反弹/停止在 `AbstractBigCannonProjectile#calculateBlockPenetration`：`AbstractBigCannonProjectile.java:155-244`。
- HE/AP shell 的爆炸行为在各自 `detonate` 中创建 `ShellExplosion`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/he_shell/HEShellProjectile.java:29-36`、`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ap_shell/APShellProjectile.java:29-36`。
- Shrapnel shell 先创建 `ShrapnelExplosion`，再 `CBCProjectileBurst.spawnConeBurst`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/shrapnel/ShrapnelShellProjectile.java:30-40`。
- Smoke shell 创建 `SmokeExplosion` 和 `SmokeEmitterEntity`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/smoke_shell/SmokeShellProjectile.java:29-39`。
- Fluid shell 创建 `FluidExplosion`，并按流体量生成 fluid blob burst：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/fluid_shell/FluidShellProjectile.java:45-66`。

### 兼容附属炮弹的事实入口

- CBC 本体识别炮弹的核心模型是“炮弹方块/物品 -> `ProjectileBlock` -> 关联 `EntityType` -> munition properties handler”。因此，对 CBC 附属炮弹最稳定的输入形态是 `ItemStack`，且其 `Item` 应是某个 `ProjectileBlock` 的 `BlockItem`。
- 如果附属只注册实体而没有继承/使用 `ProjectileBlock`，本次调研未找到 CBC 提供从任意 `EntityType` 反查炮弹物品和引信/组件的通用 API。

## 3. 视觉效果与倒计时可行性

### CBC 现有视觉

- 未找到 CBC 现成“信号弹、落点光柱、目标指示器、倒计时提示、空袭提示”实体或 API。检索关键词包括 `beacon`、`beam`、`flare`、`signal`、`marker`、`countdown`、`TextDisplay`、`Display`。
- CBC 有 projectile trail 和 flyby 声音：`AbstractBigCannonProjectile#tick` 在飞行时生成 trail smoke，并播放飞过声音：`AbstractBigCannonProjectile.java:61-99`。
- CBC 有曳光渲染：`BigCannonProjectileRenderer#render` 根据 `entity.hasTracer()` 渲染发光 billboard，并可 full-bright：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/BigCannonProjectileRenderer.java:32-85`。
- CBC 有 GUI overlay 注册入口，但当前只注册 entity goggles 和 gas mask：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCClientCommon.java:140-147`；NeoForge 侧挂到 `RegisterGuiLayersEvent`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCClientNeoForge.java:146-149`。

### 原版信标光柱

- 原版 `BeaconRenderer#render` 从 `BeaconBlockEntity#getBeamSections()` 读取信标光柱分段，然后调用 `renderBeaconBeam`：NeoForm sources zip `net/minecraft/client/renderer/blockentity/BeaconRenderer.java:26-44`。
- `BeaconRenderer.renderBeaconBeam(...)` 是 public static，参数包括 `PoseStack`、`MultiBufferSource`、beam texture、partialTick、gameTime、yOffset、height、color、beamRadius、glowRadius：`BeaconRenderer.java:52-64`。
- 光柱实际几何在 `renderBeaconBeam` 内生成，并使用 `RenderType.beaconBeam`：`BeaconRenderer.java:65-131`。
- 结论：原版没有“服务端在任意世界点创建信标光柱”的现成世界对象；但客户端自定义实体/方块渲染器可以在 marker 坐标系下调用或复制 `BeaconRenderer.renderBeaconBeam` 的渲染逻辑。

### 世界文字、图标和倒计时

- 原版 `EntityRenderer#render` 会触发 NeoForge `RenderNameTagEvent`，并按 `shouldShowName` 调用 `renderNameTag`：NeoForm sources zip `net/minecraft/client/renderer/entity/EntityRenderer.java:89-102`。
- `EntityRenderer#renderNameTag` 使用实体的 `EntityAttachment.NAME_TAG`、相机朝向、font draw 绘制世界名牌：`EntityRenderer.java:188-214`。一个自定义 marker entity 可以利用实体自定义名牌显示倒计时，但样式和图标能力有限。
- 原版 `Display.TextDisplay` 有同步字段 `DATA_TEXT_ID`、line width、背景、透明度、flag 等：NeoForm sources zip `net/minecraft/world/entity/Display.java:796-839`。其 `setText` 等方法是 private：`Display.java:849-887`；NBT 读取会解析 `"text"` 并调用 private setter：`Display.java:927-935`。因此 Java 代码直接动态设置 TextDisplay 文本不如自定义实体渲染直接。
- `Display.ItemDisplay` 有 `ItemStack` 同步字段和 NBT 读写，可作为图标显示参考：`Display.java:648-741`；同样缺少直接公开 setter。

## 4. 物品、投掷物与输入交互

### CBC 物品交互参考

- `TimedFuzeItem#use` 在服务端玩家右键时初始化 `CBCDataComponents.FUZE_TIMER`，并打开 `CBCMenuTypes.SET_TIMED_FUZE`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/fuzes/TimedFuzeItem.java:48-61`。
- `ProximityFuzeItem#use` 初始化 `CBCDataComponents.DETONATION_DISTANCE`，并打开 proximity fuze 菜单：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/fuzes/ProximityFuzeItem.java:128-140`。
- `ItemStackServerData` 把 `ItemStack` 上的 `DataComponentType<Integer>` 暴露为 `ContainerData`，用于菜单同步/修改：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/base/ItemStackServerData.java:7-30`。
- `ServerboundSetContainerValuePacket` 把客户端菜单值写回 `SimpleValueContainer`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/network/ServerboundSetContainerValuePacket.java:12-21`。
- CBC 数据组件注册直接使用 `BuiltInRegistries.DATA_COMPONENT_TYPE`，并设置 `persistent` 与 `networkSynchronized`。与本模组相关的参考有 `TRACER`、`FUZE`、`PROJECTILE`、`FUZE_TIMER`、`DETONATION_DISTANCE`、`ARMED`、`ACTIVATED`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCDataComponents.java:47-99`。

### 原版长按使用

- 原版 `Item#use` 默认在可食用时调用 `player.startUsingItem(usedHand)`；`onUseTick`、`finishUsingItem`、`getUseDuration`、`releaseUsing` 是持续使用/松开相关入口：NeoForm sources zip `net/minecraft/world/item/Item.java:123-127`、`:162-185`、`:317-329`。
- `ItemUtils.startUsingInstantly` 是标准的开始使用工具方法：NeoForm sources zip `net/minecraft/world/item/ItemUtils.java:9-13`。
- `BowItem#releaseUsing` 展示了长按后松开计算蓄力并发射 projectile 的模式：NeoForm sources zip `net/minecraft/world/item/BowItem.java:25-64`，`getUseDuration` 和 `getUseAnimation` 在 `BowItem.java:79-90`。
- `SpyglassItem#use` 展示了右键开始持续使用，`releaseUsing` 展示了松开停止：NeoForm sources zip `net/minecraft/world/item/SpyglassItem.java:35-57`。

### 原版投掷物

- 原版投掷物基类 `ThrowableItemProjectile` 继承 `ThrowableProjectile`，用同步 `ItemStack` 表示外观/物品：NeoForm sources zip `net/minecraft/world/entity/projectile/ThrowableItemProjectile.java:13-44`，NBT 存取在 `ThrowableItemProjectile.java:46-63`。
- `SnowballItem#use` 创建 `Snowball`、`setItem(itemstack)`、`shootFromRotation(...)`、`level.addFreshEntity(...)`，然后消耗物品：NeoForm sources zip `net/minecraft/world/item/SnowballItem.java:23-46`。
- `EnderpearlItem#use` 同样创建 `ThrownEnderpearl`、设置冷却、发射并加入世界：NeoForm sources zip `net/minecraft/world/item/EnderpearlItem.java:20-44`。
- `Snowball#onHit` 命中后在服务端广播事件并 discard：NeoForm sources zip `net/minecraft/world/entity/projectile/Snowball.java:70-77`。
- `ThrownEnderpearl#onHit` 命中后处理粒子、传送和 discard：NeoForm sources zip `net/minecraft/world/entity/projectile/ThrownEnderpearl.java:49-113`。
- CBC 未找到自定义雪球/末影珍珠式 throwable，可直接参考原版路径实现战略配备信标投掷。

### 客户端输入

- CBC 注册两个 key mapping：`PITCH_MODE` 和 `FIRE_CONTROLLED_CANNON`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCClientCommon.java:80-83`，NeoForge 客户端注册在 `.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCClientNeoForge.java:82-87`。
- CBC 客户端 tick 读取 `mc.player.input.left/right/up/down` 并发网络包：`CBCClientCommon.java:149-164`；鼠标点击和滚轮事件也走 NeoForge input event：`CBCClientCommon.java:172-195`、`CBCClientNeoForge.java:90-98`。
- 未找到 CBC 已实现 WASD 指令序列输入。后续可参考 CBC 的 client tick/input event + server packet 模式，但指令序列、前缀规则和 UI overlay 需要本模组实现。

## 5. 兼容性与注册系统

- CBC 主初始化在 `CreateBigCannons.init()` 中依次注册 blocks/items/block entities/entity types/menu/fluids/recipes/network：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CreateBigCannons.java:48-67`。
- NeoForge 入口 `CreateBigCannonsNeoForge` 同时使用 Registrate 和 NeoForge `DeferredRegister`。粒子、配方 serializer/type 走 `DeferredRegister`：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CreateBigCannonsNeoForge.java:44-60`；自定义 registry 和 DataComponents 初始化在 `RegisterEvent`：`CreateBigCannonsNeoForge.java:104-116`。
- NeoForge 事件绑定集中在 `CBCCommonNeoForgeEvents.register`，包括 tick、datapack sync、reload listener、item use on block 等：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/CBCCommonNeoForgeEvents.java:35-44`、`:70-80`。
- terrain damage 扩展点是 `ProjectileDamageEvent`，可取消：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/events/ProjectileDamageEvent.java:8-23`；CBC 命中方块时通过 `ProjectileDamageHooks.canDamageTerrain` 发送该事件：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/ProjectileDamageHooks.java:13-19`。
- 看起来较稳定的附属交互入口：Minecraft/NeoForge 注册表、`ItemStack`、`ProjectileBlock`/`FuzedProjectileBlock`、CBC 的 munition properties handler 注册路径和 datapack JSON。
- 风险较高的内部实现：`MountedBigCannonContraption#fireShot` 的整段炮管扫描、故障、squib、后坐和 contraption 细节；它适合作为行为参考，不适合直接依赖整段流程来做空中打击。

## 6. 未找到或待验证项

- 未找到 CBC 任意位置发射炮弹的公开高级 API。已找到的事实路径是炮管内部手动 `setPos`、`setChargePower`、`shoot`、`addFreshEntity`。
- 未找到 CBC 战备式 WASD 指令输入、前缀规则或呼叫面板。
- 未找到 CBC 信号弹/落点光柱/世界倒计时提示。原版可参考 beacon beam 和 entity name tag，实际 marker 表现需后续设计。
- 待验证：手动发射 CBC 炮弹时是否需要显式设置 owner 才能满足权限、战绩、伤害归因或防误伤需求。CBC 炮管路径没有设置 owner，只设置短时间 untouchable。
- 待验证：Display entity 在 Java 模组代码中动态更新文本/图标是否值得使用。源码显示关键 setter 是 private，可能不如自定义 marker entity renderer 直接。
- 待验证：附属炮弹如果不是 `ProjectileBlock` 模型，只注册 `AbstractBigCannonProjectile` 实体，本模组是否要额外支持。CBC 当前未提供从实体类型反查可发射 `ItemStack` 的通用路径。

## 7. 代码索引

- 炮弹总基类：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/AbstractCannonProjectile.java`
- Big cannon 炮弹基类：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile.java`
- 带引信 big cannon 炮弹：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/FuzedBigCannonProjectile.java`
- 炮弹方块/物品入口：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlock.java`、`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/ProjectileBlockItem.java`
- CBC 炮管开火参考：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/cannon_control/contraption/MountedBigCannonContraption.java`
- 实体注册：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCEntityTypes.java`
- 炮弹方块注册：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCBlocks.java`
- 数据组件：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/index/CBCDataComponents.java`
- 弹药属性 reload/sync：`.external/CreateBigCannons/src/main/java/rbasamoyai/createbigcannons/munitions/config/MunitionPropertiesHandler.java`
- 原版信标光柱：NeoForm sources zip `net/minecraft/client/renderer/blockentity/BeaconRenderer.java`
- 原版世界名牌：NeoForm sources zip `net/minecraft/client/renderer/entity/EntityRenderer.java`
- 原版投掷物：NeoForm sources zip `net/minecraft/world/entity/projectile/ThrowableItemProjectile.java`、`Snowball.java`、`ThrownEnderpearl.java`
- 原版物品使用：NeoForm sources zip `net/minecraft/world/item/Item.java`、`ItemUtils.java`、`BowItem.java`、`SpyglassItem.java`、`SnowballItem.java`、`EnderpearlItem.java`
