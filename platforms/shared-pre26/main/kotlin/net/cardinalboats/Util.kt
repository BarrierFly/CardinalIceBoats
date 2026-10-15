package net.cardinalboats

import net.cardinalboats.alias.*
import net.cardinalboats.config.CIBConfig
// The real class, not the alias typealias: nested Direction.Axis access
// does not resolve through the alias.
import net.minecraft.util.math.Direction


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
//
// pseudoIceSideAxis (2.3, Smart Center only): when set, a non-ice block whose
// collision does not reach the block top - collision-free blocks included - is
// accepted as ice when the block across the lane to its left or right is ice
// and the cells above both blocks up to the boat's own layer are collision-free.
// Callers that pass no axis keep the strict ice/water rule.
fun surfaceDropAt(level: World, x: Int, y: Int, z: Int, maxDrop: Int,
                  pseudoIceSideAxis: Direction.Axis? = null): Int? {
    fun collides(atX: Int, atZ: Int, yOff: Int): Boolean {
        val pos = BlockPos(atX, y + yOff, atZ)
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty
    }

    // Every cell from just above yOff up to the boat's own layer is collision-free.
    fun columnClearAbove(atX: Int, atZ: Int, yOff: Int): Boolean {
        for (clear in yOff + 1..0) {
            if (collides(atX, atZ, clear)) return false
        }
        return true
    }

    fun pseudoIce(yOff: Int, state: BlockState): Boolean {
        // "Collision that does not reach the block top": no collision at all
        // counts; a present collision must top out below the block top, with
        // the same 0.01 tolerance the flush-edge check uses.
        val shape = state.getCollisionShape(level, BlockPos(x, y + yOff, z))
        if (!shape.isEmpty && shape.getMax(Direction.Axis.Y) >= 1.0 - 0.01) return false
        // The pseudo block itself must be reachable: clear above up to the boat.
        if (!columnClearAbove(x, z, yOff)) return false
        // Across-lane neighbour must be ice, with the same clearance above.
        val stepX = if (pseudoIceSideAxis == Direction.Axis.X) 1 else 0
        val stepZ = 1 - stepX
        for (sign in intArrayOf(1, -1)) {
            val nx = x + sign * stepX
            val nz = z + sign * stepZ
            if (isIce(level.getBlockState(BlockPos(nx, y + yOff, nz))) && columnClearAbove(nx, nz, yOff)) return true
        }
        return false
    }

    return surfaceDropDepth(maxDrop,
        { yOff ->
            val state = level.getBlockState(BlockPos(x, y + yOff, z))
            isIce(state) || (pseudoIceSideAxis != null && pseudoIce(yOff, state))
        },
        { yOff -> isWater(level.getBlockState(BlockPos(x, y + yOff, z))) },
        { yOff -> collides(x, z, yOff) })
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
