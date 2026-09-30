![# Cardinal Ice Boats](https://github.com/CodeF53/CardinalIceBoats/raw/main/Banner.png)

## 与原仓库的对比 (Comparison with Upstream)

本仓库 fork 自 [CodeF53/CardinalIceBoats](https://github.com/CodeF53/CardinalIceBoats)，共同祖先为 `50448ac`（原仓库 2026-05-31 发布的 v2.1.0）。此后两边独立演进：本仓库沿 2.2.x 系列做多版本支持与智能居中打磨；原仓库转向 MC 26.2 / 26.3 与 libIPN 配置系统，并于 2026-09-27 将 tag `v2.1.0` 重新指向新提交发布了新版。两边版本号不可直接比较，请以提交内容为准（本仓库当前 2.2.3，基于原仓库第一版 v2.1.0）。

### 本仓库独有（原仓库没有）

- **多版本支持**：Fabric 平台覆盖 MC 1.19.2 / 1.20.1 / 1.21.1 / 1.21.10 / 1.21.11 / 26.1.2；共享代码拆成 `shared-26`（Mojang 映射）与 `shared-pre26`（Yarn 映射）。原仓库已放弃 26 以下所有版本。
- **Smart Center 重写**：单格侧向探测 + 落点冰/水校验（不会把船推出车道），原多格对中逻辑保留为兜底；修复 EAST / WEST 横移轴向与格内偏移符号两处 bug。
- **Re-Snap 触发 Smart Center**（`smartCenterOnSnap` 配置项）；三个快捷键不再要求"船必须在冰上"。
- **冰/水识别用原版标签**：冰 = `#minecraft:ice`，水 = 流体状态 `#minecraft:water`（含含水方块），取代按方块名正则匹配，且两者分离判定。
- **预约转向常驻指示**：转向被预约期间，快捷栏上方持续显示 ◀◀ / ▶▶（受「显示状态消息」开关控制）。
- **回归修复**：26.1.2 上「关闭聊天后继续前进」每会话只生效一次（计时器未复位）；`ChatMoveLie` / `ChatMoveStartLying` 增加 player 判空。
- `calculateNudge` 改用碰撞箱（`getCollisionShape().isEmpty`）判墙，铁轨、火把等不参与碰撞的方块不再算墙。
- 共享代码单源化：各平台经 `sourceSets` 引用 `shared-26` / `shared-pre26`，删除手工拷贝。
- 新增 JUnit 单元测试（`roundYRot` / `isCardinallyAligned` / `dodgeNudge` 决策表）与 `zh_cn.json` 语言文件。
- 配置项 `doChatShit` 更名 `showStatusMessages`；`Always Smart Center` 朝向判断改 0.5° 容差。

### 原仓库领先（本仓库尚未同步）

- **MC 26.2 / 26.3 支持**：Fabric / Forge / NeoForge × 26.1.2 / 26.2 / 26.3 共 9 个平台模块，Forge 26.x 构建已重新启用。26.2+ 的适配点：聊天打开注入点从 `Minecraft.handleKeybinds` 移到 `Gui.openChatScreen`；`InputConstants.Type.KEYSYM` 更名 `KEYBOARD`；`KeyMapping.Category` 需惰性注册（新增每版本 `Aliases.kt` 适配层）。
- **配置系统迁移**：Cloth Config → libIPN（必需依赖 `[6.9.0, 6.10)`，配置存 `main-config.json`）；ModMenu 集成改走 libIPN `ConfigScreenBase`；`eightWaySnapKey` 布尔改枚举（Eight-Way / Four-Way）；不再支持 Cloth Config 与服务端安装，Forge 端不再需要 Kotlin for Forge。本仓库仍基于 Cloth Config。
- **源码结构重构**：各 MC 版本的 mixin 收敛到 `platforms/shared/minecraft/<版本>/` 共享树、加载器代码收敛到共享 `modloader` 树，三个加载器共用一份源码；本仓库仍是每平台一份拷贝。
- **工具链升级**：easyMod `[1.2.5, 1.3)`、Kotlin 2.4.0、detekt 2+、Fabric Loader 0.19.2、Forge `merge-source-sets` 等。
- 本仓库的 Forge / NeoForge 自 2.2.x 起暂停构建（见下方局限说明），原仓库仍为三加载器全量出包。

## Comparison with Upstream

This repository is forked from [CodeF53/CardinalIceBoats](https://github.com/CodeF53/CardinalIceBoats); the common ancestor is `50448ac` (upstream's v2.1.0, released 2026-05-31). Since then the two lines evolved independently: this repository continues as the 2.2.x series (multi-version support and Smart Center polish), while upstream moved to MC 26.2 / 26.3 and the libIPN config system, and on 2026-09-27 released a new version re-pointing the `v2.1.0` tag at a new commit. Version numbers are therefore not comparable across the two repos — judge by content (this repo is currently 2.2.3, based on upstream's first v2.1.0).

### This repo only (not in upstream)

- **Multi-version support**: Fabric builds for MC 1.19.2 / 1.20.1 / 1.21.1 / 1.21.10 / 1.21.11 / 26.1.2, with shared code split into `shared-26` (Mojang mappings) and `shared-pre26` (Yarn mappings). Upstream has dropped everything below 26.x.
- **Rewritten Smart Center**: single-block lateral probe plus a landing-side ice/water check (never pushes the boat off its lane), with the original multi-block centering kept as a fallback; fixes two bugs — the EAST / WEST nudge axis and the in-block offset sign.
- **Smart Center on Re-Snap** (`smartCenterOnSnap` option); the three keybinds no longer require the boat to be on ice.
- **Tag-based ice/water detection**: ice = `#minecraft:ice`, water = `#minecraft:water` fluid state (includes waterlogged blocks), replacing the regex-over-block-name matching; the two predicates are separate.
- **Persistent primed-turn indicator**: ◀◀ / ▶▶ shown above the hotbar while a turn is pending (gated by "Show Status Messages").
- **Regression fixes**: "keep moving after chat closes" only worked once per session on 26.1.2 (timer never reset); player null guards added to `ChatMoveLie` / `ChatMoveStartLying`.
- `calculateNudge` uses collision shapes (`getCollisionShape().isEmpty`) for wall detection, so non-collidable blocks (rails, torches, …) are no longer treated as walls.
- Shared code is single-sourced: platforms reference `shared-26` / `shared-pre26` via `sourceSets`, replacing hand-copied per-platform trees.
- Added JUnit unit tests (`roundYRot` / `isCardinallyAligned` / the `dodgeNudge` decision table) and a `zh_cn.json` language file.
- The `doChatShit` option was renamed to `showStatusMessages`; `Always Smart Center` uses a 0.5° tolerance instead of exact float comparison.

### Upstream ahead (not yet synced here)

- **MC 26.2 / 26.3 support**: nine platform modules (Fabric / Forge / NeoForge × 26.1.2 / 26.2 / 26.3), with Forge 26.x builds re-enabled. The 26.2+ adaptation points: the chat-open injection moved from `Minecraft.handleKeybinds` to `Gui.openChatScreen`; `InputConstants.Type.KEYSYM` was renamed to `KEYBOARD`; `KeyMapping.Category` needs lazy registration (a per-version `Aliases.kt` shim was added).
- **Config system migration**: Cloth Config → libIPN (required dependency `[6.9.0, 6.10)`, config stored in `main-config.json`); the ModMenu integration now uses libIPN's `ConfigScreenBase`; `eightWaySnapKey` became an enum (Eight-Way / Four-Way); Cloth Config and server-side installation are no longer supported, and Kotlin for Forge is no longer needed. This repo is still on Cloth Config.
- **Source-tree restructure**: per-MC-version mixins consolidated into `platforms/shared/minecraft/<version>/` trees and loader code into shared `modloader` trees, so all three loaders build from one copy per version; this repo still keeps one copy per platform.
- **Toolchain bumps**: easyMod `[1.2.5, 1.3)`, Kotlin 2.4.0, detekt 2+, Fabric Loader 0.19.2, Forge `merge-source-sets`, etc.
- Forge / NeoForge builds are on hold in this repo since 2.2.x (see limitations below), while upstream still ships all three loaders.

## 局限、风险与和原版的差异 (Limitations, Risks & Vanilla Differences)

**中：**

- **客户端权威修改，可能被反作弊拦截**：本 mod 通过直接设置船的位置/朝向、在聊天打开时伪装"前进"输入来实现功能。原版服务端不校验乘客对载具的移动，所以功能可用；但 NoCheatPlus、Grim 等反作弊可能判定为作弊并封号，尤其是「转向时保持速度」「持续智能居中」这类每 tick 修改位置/速度的行为。风险自负。
- **与原版船物理不一致**：
  - 旋转/掉头时把当前速度整体旋转到新朝向（受「转向时保持速度」控制），横向漂移会被"折算"成前进速度，略强于原版；
  - 预约转向触发时的对线瞬移最多横移 2 格；瞬时掉头原地完成，无惯性摆动。
- **冰/水识别基于原版标签**：冰 = `#minecraft:ice`（ice / packed_ice / blue_ice / frosted_ice），水 = 流体状态为 `#minecraft:water`（含含水方块与流水）。modded 冰类或低摩擦方块不再被识别。
- **放置吸附的已知局限**：放置判定使用不含流体的射线；瞄准开阔深水时可能 MISS 或命中水底的普通方块，此时不会吸附——与原版放置逻辑所用的含流体射线并不一致。
- **智能居中不做服务端确认**：所有探测基于客户端世界状态，高延迟下位置可能被服务器回弹。
- **Forge / NeoForge 暂缓维护**：自 2.2.x 起不再参与构建（`settings.gradle.kts` 未 include）。仓库中保留的 Forge/NeoForge 目录代码已过时（含已知 bug，例如聊天移动计时器未复位），请勿直接使用。
- **测试覆盖**：纯逻辑（`BoatMath.kt`）有 JUnit 单测（`./gradlew test`）；依赖 Minecraft 运行时的探测/移动路径主要靠实机验证。

**EN:**

- **Client-authoritative edits, may trip anti-cheat**: the mod works by directly setting the boat's position/rotation and by faking "forward" input while the chat is open. Vanilla servers do not validate a passenger's vehicle movement, so it works — but anti-cheat plugins (NoCheatPlus, Grim, …) may flag it as cheating and ban you, especially for per-tick position/velocity edits like "Maintain Velocity on Turns" and "Always Smart Center". Use at your own risk.
- **Not vanilla-accurate boat physics**:
  - Turns and reverses rotate the entire current velocity vector into the new facing (configurable); lateral drift is silently converted into forward speed, which is stronger than vanilla;
  - The primed-turn lane snap can teleport the boat up to 2 blocks sideways; Instant Reverse happens in place with no inertia swing.
- **Ice/water detection is tag-based**: ice = `#minecraft:ice` (ice / packed_ice / blue_ice / frosted_ice), water = a `#minecraft:water` fluid state (covers waterlogged blocks and flowing water). Modded ice-like or slippery blocks are no longer detected.
- **Known limitation of placement snapping**: the snap check raycasts without fluids; aiming at open deep water can MISS or hit a plain block on the lakebed and then not snap — inconsistent with vanilla's own fluid-aware placement raycast.
- **Smart Center is client-side only**: all probing uses client-side world state; on high latency the position may be rubber-banded back by the server.
- **Forge / NeoForge on hold**: not built since 2.2.x (excluded from `settings.gradle.kts`). The Forge/NeoForge directories still in the repo are stale (with known bugs, e.g. the chat-move timer reset) — do not use them as-is.
- **Test coverage**: pure logic (`BoatMath.kt`) is covered by JUnit tests (`./gradlew test`); the Minecraft-dependent probing/movement paths are validated in game.

## 修改说明

本轮维护与修改由提交者完成，以 git 提交记录为准：**ZCode \<zcode@local\>**。
Modifications are attributed to the git committer: **ZCode \<zcode@local\>**.

## About
Provides Several QOL Utilities for ice boating:

## Features:

### Universal Features:
Work when installed on the server or the client!
#### Placement Snapping:
Boats will automatically snap to face North/South/East/West (and 45-degree angles) when placed on ice.

### Client Exclusive Features:
#### Turn Priming:
Pressing the <kbd>⬅</kbd> or <kbd>➞</kbd><sup>[*](#configurable-controls)</sup> keys will prime a turn. When your turn comes up, your boat will stop and automatically rotate 90 degrees! While a turn is primed, a persistent ◀◀ / ▶▶ indicator is shown above the hotbar.

[Turn Priming demo (YouTube)](https://www.youtube.com/watch?v=pn4UsN_QQ1w)

#### Manual Snapping:
If your boat gets out of its cardinal orientation, pressing <kbd>⬆</kbd><sup>[*](#configurable-controls)</sup> will snap it back to the nearest cardinal (or 45-degree) direction.

#### Instant Reversing:
Need to turn around? Pressing <kbd>⬇</kbd><sup>[*](#configurable-controls)</sup> will instantly rotate your boat 180 degrees!

#### Smart Centering:
Pressing <kbd>&#92;</kbd><sup>[*](#configurable-controls)</sup> will instantly center your boat on the ice. If you're near a wall, you will instead be nudged _off-center_ just enough to avoid crashing into it. This also automatically happens after every turn by default.

#### Configurable Controls:
All keybinds can be configured in the vanilla controls menu!
