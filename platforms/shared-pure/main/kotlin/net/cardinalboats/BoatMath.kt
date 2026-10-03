package net.cardinalboats

import kotlin.math.abs
import kotlin.math.roundToInt

// Pure boat math shared by every platform. No Minecraft types in this file, so
// it lives in shared-pure and every platform compiles this single copy - there
// is no per-mapping twin to keep in sync. The unit tests run without
// bootstrapping the game.

// Dodge decision constants. The nudge magnitude is part of the documented
// public contract (README and the tests pin these values).
private const val NUDGE_STEP = 0.2
private const val CENTERED_EPSILON = 0.02
private const val YAW_DRIFT_EPSILON = 0.5f

@Suppress("MagicNumber")
fun roundYRot(yRot: Float, toNearest: Int): Float {
    return ((yRot % 360 / toNearest).roundToInt() * toNearest).toFloat()
}

@Suppress("MagicNumber")
fun isCardinallyAligned(yRot: Float): Boolean {
    // Small tolerance instead of an exact float comparison: physics and server
    // sync can leave the yaw a fraction of a degree off a cardinal angle.
    return abs(yRot - roundYRot(yRot, 90)) < 0.5f
}

/**
 * Decide the smart-center dodge direction.
 *
 * @param leftBlocked  the left side lane is not usable (wall, or no ice/water surface)
 * @param rightBlocked the right side lane is not usable (wall, or no ice/water surface)
 * @param sideOffset   signed offset of the boat inside its lane block; > 0 means it sits on the left half
 * @param yawDiff      bow yaw minus lane facing, wrapped into (-180, 180]
 * @param leftExtendsToEdge  when both lanes are blocked: the left wall's collision reaches the
 *                           edge it shares with the boat's lane, leaving no gap to slip into on the left
 * @param rightExtendsToEdge when both lanes are blocked: the right wall's collision reaches the
 *                           edge it shares with the boat's lane, leaving no gap to slip into on the right
 * @return signed nudge in {-NUDGE_STEP, 0.0, +NUDGE_STEP}; positive means step right, along (-lx, -lz)
 */
fun dodgeNudge(leftBlocked: Boolean,
               rightBlocked: Boolean,
               sideOffset: Double,
               yawDiff: Float,
               leftExtendsToEdge: Boolean = false,
               rightExtendsToEdge: Boolean = false): Double {
    return when {
        !leftBlocked && rightBlocked  -> -NUDGE_STEP  // only the left lane is open -> step left
        !rightBlocked && leftBlocked  ->  NUDGE_STEP  // only the right lane is open -> step right
        !leftBlocked && !rightBlocked -> when {
            // Both side lanes are open: do NOT cancel the two ±NUDGE_STEP's. Lean
            // toward the side the boat already sits on; if it is centered, toward
            // the bow's yaw drift (defaulting to the right).
            abs(sideOffset) >= CENTERED_EPSILON -> if (sideOffset > 0) -NUDGE_STEP else NUDGE_STEP
            yawDiff < -YAW_DRIFT_EPSILON        -> -NUDGE_STEP
            yawDiff >  YAW_DRIFT_EPSILON        ->  NUDGE_STEP
            else                                ->  NUDGE_STEP
        }
        // Both lanes are blocked: exactly one wall being flush with the lane edge
        // means the gap is on the opposite side, so step toward the gap. The flush
        // wall names the side to step AWAY from: right flush -> step left.
        leftExtendsToEdge != rightExtendsToEdge -> if (rightExtendsToEdge) -NUDGE_STEP else NUDGE_STEP
        else -> 0.0 // both walls flush or both recessed -> defer to the multi-block centering
    }
}
