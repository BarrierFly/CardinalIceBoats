package net.cardinalboats

import com.mojang.blaze3d.platform.InputConstants
import net.cardinalboats.alias.KEY_BINDING_CATEGORY
import net.cardinalboats.config.CIBConfig
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.level.Level
@Suppress("MagicNumber")
object TurnPriming: TurnPrimingBase {

    // The action bar fades after a couple of seconds, so the pending-turn
    // indicator is re-sent at this interval to stay visible until the turn fires.
    private const val PENDING_TURN_NOTICE_INTERVAL_TICKS = 25

    private class TickCountingTask(private var ticks: Int? = null,
                                   private var times: Int? = null,
                                   val task: (delayed: Boolean) -> Unit) {
        init {
            if (ticks == null) {
                ticks = CIBConfig.getInstance().smartCenterPrimedTurnDelayTicks
            }
            if (times == null) {
                times = 0
            }
        }

        fun tick(): Boolean {
            ticks = ticks!! - 1
            if (ticks!! <= 0) {
                task(true)
                times = times!! - 1
                if (times!! <= 0) {
                    return true
                } else {
                    ticks = CIBConfig.getInstance().smartCenterPrimedTurnDelayTicks
                }
            }
            return false
        }

        fun runNow() {
            task(false)
        }
    }

    private val tasks = mutableListOf<TickCountingTask>()

    private fun TickCountingTask.addTask(): TickCountingTask {
        synchronized(tasks) {
            tasks.add(this)
        }
        return this;
    }

    // Decision-trace echo for debugSmartCenter. Compact: one line of probe
    // context (trigger, facing, lane/wall probes) plus one result line, flushed
    // together at the end of the smartCenter call.
    class CenterTrace(private val source: String) {
        private var head = ""
        private val probes = mutableListOf<String>()
        private var outcome: String? = null

        fun header(direction: Direction) {
            head = "§8[§fSC§8]§7 $source§8 ·§f ${playerWord(direction)}"
        }

        // Probe results share the header line, separated by a middot.
        fun step(text: String) {
            probes.add(text)
        }

        fun result(text: String) {
            outcome = text
        }

        fun flush(player: Player?) {
            val out = mutableListOf<String>()
            val context = (listOf(head) + probes).filter { it.isNotEmpty() }
            if (context.isNotEmpty()) out.add(context.joinToString("§8 ·§f "))
            outcome?.let { out.add("§8=§a $it") }
            if (out.isEmpty()) return
            clientChatLog(player, out.joinToString("\n"))
        }

        companion object {
            // shift() takes the signed displacement along the player's RIGHT axis:
            // positive is the player's right, negative the player's left. Callers
            // must convert their raw code-side values into that axis first.
            fun playerWord(direction: Direction): String = when (direction) {
                Direction.SOUTH -> "南"
                Direction.WEST  -> "西"
                Direction.NORTH -> "北"
                Direction.EAST  -> "东"
                else            -> direction.name
            }

            fun shift(v: Double): String = when {
                v > 0.0 -> "向右移 ${String.format("%.1f", v)}"
                v < 0.0 -> "向左移 ${String.format("%.1f", -v)}"
                else -> "不动"
            }
        }
    }

    override val lQueueKey = KeyMapping("key.cardinalboats.prime_left",
                                        InputConstants.Type.KEYSYM,
                                        InputConstants.KEY_LEFT,
                                        KEY_BINDING_CATEGORY) //"category.cardinalboats.key_category_title"

    override val rQueueKey = KeyMapping("key.cardinalboats.prime_right",
                                        InputConstants.Type.KEYSYM,
                                        InputConstants.KEY_RIGHT,
                                        KEY_BINDING_CATEGORY
        //"category.cardinalboats.key_category_title"
    )


    override val smartCenterKey = KeyMapping("key.cardinalboats.smartCenter",
                                             InputConstants.Type.KEYSYM,
                                             InputConstants.KEY_BACKSLASH,
                                             KEY_BINDING_CATEGORY
                                            //"category.cardinalboats.key_category_title"
    )

    private var lTurnPrimed = false
    private var rTurnPrimed = false
    private var pendingTurnNotice = 0

