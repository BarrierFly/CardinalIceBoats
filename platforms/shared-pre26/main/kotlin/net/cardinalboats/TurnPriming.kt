package net.cardinalboats

import net.cardinalboats.config.CIBConfig
import net.cardinalboats.alias.*
import net.cardinalboats.alias.translatable

@Suppress("MagicNumber")
object TurnPriming: TurnPrimingBase {

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
            }
            while (rQueueKey.wasPressed()) {
                clientChatLog(player, translatable("info.cardinalboats.right_turn_queue").string)
                rTurnPrimed = true
                lTurnPrimed = false
            }

            if (CIBConfig.getInstance().alwaysSmartCenter && boat.yaw % 90 == 0f) {
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
        } else {
            // if we aren't on the boat anymore, we don't care
            if (lTurnPrimed || rTurnPrimed) {
                clientChatLog(minecraft.player, translatable("info.cardinalboats.cancel").string)
            }
            lTurnPrimed = false
            rTurnPrimed = false

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

        // Step 1: probe the lane the boat is currently in (front block + front-upper block).
        val frontBlocked = collidesAt(dx, dz, 0) || collidesAt(dx, dz, 1)

        // If a collision is in front, the new single-block lateral dodge runs *only* when
        // exactly one side lane is open — that is the only case the new logic is allowed
        // to nudge the boat from its current position. Any other case (front clear, both
        // sides open, both sides blocked) falls through to the original multi-block
        // centering logic.
        if (frontBlocked) {
            val leftBlocked  = collidesAt(dx + lx, dz + lz, 0) || collidesAt(dx + lx, dz + lz, 1)
            val rightBlocked = collidesAt(dx - lx, dz - lz, 0) || collidesAt(dx - lx, dz - lz, 1)

            // Signed "right step" in {-0.2, 0, +0.2}. Positive means step right, along (-lx, -lz).
            val nudge: Double = when {
                !leftBlocked && rightBlocked  -> -0.2  // only left lane is open -> step left
                !rightBlocked && leftBlocked  ->  0.2  // only right lane is open -> step right
                else                          ->  0.0  // any other case -> defer to original logic
            }

            if (nudge != 0.0) {
                // Apply nudge as a vector along (-lx, -lz). Same formula works for all 4 facings.
                boat.setPosition(boat.x - nudge * lx,
                                 boat.y,
                                 boat.z - nudge * lz)
                return
            }
        }

        // Fall through: original multi-block centering logic. Used when the front is clear,
        // when both side lanes are open, or when both side lanes are blocked.
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

