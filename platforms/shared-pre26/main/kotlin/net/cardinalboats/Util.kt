package net.cardinalboats

import net.cardinalboats.alias.*
import net.cardinalboats.config.CIBConfig


@JvmField
var lieAboutMovingForward = false;

fun rotateBoat(boat: AbstractBoatEntity, rotation: Float, maintainVelocity: Boolean, postAction: () -> Unit = {}) {

    if (maintainVelocity) {
        // get current velocity vector length
        val currentVelocity = boat.velocity.length()
        // create new vector normalized to rotation
        val newVelocity = Vec3d(0.0, 0.0, currentVelocity).rotateY(-rotation * RADIANS_PER_DEGREE) // Trig magic
        // give boat new thing
        boat.velocity = newVelocity
    } else {
        boat.velocity = Vec3d.ZERO
    }
    boat.yaw = rotation
    boat.yawVelocity = 0f
    boat.controllingPassenger?.yaw = boat.yaw

    postAction()
}

fun isIce(blockState: BlockState) = blockState.isInIceTag()

// Waterlogged blocks carry a water fluid state, so they count as water too.
fun isWater(blockState: BlockState) = blockState.hasWaterFluid()

// Downhill lane probe (2.3): walk the column at (x, z) downward from the boat's
// nominal surface level - ice at y-1, water at y - until ice or water is found
// within maxDrop steps, and return the drop depth (0 = at the nominal level,
// null = no reachable surface). maxDrop 0 probes exactly like the pre-2.3 flat
// check. A hit only counts when the cells above it up to the boat's own layer
// are collision-free, so a surface under a block reads as a step, not a slope.
// See surfaceDropDepth in BoatMath.kt for the search rules.
fun surfaceDropAt(level: World, x: Int, y: Int, z: Int, maxDrop: Int): Int? {
    return surfaceDropDepth(maxDrop,
        { yOff -> isIce(level.getBlockState(BlockPos(x, y + yOff, z))) },
        { yOff -> isWater(level.getBlockState(BlockPos(x, y + yOff, z))) },
        { yOff -> !level.getBlockState(BlockPos(x, y + yOff, z)).getCollisionShape(level, BlockPos(x, y + yOff, z)).isEmpty })
}

fun clientChatLog(player: ClientPlayerEntity?, message: String) {
    if (player == null) return

    if (CIBConfig.getInstance().showStatusMessages) {
        // overlay = true puts the text on the action bar above the hotbar, like
        // the shared-26 copy. Chat (overlay = false) would flood the history
        // with the periodic pending-turn indicator.
        player.sendMessage(makeText("[cardinalboats] $message"), true)
    }
}

@Suppress("MagicNumber")
fun shouldSnap(level: World, player: PlayerEntity): Boolean {
    // If we are putting a boat on a block
    val lookingAt = player.raycast(20.0, 0.0f, false)
    if (lookingAt != null && lookingAt.type == HitResultType.BLOCK) {
        // If that block is ice or water, return true
        return isIce(level.getBlockState((lookingAt as BlockHitResult).blockPos))
                || isWater(level.getBlockState((lookingAt as BlockHitResult).blockPos))
    }
    return false
}
