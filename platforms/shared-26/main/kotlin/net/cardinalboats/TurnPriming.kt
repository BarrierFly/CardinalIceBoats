package net.cardinalboats

import com.mojang.blaze3d.platform.InputConstants
import net.cardinalboats.alias.KEY_BINDING_CATEGORY
import net.cardinalboats.config.CIBConfig
import net.minecraft.client.KeyMapping
import net.minecraft.client.KeyMapping.Category
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

            if (CIBConfig.getInstance().alwaysSmartCenter && boat.yRot % 90 == 0f) {
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
        } else {
            // if we aren't on the boat any more, we don't care
            if (lTurnPrimed || rTurnPrimed) {
                clientChatLog(minecraft.player, Component.translatable("info.cardinalboats.cancel").string)
            }
            lTurnPrimed = false
            rTurnPrimed = false

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

        // Step 1: probe the lane the boat is currently in (front block + front-upper block).
        // If both are clear, there is nothing to dodge.
        val frontBlocked = collidesAt(dx, dz, 0) || collidesAt(dx, dz, 1)
        if (!frontBlocked) return

        // Step 2: probe the lane one block to the left and one block to the right.
        val leftBlocked  = collidesAt(dx + lx, dz + lz, 0) || collidesAt(dx + lx, dz + lz, 1)
        val rightBlocked = collidesAt(dx - lx, dz - lz, 0) || collidesAt(dx - lx, dz - lz, 1)

        // Step 3: decide lateral offset.
        val nudge: Double = when {
            !leftBlocked && rightBlocked  -> -0.2  // only left lane is open -> step left
            !rightBlocked && leftBlocked  ->  0.2  // only right lane is open -> step right
            else -> {
                // Both sides open, or both sides blocked: pick from in-block position + bow drift.
                val centerX = rootX + 0.5
                val centerZ = rootZ + 0.5
                val sideOffset = (boat.x - centerX) * lx + (boat.z - centerZ) * lz
                val centered = kotlin.math.abs(sideOffset) < 0.02

                if (!centered) {
                    // Boat already off-center in this lane: lean toward the side it sits on.
                    if (sideOffset < 0) -0.2 else 0.2
                } else {
                    // Boat is centered in the lane: pick by bow drift.
                    val facingYaw = when (direction) {
                        Direction.SOUTH ->   0f
                        Direction.WEST  ->  90f
                        Direction.NORTH -> 180f
                        Direction.EAST  -> 270f
                        else -> 0f
                    }
                    // Wrap yaw diff into (-180, 180].
                    var diff = boat.yRot - facingYaw
                    diff = ((diff + 180f) % 360f + 360f) % 360f - 180f
                    when {
                        diff < -0.5f  -> -0.2  // bow points to the left of facing
                        diff >  0.5f  ->  0.2  // bow points to the right of facing
                        else          ->  0.2  // bow straight ahead: default to right
                    }
                }
            }
        }

        if (nudge == 0.0) return  // nothing decided (defensive)

        // Step 4: move the boat from its *current* position by the decided nudge.
        // Skip the lane-center snap; only adjust laterally.
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            boat.setPos(boat.x + nudge, boat.y, boat.z)
        } else {
            boat.setPos(boat.x, boat.y, boat.z + nudge)
        }
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

