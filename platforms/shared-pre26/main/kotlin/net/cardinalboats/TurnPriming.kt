package net.cardinalboats

import net.cardinalboats.config.CIBConfig
import net.cardinalboats.alias.*
import net.cardinalboats.alias.translatable
// The real class, not the alias typealias: nested Direction.Axis access
// does not resolve through the alias.
import net.minecraft.util.math.Direction

@Suppress("MagicNumber")
object TurnPriming: TurnPrimingBase {

    // The action bar fades after a couple of seconds, so the pending-turn
    // indicator is re-sent at this interval to stay visible until the turn fires.
    private const val PENDING_TURN_NOTICE_INTERVAL_TICKS = 25

    private class TickCountingTask(private var ticks: Int? = null,
                                   private var times: Int? = null,
                                   val task: () -> Unit) {
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
                task()
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
            task()
        }
    }

    private val tasks = mutableListOf<TickCountingTask>()

    private fun TickCountingTask.addTask(): TickCountingTask {
        synchronized(tasks) {
            tasks.add(this)
        }
        return this;
    }

    override val lQueueKey = KeyBinding(
        "key.cardinalboats.prime_left",
        InputUtilType.KEYSYM,
        GLFW_KEY_LEFT,
        KEY_BINDING_CATEGORY
        //"category.cardinalboats.key_category_title"
    )

    override val rQueueKey = KeyBinding(
        "key.cardinalboats.prime_right",
        InputUtilType.KEYSYM,
        GLFW_KEY_RIGHT,
        KEY_BINDING_CATEGORY
        //"category.cardinalboats.key_category_title"
    )


    override val smartCenterKey = KeyBinding(
        "key.cardinalboats.smartCenter",
        InputUtilType.KEYSYM,
        GLFW_KEY_BACKSLASH,
        KEY_BINDING_CATEGORY
        //"category.cardinalboats.key_category_title"
    )

    private var lTurnPrimed = false
    private var rTurnPrimed = false
    private var pendingTurnNotice = 0

    private val toScanMapLeft = mapOf(
        Direction.SOUTH to arrayOf(intArrayOf(3, 0), intArrayOf(3, -1), intArrayOf(3, 1), intArrayOf(3, -2)),
        Direction.NORTH to arrayOf(intArrayOf(-3, 0), intArrayOf(-3, 1), intArrayOf(-3, -1), intArrayOf(-3, 2)),
        Direction.EAST to arrayOf(intArrayOf(0, -3), intArrayOf(-1, -3), intArrayOf(1, -3), intArrayOf(-2, -3)),
        Direction.WEST to arrayOf(intArrayOf(0, 3), intArrayOf(1, 3), intArrayOf(-1, 3), intArrayOf(2, 3))
    )

    private val toScanMapRight = mapOf(
        Direction.SOUTH to arrayOf(intArrayOf(-3, 0), intArrayOf(-3, -1), intArrayOf(-3, 1), intArrayOf(-3, -2)),
        Direction.NORTH to arrayOf(intArrayOf(3, 0), intArrayOf(3, 1), intArrayOf(3, -1), intArrayOf(3, 2)),
        Direction.EAST to arrayOf(intArrayOf(0, 3), intArrayOf(-1, 3), intArrayOf(1, 3), intArrayOf(-2, 3)),
        Direction.WEST to arrayOf(intArrayOf(0, -3), intArrayOf(1, -3), intArrayOf(-1, -3), intArrayOf(2, -3))
    )

    private val snapBlockMap = mapOf(
        Direction.SOUTH to arrayOf(intArrayOf(0, 0), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(0, -2)),
        Direction.NORTH to arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(0, 2)),
        Direction.EAST to arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(-2, 0)),
        Direction.WEST to arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(2, 0))
    )

    val centerTask = {
        val boat = MinecraftClient.getInstance().player?.vehicle
        if (boat != null && boat is AbstractBoatEntity) {
            smartCenter(boat)
        }
    }

    @Suppress("EmptyWhileBlock", "MagicNumber", "CyclomaticComplexMethod")
    override fun tick(minecraft: MinecraftClient) {
        val player = minecraft.player
        if (player != null && player.vehicle != null && player.vehicle is AbstractBoatEntity) {
            tasks.runAll()
            val boat = player.vehicle as AbstractBoatEntity
            while (lQueueKey.wasPressed()) {
                clientChatLog(player, translatable("info.cardinalboats.left_turn_queue").string)
                lTurnPrimed = true
                rTurnPrimed = false
                // Start the indicator counter at the full interval: the priming
                // tick already shows the "turn primed" message.
                pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
            }
            while (rQueueKey.wasPressed()) {
                clientChatLog(player, translatable("info.cardinalboats.right_turn_queue").string)
                rTurnPrimed = true
                lTurnPrimed = false
                pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
            }

            if (CIBConfig.getInstance().alwaysSmartCenter && isCardinallyAligned(boat.yaw)) {
                TickCountingTask(task = centerTask).addTask().runNow()
            }

            while (smartCenterKey.wasPressed()) {
                TickCountingTask(task = centerTask).addTask().runNow()
            }

            val world = minecraft.world!!

            if (lTurnPrimed && shouldTurn(boat, world, true)) {
                rotateBoat(boat, roundYRot(boat.yaw - 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                lTurnPrimed = false
                clientChatLog(player, translatable("info.cardinalboats.left_turn_complete").string)
                TickCountingTask {
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask()
                }.addTask().runNow()
            } else if (rTurnPrimed && shouldTurn(boat, world, false)) {
                rotateBoat(boat, roundYRot(boat.yaw + 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                rTurnPrimed = false
                clientChatLog(player, translatable("info.cardinalboats.right_turn_complete").string)
                TickCountingTask {
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask()
                }.addTask().runNow()
            }

            // Persistent pending-turn arrow: re-sent periodically while a turn is
            // primed so it stays visible after the action bar fades.
            if (lTurnPrimed || rTurnPrimed) {
                pendingTurnNotice -= 1
                if (pendingTurnNotice <= 0) {
                    pendingTurnNotice = PENDING_TURN_NOTICE_INTERVAL_TICKS
                    clientChatLog(player, translatable(
                        if (lTurnPrimed) "info.cardinalboats.left_turn_pending"
                        else "info.cardinalboats.right_turn_pending").string)
                }
            } else {
                pendingTurnNotice = 0
            }
        } else {
            // if we aren't on the boat anymore, we don't care
            if (lTurnPrimed || rTurnPrimed) {
                clientChatLog(minecraft.player, translatable("info.cardinalboats.cancel").string)
            }
            lTurnPrimed = false
            rTurnPrimed = false
            pendingTurnNotice = 0

            // not in a boat, don't care about any presses these buttons get right now
            while (lQueueKey.wasPressed() || rQueueKey.wasPressed() || smartCenterKey.wasPressed()) {}
        }
    }

    fun shouldTurn(boat: AbstractBoatEntity, level: ClientWorld, left: Boolean): Boolean {
        val rootX = boat.blockX
        val rootY = boat.blockY - 1
        val rootZ = boat.blockZ

        // get the direction the boat is facing
        // north/south/east/west
        val direction = boat.horizontalFacing
        // get the block offsets for left/right
        val map = if (left) {
            toScanMapLeft[direction]!!
        } else {
            toScanMapRight[direction]!!
        }

        for (i in map.indices) {
            val testBlockPos = BlockPos(rootX + map[i][0], rootY, rootZ + map[i][1])
            val testBlockPosAbove = BlockPos(rootX + map[i][0], rootY + 1, rootZ + map[i][1])
            if (isIce(level.getBlockState(testBlockPos)) || isWater(level.getBlockState(testBlockPosAbove))) {
                lieAboutMovingForward = true
                val snapBlock = snapBlockMap[direction]!![i]
                boat.setPosition(rootX + snapBlock[0] + 0.5, boat.y, rootZ + snapBlock[1] + 0.5)
                lieAboutMovingForward = false
                return true
            }
        }

        return false
    }

    fun smartCenter(boat: AbstractBoatEntity) {
        val world = boat.world
        val direction = boat.horizontalFacing
        val rootX = boat.blockX
        val rootY = boat.blockY
        val rootZ = boat.blockZ

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
        fun hasSurfaceAt(x: Int, z: Int): Boolean {
            return isIce(world.getBlockState(BlockPos(rootX + x, rootY - 1, rootZ + z)))
                    || isWater(world.getBlockState(BlockPos(rootX + x, rootY, rootZ + z)))
        }

        // Step 1: probe the lane the boat is currently in (front block + front-upper block).
        val frontBlocked = collidesAt(dx, dz, 0) || collidesAt(dx, dz, 1)

        // If a collision is in front, the single-block lateral dodge runs; see
        // dodgeNudge in BoatMath.kt for the decision table. A side lane only
        // counts as open when it is collision-free AND still has a rideable
        // surface - a missing surface is treated like a wall so the boat never
        // dodges off the lane. Whatever the dodge defers (front clear, or both
        // side lanes blocked) falls through to the multi-block centering logic.
        if (frontBlocked) {
            fun sideLaneOpen(x: Int, z: Int): Boolean {
                return !collidesAt(x, z, 0) && !collidesAt(x, z, 1) && hasSurfaceAt(x, z)
            }

            // True when the collision shape of the side block at (x, z) - offsets
            // from the boat's lane block - reaches the edge it shares with the
            // lane along the axis of the left step (lx, lz), i.e. there is no gap
            // on that side. A lane can also count as blocked for having no
            // ice/water; such a lane has no collision shape here and reads as
            // recessed, which is intended: nudged NUDGE_STEP from the block
            // centre toward a clear but surfaceless side is safe when the other
            // side is obstructed.
            fun extendsToLaneEdge(x: Int, z: Int): Boolean {
                val axis = if (lx != 0) Direction.Axis.X else Direction.Axis.Z
                // Stepping from the side block back to the lane negates its offset;
                // the sign picks which face of the shape is the shared edge.
                val towardLane = -(if (axis == Direction.Axis.X) x else z)
                for (yOff in 0..1) {
                    val pos = BlockPos(rootX + x, rootY + yOff, rootZ + z)
                    val shape = world.getBlockState(pos).getCollisionShape(world, pos)
                    if (shape.isEmpty) continue
                    val reaches = if (towardLane > 0) shape.getMax(axis) >= 1.0 - 0.01
                                  else shape.getMin(axis) <= 0.01
                    if (reaches) return true
                }
                return false
            }

            // sideOffset > 0 means the boat sits on the left half of its lane block.
            val sideOffset = (boat.x - (rootX + 0.5)) * lx + (boat.z - (rootZ + 0.5)) * lz
            // Wrap the bow's yaw offset from the lane facing into (-180, 180].
            var yawDiff = boat.yaw - when (direction) {
                Direction.SOUTH ->  0f
                Direction.WEST  -> 90f
                Direction.NORTH -> 180f
                else            -> 270f // EAST
            }
            yawDiff = ((yawDiff + 180f) % 360f + 360f) % 360f - 180f

            val leftBlocked = !sideLaneOpen(dx + lx, dz + lz)
            val rightBlocked = !sideLaneOpen(dx - lx, dz - lz)
            val nudge = if (leftBlocked && rightBlocked) {
                // Both walls block: dodge toward the wall whose collision does NOT
                // reach the edge it shares with the lane - that side has a gap.
                // The side blocks sit at the left step (lx, lz) from the lane and
                // its negation.
                dodgeNudge(leftBlocked, rightBlocked, sideOffset, yawDiff,
                           extendsToLaneEdge(lx, lz),
                           extendsToLaneEdge(-lx, -lz))
            } else {
                dodgeNudge(leftBlocked, rightBlocked, sideOffset, yawDiff)
            }
            if (nudge != 0.0) {
                // Positive nudge steps right, along (-lx, -lz); same formula for all 4 facings.
                boat.setPosition(boat.x - nudge * lx,
                                 boat.y,
                                 boat.z - nudge * lz)
                return
            }
        }

        // Fall through: original multi-block centering logic. Used when the front is clear,
        // or when the front is blocked but both side lanes are blocked.
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
            boat.setPosition(rootX + 0.5 + nudgeX, boat.y, boat.z)
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
            boat.setPosition(boat.x, boat.y, rootZ + 0.5 + nudgeZ)
        }
    }

    private fun calculateNudge(world: World,
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
        return MathHelper.clamp(nudge.toDouble(), -0.2, 0.2)
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
