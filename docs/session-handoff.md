# CBC-Stratgems 会话交接记录

## 当前状态

- 已完成玩法设想、CBC 源码调研、技术设计和实现计划。
- 已创建 NeoForge 1.21.1 Gradle 模组骨架。
- 当前 `modid`：`cbc_stratgems`。
- 当前 Java 根包：`com.aeroseira.cbcstratgems`。
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
- `src/main/java/com/aeroseira/cbcstratgems/CBCStratgems.java`：当前最小 mod 入口。

## 已验证

在本次远程会话中已通过：

```bash
./gradlew clean build
```

构建产物：

```text
build/libs/cbc_stratgems-0.1.0+mc.1.21.1.jar
```

注意：本次远程环境的 Java/Gradle 访问部分 Maven 仓库时出现 TLS/代理问题，曾在远程机器的本地 `~/.m2` 缓存中补齐 NeoForge 工具依赖。该缓存不属于仓库状态。迁移到本地机器后应优先直接执行 `./gradlew clean build`；如果依赖下载失败，再按本地网络环境配置 `~/.gradle/gradle.properties` 代理，不要把个人代理配置提交进仓库。

## 本地迁移步骤

```bash
git clone git@github.com:Aero-Seira/CBC-Stratgems.git
cd CBC-Stratgems
./gradlew clean build
```

Windows 可使用：

```bat
gradlew.bat clean build
```

IDEA 中直接 Open 本仓库，让 IDEA 作为 Gradle 项目导入即可，不需要新建项目后合并。

## 下一个代码阶段

建议从 `docs/implementation-plan.md` 的 M2 注册层开始：

- `registry/ModItems.java`
- `registry/ModDataComponents.java`
- `registry/ModSoundEvents.java`
- `registry/ModKeyMappings.java`
- `registry/ModCreativeTabs.java`

第一轮只做注册和最小可编译空壳，不直接实现完整玩法状态机。随后再做呼叫装置、许可证、数据驱动战备、输入 overlay、信标实体和 CBC 发射适配。

## 新会话建议提示

```text
请先阅读 AGENT.md、docs/session-handoff.md、design.md、docs/implementation-plan.md 和 research/cbc-source-map.md。
当前仓库已经有 NeoForge 1.21.1 Gradle 骨架，modid 为 cbc_stratgems。
请从 M2 注册层开始实现，保持小步提交，并先确保 ./gradlew build 通过。
```
