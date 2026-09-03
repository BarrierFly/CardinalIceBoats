<!-- latest begin -->
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
