![# Cardinal Ice Boats](https://github.com/CodeF53/CardinalIceBoats/raw/main/Banner.png)

## 近期改动 (commit `7766b45` 起，至本仓库当前 HEAD)

中 / EN — 涵盖 `7766b45 fix: use collision shape instead of air check in calculateNudge, restore multi-version build` 到本仓库当前 HEAD（含）之间的所有提交。

- **最新提交 — 第二轮打磨（详见 git log）**
  - 冰/水识别改用原版标签：冰 = `#minecraft:ice`，水 = 流体状态为 `#minecraft:water`（含含水方块）。不再按方块名做正则匹配。
  - Smart Center 侧向闪避增加落点检查：目标侧格若无冰（船下一格）/ 水（与船同高），视为有墙，不会把船推出车道。
  - `Always Smart Center` 的朝向判断从浮点精确比较改为 0.5° 容差。
  - 预约转向新增常驻指示：转向被预约期间，快捷栏上方持续显示 ◀◀ / ▶▶ 提示（随「显示状态消息」开关）。
  - 修复 26.1.2 上「关闭聊天后继续前进」只在会话中第一次生效的回归（计时器未复位）。
  - `ChatMoveLie` / `ChatMoveStartLying` 增加 player 判空；Re-Snap 的速度处理改由「转向时保持速度」配置统一控制。
  - 共享代码改为单一来源：各平台通过 `sourceSets` 直接引用 `shared-26` / `shared-pre26`，删除各平台 `src/shared` 手工拷贝。
  - 新增 JUnit 单元测试（`roundYRot` / `isCardinallyAligned` / `dodgeNudge` 决策表）；新增 `zh_cn.json` 语言文件。
  - 配置项 `doChatShit` 更名为 `showStatusMessages`（旧配置文件中的该项会回到默认值，需重新设置）。

- **`7766b45` — 修复探测方式 & 恢复多版本构建**
  - `calculateNudge` 改用 `getCollisionShape().isEmpty` 判定，取代原先的 `!is AirBlock` 检查，从而把铁轨、火把等不参与碰撞的方块从"墙"中排除。
  - 恢复 MC 1.19.2 – 1.21.11 的 Fabric 平台模块；将共享代码拆分为 `shared-26`（Mojang 命名空间）与 `shared-pre26`（Yarn 命名空间）。
  - Gradle Wrapper 升到 9.5.1，foojay-resolver 升到 1.+。

- **`ba4a319` — Re-Snap 触发智能居中，键位放开冰面限制，冰/水分离识别**
  - 在 Manual Snap 的 Re-Snap 操作后自动执行 Smart Center（可配置开关）。
  - 三个键位不再要求"船必须在冰上"才生效。
  - 冰面和水体识别逻辑分离。

- **`2d26ad7` — 1.19.3+ 兼容：使用 `passengerList.firstOrNull()` 代替 `primaryPassenger`**

- **`ece7fa5` / `e077a08` / `592439b` — 2.2.0 / 关闭 SNAPSHOT 后缀 / 2.2.1**
  - 版本号 2.1.0 → 2.2.0。
  - `modVersion.release` 设为 `true`，构建产物不再带 `-SNAPSHOT` 后缀。
  - Turn Priming 的扫描偏移位置由 3 个扩展为 5 个，再精简到 4 个。版本号升到 2.2.1。

- **`a42dee4` — 重写 `smartCenter` 为单格侧向探测**
  - 探测范围从"前方多格"收敛到"前 1 格 + 前上 1 格"两格；左右各 1 格同样只测前两格。
  - 仅一侧空 → 沿该方向横移 ±0.2。
  - 两侧都空或都堵 → 退回用船在格内的位置 + 船头偏航做兜底决策。
  - 决定横移时直接基于船当前位置移动，不再先对到车道中心。

- **`08a069a` — 版本号 2.2.2（snapshot 模式）**
  - `modVersion.release` 改为 `false`，`changelog.md` 同步新增 2.2.2 条目。

- **`36313b7`（fix on top of `08a069a`）— 修复 `smartCenter` 两个 bug**
  - 原算法作为兜底：当新算法决定无横移，或前方两格均无碰撞时，回落到原有多格对中逻辑（`calculateNudge` 已被恢复）。
  - 横移应用改为向量形式 `boat.pos + nudge * (-lx, -lz)`，保证 EAST / WEST 朝向时方向也正确（原实现里 EAST/WEST 写成了沿 z 加减，方向反了）。
  - 兜底分支中"船在格内偏左/偏右"的符号也修正：左侧 → `nudge = -0.2`，右侧 → `+0.2`。

## Recent changes (commits from `7766b45` up to current HEAD, inclusive)

ZH / EN — covers everything from `7766b45 fix: use collision shape instead of air check in calculateNudge, restore multi-version build` through this repository's current HEAD.

- **Latest commit — second polish pass (see git log for details)**
  - Ice/water detection now uses vanilla tags: ice = `#minecraft:ice`, water = fluid state of `#minecraft:water` (includes waterlogged blocks). The regex-over-block-name matching is gone.
  - Smart Center's lateral dodge now validates the landing side: if the target cell has no ice (one block below the boat) or water (level with the boat), it is treated like a wall so the boat is never pushed off the lane.
  - `Always Smart Center` uses a 0.5° tolerance instead of an exact float comparison.
  - Primed turns now show a persistent ◀◀ / ▶▶ indicator above the hotbar while a turn is pending (gated by "Show Status Messages").
  - Fixed a regression on 26.1.2 where "keep moving after chat closes" only worked the first time per session (timer was never reset).
  - `ChatMoveLie` / `ChatMoveStartLying` gained player null guards; Re-Snap's velocity handling now follows the "Maintain Velocity on Turns" config like every other action.
  - Shared code is single-sourced: platforms reference `shared-26` / `shared-pre26` via `sourceSets`, and the hand-copied per-platform `src/shared` trees are deleted.
  - Added JUnit unit tests (`roundYRot` / `isCardinallyAligned` / the `dodgeNudge` decision table) and a `zh_cn.json` language file.
  - The `doChatShit` option was renamed to `showStatusMessages` (existing config files fall back to the default; set it again once).

- **`7766b45` — fix detection method & restore the multi-version build**
  - `calculateNudge` now uses `getCollisionShape().isEmpty` instead of `!is AirBlock`, so non-collidable blocks (rails, torches, …) are no longer treated as walls.
  - Restore Fabric platform modules for MC 1.19.2 – 1.21.11; split shared code into `shared-26` (Mojang mappings) and `shared-pre26` (Yarn mappings).
  - Bump Gradle Wrapper to 9.5.1 and foojay-resolver to `1.+`.

- **`ba4a319` — Smart Center on Re-Snap; keybinds no longer require ice; separate ice/water detection**
  - After a Manual Snap (Re-Snap), Smart Center now runs automatically (configurable).
  - The three keybinds no longer require the boat to be on ice.
  - Ice and water detection are now separate predicates.

- **`2d26ad7` — 1.19.3+ compatibility: use `passengerList.firstOrNull()` instead of `primaryPassenger`**

- **`ece7fa5` / `e077a08` / `592439b` — 2.2.0 / disable SNAPSHOT suffix / 2.2.1**
  - Bump version 2.1.0 → 2.2.0.
  - Set `modVersion.release = true`; built artifacts no longer carry a `-SNAPSHOT` suffix.
  - Turn Priming's lateral scan offsets grow from 3 to 5 positions, then are trimmed back to 4. Version bumped to 2.2.1.

- **`a42dee4` — rewrite `smartCenter` to a single-block lateral probe**
  - The probe shrinks from "many blocks ahead" to just "front block + front-upper block"; the two side lanes are probed the same way.
  - If only one side is open → nudge that way by `±0.2`.
  - If both sides are open or both are blocked → fall back to in-block position and bow drift.
  - When a nudge is decided, the boat is moved from its current position; the lane-center snap is skipped.

- **`08a069a` — version 2.2.2 (snapshot mode)**
  - Set `modVersion.release = false`; add a `2.2.2` entry in `changelog.md`.

- **`36313b7` (fix on top of `08a069a`) — fix two `smartCenter` bugs**
  - The original multi-block centering logic is restored as a fallback: runs when the new dodge decides "no nudge", or when the front two blocks are clear.
  - The nudge is now applied as a vector — `boat.pos + nudge * (-lx, -lz)` — so EAST / WEST facings no longer get the wrong axis. (`calculateNudge` is back.)
  - Sign of the "in-block offset" fallback is fixed: sitting on the left side now maps to `nudge = -0.2`, sitting on the right to `+0.2`.

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
