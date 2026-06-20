# CBC-Stratagems 会话交接记录

## 当前状态

- 已完成玩法设想、CBC 源码调研、技术设计和实现计划。
- 已创建 NeoForge 1.21.1 Gradle 模组骨架。
- 已完成 M2 注册基础项空壳：物品、DataComponents、EntityTypes、SoundEvents、KeyMapping、Creative Tab、packet 注册入口。
- 已通过本地 `runClient` 验证创造栏显示和控制设置按键注册。
- 战备面板默认入口是主手持呼叫装置右键长按；`open_stratagem_panel` KeyMapping 只作为可选辅助入口，默认未绑定。
- 已完成 M3 代码层：战备 JSON 数据模型、reload listener、单项/前缀校验、服务端 registry、登录和 `/reload` 定义摘要同步、客户端定义缓存。
- M3 调试战备 JSON 和其他资源/数据文件由用户自行维护；本阶段未由代理写入 `src/main/resources`。
- 已完成 M4 代码层：玩家解锁/冷却 attachment、玩家数据同步 packet、客户端玩家数据缓存、主手装置 + 副手许可证解锁流程。
- M4 尚未做游戏内手动验证；已通过 `./gradlew build`。
- 已完成 M5/M6 最小输入闭环代码层：主手装置右键长按创建服务端输入 session、客户端 WASD 边沿发送方向包、服务端按前缀/完整指令匹配、命中后写入 `device_mode=beacon` 和 `selected_stratagem`、客户端基础 HUD 文本反馈与移动输入抑制。
- M5/M6 尚未做游戏内手动验证；overlay 仍是临时文本 HUD，尚未接 UI theme 纹理；`open_stratagem_panel` 可选辅助键还未接服务端打开包。
- 已完成 M7 信标链路代码层：`BEACON` 模式装置右键投掷 `StratagemBeaconProjectile`、命中后生成 `StratagemMarkerEntity`、marker 通过露天/阻隔校验后启动玩家战备冷却，倒计时结束后提示炮击调度尚未实现。非露天长按呼叫装置会打开输入 HUD，但显示露天提示并阻止方向输入。
- M7 已经过游戏内输入/信标流程验证并做过多轮小修；marker renderer、名称/倒计时和光柱已有第一版。CBC 炮击调度尚未实现。
- 当前 `modid`：`cbc_stratagems`。
- 当前 Java 根包：`com.aeroseira.cbcstratagems`。
- 目标依赖版本：
  - Minecraft `1.21.1`
  - NeoForge `21.1.225`
  - Create `6.0.10-280`
  - Create Big Cannons `5.11.6+mc.1.21.1`
  - Ritchie's Projectile Library `2.1.2`

## 关键文件

- `AGENT.md`：仓库工作规则和阶段门槛。
- `docs/stratagem-concept.md`：玩法设想。
- `research/cbc-source-map.md`：CBC 源码调研结论和证据。
- `design.md`：技术设计文档。
- `docs/implementation-plan.md`：代码落地计划。
- `build.gradle`、`gradle.properties`、`settings.gradle`：Gradle/NeoForge 骨架。
- `src/main/java/com/aeroseira/cbcstratagems/CBCStratagems.java`：mod 入口和注册层挂载。
- `src/main/java/com/aeroseira/cbcstratagems/registry/`：注册层，包含 items、data components、attachments、packets 等。
- `src/main/java/com/aeroseira/cbcstratagems/player/`：M4 玩家解锁/冷却数据和同步管理。
- `src/main/java/com/aeroseira/cbcstratagems/stratagem/input/`：M5/M6 服务端战备输入 session、匹配和状态同步。
- `src/main/java/com/aeroseira/cbcstratagems/client/StratagemInputClient.java`：M6 客户端 WASD 输入采集、移动抑制和临时 HUD。
- `src/main/java/com/aeroseira/cbcstratagems/entity/`：M7 信标投掷实体和 marker 倒计时实体。

## 已验证

在本次远程会话中已通过：

```bash
./gradlew clean build
```

构建产物：

```text
build/libs/cbc_stratagems-0.1.0+mc.1.21.1.jar
```

注意：本次远程环境的 Java/Gradle 访问部分 Maven 仓库时出现 TLS/代理问题，曾在远程机器的本地 `~/.m2` 缓存中补齐 NeoForge 工具依赖。该缓存不属于仓库状态。迁移到本地机器后应优先直接执行 `./gradlew clean build`；如果依赖下载失败，再按本地网络环境配置 `~/.gradle/gradle.properties` 代理，不要把个人代理配置提交进仓库。

## 本地迁移步骤

```bash
git clone git@github.com:Aero-Seira/CBC-Stratagems.git
cd CBC-Stratagems
./gradlew clean build
```

Windows 可使用：

```bat
gradlew.bat clean build
```

IDEA 中直接 Open 本仓库，让 IDEA 作为 Gradle 项目导入即可，不需要新建项目后合并。

## 下一个代码阶段

建议先做 M4-M6 的游戏内手动验证和小修：

- 准备一个调试战备 JSON，并用带 `license_stratagem` DataComponent 的许可证解锁。
- 露天环境下主手长按装置，输入 WASD 指令，确认装置切到 `BEACON` 模式并绑定 `selected_stratagem`。
- 验证未解锁、无匹配、地下/有天花板反馈。
- 验证右键按住期间 GUI 持续显示，松开右键或呼叫装置离开主手时立即关闭。
- 如果输入闭环稳定，再把临时 HUD 替换为 UI theme 资源绘制。

随后再做 CBC 发射适配、marker 视觉细节调优和完整火力链路验证。

## 新会话建议提示

```text
请先阅读 AGENT.md、docs/session-handoff.md、design.md、docs/implementation-plan.md 和 research/cbc-source-map.md。
当前仓库已经有 NeoForge 1.21.1 Gradle 骨架，modid 为 cbc_stratagems。
M2 注册层已完成并通过 runClient 验证创造栏和按键注册；战备面板默认由手持呼叫装置右键长按打开。
M3 代码层已完成，资源/数据 JSON 由用户自行维护。
M4 玩家数据与许可证代码层已完成并通过 ./gradlew build，但还需游戏内手动验证。
M5/M6 最小输入闭环代码层已完成并通过 ./gradlew build；overlay 目前是临时文本 HUD。
M7 信标投掷、marker 生成、露天/阻隔校验、倒计时和冷却启动代码层已完成并通过 ./gradlew build；输入/信标流程已做过游戏内验证。
请先验证露天/阻隔拒绝逻辑，再继续 CBC 发射适配、marker 视觉细节调优和完整火力链路验证。
```
