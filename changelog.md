<!-- latest begin -->
### 2.2.2

- Second polish pass (same 2.2.2 snapshot line):
  * Ice/water detection now uses vanilla tags (`#minecraft:ice` / `#minecraft:water` fluid state) instead of block-name regex.
  * Smart Center's lateral dodge treats a side lane without ice (one block below) or water (level with the boat) as blocked, so it never nudges the boat off the lane.
  * Always Smart Center now uses a 0.5° yaw tolerance instead of an exact float comparison.
  * Primed turns show a persistent ◀◀ / ▶▶ pending indicator above the hotbar.
  * Fixed a 26.1.2 regression where the keep-moving-after-chat grace period only worked the first time per session.
  * Player null guards in the chat-move mixins; Re-Snap velocity handling now follows "Maintain Velocity on Turns".
  * Shared code is single-sourced into `shared-26` / `shared-pre26` (per-platform `src/shared` copies removed).
  * Added JUnit unit tests and a `zh_cn.json` language file.
  * Renamed the `doChatShit` option to `showStatusMessages` (old config files fall back to the default).

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
