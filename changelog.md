<!-- latest begin -->
### 2.3.0

- Downhill ice/water lanes are now recognised. All lane probes previously assumed a constant
  surface height (ice one block below the boat, water level with it), so stepped downhill lanes
  were invisible: a primed turn would not fire on a slope and side lanes a block or two lower
  were treated as walls. The new column probe walks down from the nominal surface level and
  accepts a hit up to "Surface Drop Tolerance" (`surfaceMaxDrop`, default 2) blocks below the
  boat, provided the path down to the boat's own layer is collision-free - a surface under a
  block reads as a step, not a slope. Primed turns fire on downhill lanes, Smart Center dodges
  into downhill side lanes, and the debug trace reports a lowered lane as 通(-1). Setting
  `surfaceMaxDrop` to 0 restores the 2.2.x behaviour. Uphill (raised) surfaces are not
  recognised; dodging into a lower lane drops the boat that distance in place.

### 2.2.5

- Added a "Debug Smart Center" option that echoes Smart Center's decision to the chat as a few
  short lines: whether the front is blocked ("前堵" / "前通"), whether the left and right lanes are
  open, which branch was taken (relative dodge, corridor gap snap, or the multi-block sweep), and
  the resulting offset and position. Left/right wording follows the player's perspective. The
  whole trace is emitted as one multi-line message per call
  so it stays readable at once. It fires for the Smart Center key, primed turns and Re-Snap; the
  per-tick "Always Smart Center" path is not traced, since it would flood the chat.

### 2.2.4

- Moved the Smart Center "squeeze toward a recessed wall" dodge to the condition it was meant
  for: the front is clear but both ahead-side lanes are walled, in which case the boat is held
  0.2 toward the side whose wall does not reach the passage edge (absolute cross-axis snap — it
  leans into the gap without ever leaving the lane). The flush-edge check now probes the same
  front-side diagonal blocks that decide whether a side lane is blocked, instead of the blocks
  directly beside the boat. A collision dead ahead no longer consults the flush-edge check: the
  boat uses the plain relative dodge toward the open side lane, and a trapped boat with both
  side lanes walled falls through to the multi-block centering as before.

### 2.2.3

- Ice/water detection now uses vanilla tags (`#minecraft:ice` / `#minecraft:water` fluid state) instead of block-name regex.
- Smart Center's lateral dodge treats a side lane without ice (one block below) or water (level with the boat) as blocked, so it never nudges the boat off the lane.
- When both side lanes are blocked and exactly one wall's collision reaches the edge it shares with the boat's lane, Smart Center now steps 0.2 toward the other side, whose wall sits recessed and leaves a gap to squeeze past.
- Always Smart Center now uses a 0.5° yaw tolerance instead of an exact float comparison.
- Primed turns show a persistent ◀◀ / ▶▶ pending indicator above the hotbar; the indicator no longer overwrites the "turn primed" confirmation on the tick the turn is queued.
- Status messages are printed to the action bar on every supported version (pre-1.21.11 ports previously wrote to chat, which the periodic indicator would flood).
- Fixed a 26.1.2 regression where the keep-moving-after-chat grace period only worked the first time per session.
- Player null guards in the chat-move mixins; Re-Snap velocity handling now follows "Maintain Velocity on Turns".
- Mapping-independent logic (`BoatMath`, its tests) is single-sourced in a `shared-pure` source dir compiled by every platform; shared code is single-sourced into `shared-26` / `shared-pre26` (per-platform `src/shared` copies removed).
- Added JUnit unit tests and a `zh_cn.json` language file.
- Renamed the `doChatShit` option to `showStatusMessages`; an existing `doChatShit = false` opt-out is carried over automatically.

### 2.2.2

- Rewrote `smartCenter` to a single-block lateral probe: instead of sweeping
  multiple blocks ahead and snapping the boat back to the lane center, the new
  algorithm inspects only the front block and the front-upper block of the
  current lane, then probes the lanes one block to either side. If only one
  side is open, the boat is nudged that way by `±0.2`. If both sides are open
  or both blocked, the decision falls back to the boat's in-block lateral
  position, and (when centered) to the bow's yaw drift. When a nudge is
  decided, the boat is moved from its current position, skipping the
  lane-center snap.

### 2.1.0

- Added support for:
  * Fabic 26.1.x
  * NeoForge 26.1.x

NOTE: Future versions will drop Cloth Config and switch to libIPN for the configuration. This is due to Cloth Config dropping support for Forge.

### 2.0.5

- Added support for:
  * Fabic 1.21.11
  * NeoForge 1.21.11

<!-- latest end -->
<!-- rest begin -->


### 2.0.3

- Added support for:
  * Fabic 1.21.10
  * NeoForge 1.21.10


### 2.0.2
- Added support for:
    * Fabic 1.18.2, 1.19.2, 1.20.1, 1.21.1
    * NeoForge 1.20.1, 1.21.1
    * Forge 1.18.2, 1.19.2, 1.20.1, 1.21.1
* Added a new option that controls when the mod will attempt to auto centre the boat after a primed turn.
* Turning at high speed is now more reliable.


### 2.0.1

- support for NoeForge. Requires [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge)
- added option that will keep the boat moving for some time after the chat is closed. \
  To allow the player to reengage the forward key.

### 2.0.0

- support for 1.21.6
- now implemented in Kotlin
- new requirement [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)



<!-- rest end -->