    private val toScanMapLeft = mapOf(Direction.SOUTH to arrayOf(intArrayOf(3, 0), intArrayOf(3, -1), intArrayOf(3, 1), intArrayOf(3, -2)),
                                      Direction.NORTH to arrayOf(intArrayOf(-3, 0), intArrayOf(-3, 1), intArrayOf(-3, -1), intArrayOf(-3, 2)),
                                      Direction.EAST to arrayOf(intArrayOf(0, -3), intArrayOf(-1, -3), intArrayOf(1, -3), intArrayOf(-2, -3)),
                                      Direction.WEST to arrayOf(intArrayOf(0, 3), intArrayOf(1, 3), intArrayOf(-1, 3), intArrayOf(2, 3)))

    private val toScanMapRight = mapOf(Direction.SOUTH to arrayOf(intArrayOf(-3, 0), intArrayOf(-3, -1), intArrayOf(-3, 1), intArrayOf(-3, -2)),
                                       Direction.NORTH to arrayOf(intArrayOf(3, 0), intArrayOf(3, 1), intArrayOf(3, -1), intArrayOf(3, 2)),
                                       Direction.EAST to arrayOf(intArrayOf(0, 3), intArrayOf(-1, 3), intArrayOf(1, 3), intArrayOf(-2, 3)),
                                       Direction.WEST to arrayOf(intArrayOf(0, -3), intArrayOf(1, -3), intArrayOf(-1, -3), intArrayOf(2, -3)))

    private val snapBlockMap = mapOf(Direction.SOUTH to arrayOf(intArrayOf(0, 0), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(0, -2)),
                                     Direction.NORTH to arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(0, 2)),
                                     Direction.EAST to arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(-2, 0)),
                                     Direction.WEST to arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(2, 0)))

    // A null source means "do not trace": the alwaysSmartCenter path runs every
