![# Cardinal Ice Boats](https://github.com/CodeF53/CardinalIceBoats/raw/main/Banner.png)

## 近期改动 (commit `7766b45` 起，至本仓库当前 HEAD)

中 / EN — 涵盖 `7766b45 fix: use collision shape instead of air check in calculateNudge, restore multi-version build` 到本仓库当前 HEAD（含）之间的所有提交。

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

- **本仓库当前 HEAD（fix on top of `08a069a`）— 修复 `smartCenter` 两个 bug**
  - 原算法作为兜底：当新算法决定无横移，或前方两格均无碰撞时，回落到原有多格对中逻辑（`calculateNudge` 已被恢复）。
  - 横移应用改为向量形式 `boat.pos + nudge * (-lx, -lz)`，保证 EAST / WEST 朝向时方向也正确（原实现里 EAST/WEST 写成了沿 z 加减，方向反了）。
  - 兜底分支中"船在格内偏左/偏右"的符号也修正：左侧 → `nudge = -0.2`，右侧 → `+0.2`。

## Recent changes (commits from `7766b45` up to current HEAD, inclusive)

ZH / EN — covers everything from `7766b45 fix: use collision shape instead of air check in calculateNudge, restore multi-version build` through this repository's current HEAD.

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

- **Current HEAD (fix on top of `08a069a`) — fix two `smartCenter` bugs**
  - The original multi-block centering logic is restored as a fallback: runs when the new dodge decides "no nudge", or when the front two blocks are clear.
  - The nudge is now applied as a vector — `boat.pos + nudge * (-lx, -lz)` — so EAST / WEST facings no longer get the wrong axis. (`calculateNudge` is back.)
  - Sign of the "in-block offset" fallback is fixed: sitting on the left side now maps to `nudge = -0.2`, sitting on the right to `+0.2`.

## About
Provides Several QOL Utilities for ice boating:

## Features:

### Universal Features:
Work when installed on the server or the client!
#### Placement Snapping:
Boats will automatically snap to face North/South/East/West (and 45-degree angles) when placed on ice.

### Client Exclusive Features:
#### Turn Priming:
Pressing the <kbd>⬅</kbd> or <kbd>➞</kbd><sup>[*](#configurable-controls)</sup> keys will prime a turn. When your turn comes up, your boat will stop and automatically rotate 90 degrees!

<iframe width="560" height="315" src="https://www.youtube.com/embed/pn4UsN_QQ1w" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

#### Manual Snapping:
If your boat gets out of its cardinal orientation, pressing <kbd>⬆</kbd><sup>[*](#configurable-controls)</sup> will snap it back to the nearest cardinal (or 45-degree) direction.

#### Instant Reversing:
Need to turn around? Pressing <kbd>⬇</kbd><sup>[*](#configurable-controls)</sup> will instantly rotate your boat 180 degrees!

#### Smart Centering:
Pressing <kbd>&#92;</kbd><sup>[*](#configurable-controls)</sup> will instantly center your boat on the ice. If you're near a wall, you will instead be nudged _off-center_ just enough to avoid crashing into it. This also automatically happens after every turn by default.

#### Configurable Controls:
All keybinds can be configured in the vanilla controls menu!
