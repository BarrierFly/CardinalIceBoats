package net.cardinalboats

import net.cardinalboats.alias.*
import net.cardinalboats.config.CIBConfig
import net.minecraft.state.property.Properties

import java.util.regex.Pattern
import kotlin.math.roundToInt

private val icePattern = Pattern.compile("(\\b|_)ice\\b", Pattern.CASE_INSENSITIVE)
private val waterPattern = Pattern.compile("(\\b|_)water\\b", Pattern.CASE_INSENSITIVE)


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

fun isIce(blockState: BlockState): Boolean {
    return icePattern.matcher(blockState.block.toString()).find()
}

fun isWater(blockState: BlockState): Boolean {
    if (waterPattern.matcher(blockState.block.toString()).find()) {
        return true
    }
    // Also treat waterlogged blocks as water
    if (blockState.contains(Properties.WATERLOGGED) && blockState.get(Properties.WATERLOGGED)) {
        return true
    }
    return false
}

fun clientChatLog(player: ClientPlayerEntity?, message: String) {
    if (player == null) return

    if (CIBConfig.getInstance().doChatShit) {
        player.sendMessage(makeText("[cardinalboats] $message"), false)
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

@Suppress("MagicNumber")
fun roundYRot(yRot: Float, toNearest: Int): Float {
    return ((yRot % 360 / toNearest).roundToInt() * toNearest).toFloat()
}

