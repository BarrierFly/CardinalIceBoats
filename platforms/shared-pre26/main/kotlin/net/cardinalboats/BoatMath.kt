package net.cardinalboats

import kotlin.math.abs
import kotlin.math.roundToInt

// Pure boat math shared by every platform. No Minecraft types in this file so
// the unit tests can run without bootstrapping the game.
// Keep this file identical to shared-26's copy (no mappings-dependent code).

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
 * @return signed nudge in {-0.2, 0.0, +0.2}; positive means step right, along (-lx, -lz)
 */
@Suppress("MagicNumber")
fun dodgeNudge(leftBlocked: Boolean,
               rightBlocked: Boolean,
               sideOffset: Double,
               yawDiff: Float,
               leftExtendsToEdge: Boolean = false,
               rightExtendsToEdge: Boolean = false): Double {
    return when {
        !leftBlocked && rightBlocked  -> -0.2  // only the left lane is open -> step left
        !rightBlocked && leftBlocked  ->  0.2  // only the right lane is open -> step right
        !leftBlocked && !rightBlocked -> when {
            // Both side lanes are open: do NOT cancel the two ±0.2's. Lean toward
            // the side the boat already sits on; if it is centered, toward the
            // bow's yaw drift (defaulting to the right).
            abs(sideOffset) >= 0.02 -> if (sideOffset > 0) -0.2 else 0.2
            yawDiff < -0.5f         -> -0.2
            yawDiff >  0.5f         ->  0.2
            else                    ->  0.2
        }
        // Both lanes are blocked: if exactly one wall's collision is flush with
        // the lane edge, the other wall sits recessed and leaves a gap next to
        // the lane - step 0.2 toward that side to squeeze past it.
        leftExtendsToEdge && !rightExtendsToEdge  ->  0.2
        rightExtendsToEdge && !leftExtendsToEdge  -> -0.2
        else -> 0.0 // both walls flush or both recessed -> defer to the multi-block centering
    }
}