// tick, and tracing it would flood the chat. The discrete triggers (key press,
// primed turn, Re-Snap) pass a label and are echoed when debugSmartCenter is on.
    val centerTask = { source: String? ->
        { delayed: Boolean ->
            val boat = Minecraft.getInstance().player?.vehicle
            if (boat != null && boat is AbstractBoat) {
                smartCenter(boat, source, delayed)
            }
        }
    }

    @Suppress("EmptyWhileBlock", "MagicNumber", "CyclomaticComplexMethod")
    override fun tick(minecraft: Minecraft) {
        val player = minecraft.player
        if (player != null && player.vehicle != null && player.vehicle is AbstractBoat) {
            tasks.runAll()
            val boat = player.vehicle as AbstractBoat
            while (lQueueKey.consumeClick()) {
                clientChatLog(player, Component.translatable("info.cardinalboats.left_turn_queue").string)
                lTurnPrimed = true
                rTurnPrimed = false
                // Start the indicator counter at the full interval: the priming
                // tick already shows the "turn primed" message.
                pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
            }
            while (rQueueKey.consumeClick()) {
                clientChatLog(player, Component.translatable("info.cardinalboats.right_turn_queue").string)
                rTurnPrimed = true
                lTurnPrimed = false
                pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
            }

            if (CIBConfig.getInstance().alwaysSmartCenter && isCardinallyAligned(boat.yRot)) {
                TickCountingTask(task = centerTask(null)).addTask().runNow()
            }

            while (smartCenterKey.consumeClick()) {
                TickCountingTask(task = centerTask("key")).addTask().runNow()
            }

            val world = minecraft.level!!

            if (lTurnPrimed && shouldTurn(boat, world, true)) {
                rotateBoat(boat, roundYRot(boat.yRot - 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                lTurnPrimed = false
                clientChatLog(player, Component.translatable("info.cardinalboats.left_turn_complete").string)
                TickCountingTask { delayed ->
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask("primed-turn-left" + if (delayed) "+delayed" else "")(delayed)
                }.addTask().runNow()
            } else if (rTurnPrimed && shouldTurn(boat, world, false)) {
                rotateBoat(boat, roundYRot(boat.yRot + 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                rTurnPrimed = false
                clientChatLog(player, Component.translatable("info.cardinalboats.right_turn_complete").string)
                TickCountingTask { delayed ->
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask("primed-turn-right" + if (delayed) "+delayed" else "")(delayed)
                }.addTask().runNow()
            }

            // Persistent pending-turn arrow: re-sent periodically while a turn is
            // primed so it stays visible after the action bar fades.
            if (lTurnPrimed || rTurnPrimed) {
                pendingTurnNotice -= 1
                if (pendingTurnNotice <= 0) {
                    pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
                    clientChatLog(player, Component.translatable(
                        if (lTurnPrimed) "info.cardinalboats.left_turn_pending"
                        else "info.cardinalboats.right_turn_pending").string)
                }
            } else {
                pendingTurnNotice = 0
            }
        } else {
            // if we aren't on the boat any more, we don't care
            if (lTurnPrimed || rTurnPrimed) {
                clientChatLog(minecraft.player, Component.translatable("info.cardinalboats.cancel").string)
            }
            lTurnPrimed = false
            rTurnPrimed = false
            pendingTurnNotice = 0

            // not in a boat, don't care about any presses these buttons get right now
            while (lQueueKey.consumeClick() || rQueueKey.consumeClick() || smartCenterKey.consumeClick()) {}
        }
    }

    fun shouldTurn(boat: AbstractBoat, level: ClientLevel, left: Boolean): Boolean {
        val rootX = boat.blockX
        val rootY = boat.blockY - 1
        val rootZ = boat.blockZ

        // get the direction the boat is facing
        // north/south/east/west
        val direction = boat.direction
        // get the block offsets for left/right
        val map = if (left) {
            toScanMapLeft[direction]!!
        } else {
            toScanMapRight[direction]!!
        }

        for (i in map.indices) {
            // Downhill lanes count too (2.3): the probe accepts a surface up to
            // surfaceMaxDrop blocks below the boat's nominal level.
            if (surfaceDropAt(level, rootX + map[i][0], rootY + 1, rootZ + map[i][1], CIBConfig.getInstance().surfaceMaxDrop) != null) {
                lieAboutMovingForward = true
                val snapBlock = snapBlockMap[direction]!![i]
                boat.setPos(rootX + snapBlock[0] + 0.5, boat.y, rootZ + snapBlock[1] + 0.5)
                lieAboutMovingForward = false
                return true
            }
        }

        return false
    }

    // source labels the trigger for the debug echo; null means "no trace".
    // delayed is true on the TickCountingTask pass that runs after the primed-turn
    // delay instead of immediately.
    fun smartCenter(boat: AbstractBoat, source: String? = null, delayed: Boolean = false) {
        val world = boat.level()
        val direction = boat.direction
        val rootX = boat.blockX
        val rootY = boat.blockY
        val rootZ = boat.blockZ

        val tracing = source != null && CIBConfig.getInstance().debugSmartCenter
        val trace = if (tracing) CenterTrace(source!!) else null
        trace?.header(direction)

        // Forward step (dx, dz) and perpendicular "left" step (lx, lz) for the boat's facing.
        // The "right" step is the negation of "left".
        val (dx, dz, lx, lz) = when (direction) {
            Direction.SOUTH -> intArrayOf(0,  1, -1,  0) // fwd=+Z, left=-X
            Direction.NORTH -> intArrayOf(0, -1,  1,  0) // fwd=-Z, left=+X
            Direction.EAST  -> intArrayOf(1,  0,  0,  1) // fwd=+X, left=+Z
            Direction.WEST  -> intArrayOf(-1, 0,  0, -1) // fwd=-X, left=-Z
            else -> return
        }
        fun collidesAt(x: Int, z: Int, yOff: Int = 0): Boolean {
            val pos = BlockPos(rootX + x, rootY + yOff, rootZ + z)
            return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty
        }
        // Ice sits one block below the boat; water is level with the boat.
        // Since 2.3 the probe also accepts a surface up to surfaceMaxDrop
        // blocks below the nominal level (downhill lanes). Returns the drop
        // depth, or null when the column has no reachable surface.
        fun surfaceDrop(x: Int, z: Int): Int? {
            return surfaceDropAt(world, rootX + x, rootY, rootZ + z, CIBConfig.getInstance().surfaceMaxDrop)
        }

        // Step 1: probe the lane the boat is currently in (front block + front-upper block).
        val frontBlocked = collidesAt(dx, dz, 0) || collidesAt(dx, dz, 1)
        trace?.step(if (frontBlocked) "前堵" else "前通")

        // A side lane only counts as open when it is collision-free AND still has
        // a rideable surface - a missing surface is treated like a wall so the
        // boat never dodges off the lane. Since 2.3 a surface up to surfaceMaxDrop
        // blocks below the boat counts, so downhill side lanes are usable; a fully
        // surfaceless lane still reads as a wall. See dodgeNudge in BoatMath.kt
        // for the decision table.
        fun sideLaneDrop(x: Int, z: Int): Int? {
            return if (!collidesAt(x, z, 0) && !collidesAt(x, z, 1)) surfaceDrop(x, z) else null
        }

        // True when the collision shape of the ahead-side block at (x, z) - offsets
        // from the boat's lane block - reaches the passage edge on its lane-facing
        // side along the axis of the left step (lx, lz), i.e. there is no gap on
        // that side. The forward offset vanishes along the left-step axis, so the
        // derived sign is correct for the diagonals as well. A lane can also count
        // as blocked for having no ice/water; such a lane has no collision shape
        // here and reads as recessed, which is intended: snapped NUDGE_STEP from
        // the block centre toward a clear but surfaceless side is safe when the
        // other side is obstructed.
        fun extendsToLaneEdge(x: Int, z: Int): Boolean {
            val axis = if (lx != 0) Direction.Axis.X else Direction.Axis.Z
            // Stepping from the side block back to the lane negates its offset;
            // the sign picks which face of the shape is the shared edge.
            val towardLane = -(if (axis == Direction.Axis.X) x else z)
            for (yOff in 0..1) {
                val pos = BlockPos(rootX + x, rootY + yOff, rootZ + z)
                val shape = world.getBlockState(pos).getCollisionShape(world, pos)
                if (shape.isEmpty) continue
                val reaches = if (towardLane > 0) shape.max(axis) >= 1.0 - 0.01
                              else shape.min(axis) <= 0.01
                if (reaches) return true
            }
            return false
        }

        // sideOffset > 0 means the boat sits on the left half of its lane block.
        val sideOffset = (boat.x - (rootX + 0.5)) * lx + (boat.z - (rootZ + 0.5)) * lz
        // Wrap the bow's yaw offset from the lane facing into (-180, 180].
        var yawDiff = boat.yRot - when (direction) {
            Direction.SOUTH ->  0f
            Direction.WEST  -> 90f
            Direction.NORTH -> 180f
            else            -> 270f // EAST
        }
        yawDiff = ((yawDiff + 180f) % 360f + 360f) % 360f - 180f

        val leftLaneDrop = sideLaneDrop(dx + lx, dz + lz)
        val rightLaneDrop = sideLaneDrop(dx - lx, dz - lz)
        val leftBlocked = leftLaneDrop == null
        val rightBlocked = rightLaneDrop == null
        // leftBlocked probes (dx+lx, dz+lz); (lx, lz) is the code "left" step,
        // which is mirrored and points to the player's RIGHT. Report the player's
        // view, so the code-left lane is the player's right lane. An open lane
        // sitting lower than the boat reports its drop: 左通(-1) means the
        // player's left lane surface is one block below the boat.
        fun laneWord(drop: Int?): String = when {
            drop == null -> "堵"
            drop == 0    -> "通"
            else         -> "通($drop)"
        }
        trace?.step("左${laneWord(rightLaneDrop)} 右${laneWord(leftLaneDrop)}")

        if (frontBlocked) {
            // Trapped ahead: the relative lateral dodge steps toward the open side
            // lane; both side lanes blocked defers to the multi-block centering.
            val nudge = dodgeNudge(leftBlocked, rightBlocked, sideOffset, yawDiff)
            if (nudge != 0.0) {
                // A positive nudge steps along (-lx, -lz), the player's LEFT; same
                // formula for all 4 facings.
                val nx = boat.x - nudge * lx
                val nz = boat.z - nudge * lz
                boat.setPos(nx,
                            boat.y,
                            nz)
                trace?.result("相对偏移 ${CenterTrace.shift(-nudge)}")
                trace?.flush(Minecraft.getInstance().player)
                return
            }
        } else if (leftBlocked && rightBlocked) {
            // Corridor: the front is clear but both ahead-side lanes are walled.
            // Lean toward the wall whose collision does NOT reach the passage edge
            // - that side has a gap - via the absolute cross-axis snap, which
            // holds the boat 0.2 off the block centre toward the gap without ever
            // leaving its lane. Exactly one flush wall names the other side as the
            // gap side; both flush or both recessed defers to the multi-block
            // centering. The probed blocks are the same diagonals that decided
            // leftBlocked / rightBlocked.
            val leftEdge = extendsToLaneEdge(dx + lx, dz + lz)
            val rightEdge = extendsToLaneEdge(dx - lx, dz - lz)
            // Exactly one wall flush means the gap is on the other side.
            // Mirror the code sides for the player's view: leftBlocked means the
            // lane on the player's RIGHT is unusable.
            trace?.step("右墙${if (leftEdge) "无缝" else "有缝"} 左墙${if (rightEdge) "无缝" else "有缝"}")
            val nudge = dodgeNudge(leftBlocked, rightBlocked, sideOffset, yawDiff,
                                   leftEdge,
                                   rightEdge)
            if (nudge != 0.0) {
                // Absolute snap of the cross-axis coordinate to the block centre
                // offset by the nudge along (-lx, -lz); the along-axis coordinate
                // is kept, matching the multi-block centering below.
                val nx: Double
                val nz: Double
                if (lx != 0) {
                    nx = rootX + 0.5 - nudge * lx
                    nz = boat.z
                } else {
                    nx = boat.x
                    nz = rootZ + 0.5 - nudge * lz
                }
                boat.setPos(nx, boat.y, nz)
                trace?.result("绝对偏移 ${CenterTrace.shift(-nudge)}")
                trace?.flush(Minecraft.getInstance().player)
                return
            }
        }

        // Fall through: original multi-block centering logic. Used when the front is
        // blocked but neither side lane is open, or when the front is clear and the
        // corridor gap snap does not apply (a side lane is open, or both walls are
        // flush / both recessed).
        val scanAhead = CIBConfig.getInstance().smartCenterLookAhead
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            val startZ = if (direction == Direction.NORTH) -scanAhead else -1
            val endZ = if (direction == Direction.NORTH) 1 else scanAhead
            val nudgeX = calculateNudge(world,
                                        startZ,
                                        endZ,
                                        { z ->
                                            BlockPos(rootX - 1, rootY, rootZ + z)
                                        },
                                        { z ->
                                            BlockPos(rootX + 1, rootY, rootZ + z)
                                        }
            )
            //logger.info("NS setting boat pos to x: ${rootX + 0.5 + nudgeX}, y: ${boat.y}, z: ${boat.z}")
            val nx = rootX + 0.5 + nudgeX
            boat.setPos(nx, boat.y, boat.z)
            trace?.result("多块扫描 ${scanAhead} 格 ${CenterTrace.shift(nudgeX * lx)}")
        } else {
            val startX = if (direction == Direction.WEST) -scanAhead else -1
            val endX = if (direction == Direction.WEST) 1 else scanAhead
            val nudgeZ = calculateNudge(world,
                                        startX,
                                        endX,
                                        { x ->
                                            BlockPos(rootX + x, rootY, rootZ - 1)
                                        },
                                        { x ->
                                            BlockPos(rootX + x, rootY, rootZ + 1)
                                        }
            )
            //logger.info("setting boat pos to x: ${boat.x}, y: ${boat.x}, z: ${rootZ + 0.5 + nudgeZ}")
            val nz = rootZ + 0.5 + nudgeZ
            boat.setPos(boat.x, boat.y, nz)
            trace?.result("多块扫描 ${scanAhead} 格 ${CenterTrace.shift(nudgeZ * lz)}")
        }
        trace?.flush(Minecraft.getInstance().player)
    }

    private fun calculateNudge(world: Level,
                               start: Int,
                               end: Int,
                               leftBlockPosFunc: (Int) -> BlockPos,
                               rightBlockPosFunc: (Int) -> BlockPos): Double {
        var nudge = 0
        for (i in start..end) {
            val leftBlockPos = leftBlockPosFunc(i)
            val rightBlockPos = rightBlockPosFunc(i)
            val leftState = world.getBlockState(leftBlockPos)
            val rightState = world.getBlockState(rightBlockPos)
            if (!leftState.getCollisionShape(world, leftBlockPos).isEmpty)
                nudge += 1
            if (!rightState.getCollisionShape(world, rightBlockPos).isEmpty)
                nudge -= 1
        }
        return Mth.clamp(nudge.toDouble(), -0.2, 0.2)
    }

    private fun MutableList<TickCountingTask>.runAll() {
        synchronized(this) {
            val toRemove = this.filter {
                it.tick()
            }
            this.removeAll(toRemove)
        }
    }

}
