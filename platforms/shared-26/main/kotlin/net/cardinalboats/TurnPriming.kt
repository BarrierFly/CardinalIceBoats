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
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.level.Level
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

    val centerTask = {
        val boat = Minecraft.getInstance().player?.vehicle
        if (boat != null && boat is AbstractBoat) {
            smartCenter(boat)
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
            }
            while (rQueueKey.consumeClick()) {
                clientChatLog(player, Component.translatable("info.cardinalboats.right_turn_queue").string)
                rTurnPrimed = true
                lTurnPrimed = false
            }

            if (CIBConfig.getInstance().alwaysSmartCenter && isCardinallyAligned(boat.yRot)) {
                TickCountingTask(task = centerTask).addTask().runNow()
            }

            while (smartCenterKey.consumeClick()) {
                TickCountingTask(task = centerTask).addTask().runNow()
            }

            val world = minecraft.level!!

            if (lTurnPrimed && shouldTurn(boat, world, true)) {
                rotateBoat(boat, roundYRot(boat.yRot - 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                lTurnPrimed = false
                clientChatLog(player, Component.translatable("info.cardinalboats.left_turn_complete").string)
                TickCountingTask {
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask()
                }.addTask().runNow()
            } else if (rTurnPrimed && shouldTurn(boat, world, false)) {
                rotateBoat(boat, roundYRot(boat.yRot + 90, 90), CIBConfig.getInstance().maintainVelocityOnTurns)
                rTurnPrimed = false
                clientChatLog(player, Component.translatable("info.cardinalboats.right_turn_complete").string)
                TickCountingTask {
                    if (CIBConfig.getInstance().smartCenterPrimedTurn) centerTask()
                }.addTask().runNow()
            }

            // Persistent pending-turn arrow: keeps showing which way the boat
            // will turn while a turn is primed.
            if (lTurnPrimed || rTurnPrimed) {
                if (pendingTurnNotice-- <= 0) {
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
            val testBlockPos = BlockPos(rootX + map[i][0], rootY, rootZ + map[i][1])
            val testBlockPosAbove = BlockPos(rootX + map[i][0], rootY + 1, rootZ + map[i][1])
            if (isIce(level.getBlockState(testBlockPos)) || isWater(level.getBlockState(testBlockPosAbove))) {
                lieAboutMovingForward = true
                val snapBlock = snapBlockMap[direction]!![i]
                boat.setPos(rootX + snapBlock[0] + 0.5, boat.y, rootZ + snapBlock[1] + 0.5)
                lieAboutMovingForward = false
                return true
            }
        }

        return false
    }

    fun smartCenter(boat: AbstractBoat) {
        val world = boat.level()
        val direction = boat.direction
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

            val nudge = dodgeNudge(!sideLaneOpen(dx + lx, dz + lz), !sideLaneOpen(dx - lx, dz - lz), sideOffset, yawDiff)
            if (nudge != 0.0) {
                // Positive nudge steps right, along (-lx, -lz); same formula for all 4 facings.
                boat.setPos(boat.x - nudge * lx,
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
            boat.setPos(rootX + 0.5 + nudgeX, boat.y, boat.z)
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
            boat.setPos(boat.x, boat.y, rootZ + 0.5 + nudgeZ)
        }
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
