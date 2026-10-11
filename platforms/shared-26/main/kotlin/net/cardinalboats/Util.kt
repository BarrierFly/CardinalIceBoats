package net.cardinalboats

import net.cardinalboats.alias.RADIANS_PER_DEGREE
import net.cardinalboats.config.CIBConfig
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3


@JvmField
var lieAboutMovingForward = false;

var Entity.velocity: Vec3
    get() {
        return this.getDeltaMovement()
    }
    set(value) {
        this.setDeltaMovement(value)
    }

fun rotateBoat(boat: AbstractBoat, rotation: Float, maintainVelocity: Boolean, postAction: () -> Unit = {}) {

    if (maintainVelocity) {
        // get current velocity vector length
        val currentVelocity = boat.velocity.length()
        // create new vector normalized to rotation
        val newVelocity = Vec3(0.0, 0.0, currentVelocity).yRot(-rotation * RADIANS_PER_DEGREE) // Trig magic
        // give boat new thing
        boat.velocity = newVelocity
    } else {
        boat.velocity = Vec3.ZERO
    }
    boat.yRot = rotation
    boat.deltaRotation = 0f
    boat.controllingPassenger?.yRot = boat.yRot

    postAction()
}

fun isIce(blockState: BlockState) = blockState.`is`(BlockTags.ICE)

// Waterlogged blocks carry a water fluid state, so they count as water too.
fun isWater(blockState: BlockState) = blockState.fluidState.`is`(FluidTags.WATER)

// Downhill lane probe (2.3): walk the column at (x, z) downward from the boat's
// nominal surface level - ice at y-1, water at y - until ice or water is found
// within maxDrop steps, and return the drop depth (0 = at the nominal level,
// null = no reachable surface). maxDrop 0 probes exactly like the pre-2.3 flat
// check. A hit only counts when the cells above it up to the boat's own layer
// are collision-free, so a surface under a block reads as a step, not a slope.
// See surfaceDropDepth in BoatMath.kt for the search rules.
fun surfaceDropAt(level: Level, x: Int, y: Int, z: Int, maxDrop: Int): Int? {
    return surfaceDropDepth(maxDrop,
        { yOff -> isIce(level.getBlockState(BlockPos(x, y + yOff, z))) },
        { yOff -> isWater(level.getBlockState(BlockPos(x, y + yOff, z))) },
        { yOff -> !level.getBlockState(BlockPos(x, y + yOff, z)).getCollisionShape(level, BlockPos(x, y + yOff, z)).isEmpty })
}

fun clientChatLog(player: Player?, message: String) {
    if (player == null) return

    if (CIBConfig.getInstance().showStatusMessages) {
        player.sendOverlayMessage(Component.literal("[cardinalboats] $message"))
    }
}

@Suppress("MagicNumber")
fun shouldSnap(level: Level, player: Player): Boolean {
    // If we are putting a boat on a block
    val lookingAt: HitResult? = player.pick(20.0, 0.0f, false)
    if (lookingAt != null && lookingAt.type == HitResult.Type.BLOCK) {
        // If that block is ice or water, return true
        return isIce(level.getBlockState((lookingAt as BlockHitResult).blockPos))
                || isWater(level.getBlockState((lookingAt as BlockHitResult).blockPos))
    }
    return false
}
