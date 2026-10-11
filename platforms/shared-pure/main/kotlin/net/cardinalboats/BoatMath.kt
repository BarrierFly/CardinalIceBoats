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
 * Walk one column downward from the boat's nominal surface level to find how
 * far the rideable surface has dropped. Depth 0 is the nominal level; the
 * column is searched maxDrop steps down. Callers translate each depth d into
 * block offsets and feed them back through the probes, which take a Y offset
 * relative to the boat's block: the nominal surface level is -1 for ice and 0
 * (the boat's own layer) for water.
 *
 * A hit deeper than the nominal level only counts when every cell from just
 * above the found block up to the boat's own layer is collision-free - a hit
 * under a collision is a step/wall, not a slope, and reads as no surface.
 * Depth 0 skips the clearance walk so flat lanes probe exactly as before 2.3.
 * Raised surfaces (uphill) are out of scope by design: they need upward
 * momentum, a drop does not.
 *
 * @param maxDrop    how many blocks below the nominal level to search;
 *                   0 disables downhill probing entirely
 * @return the drop depth in blocks (0 = surface at the nominal level), or
 *         null when no reachable surface exists within maxDrop
 */
fun surfaceDropDepth(maxDrop: Int,
                     isIceAt: (Int) -> Boolean,
                     isWaterAt: (Int) -> Boolean,
                     collidesAt: (Int) -> Boolean): Int? {
    for (depth in 0..maxDrop) {
        val iceY = -1 - depth
        val waterY = -depth
        val hitY = when {
            isIceAt(iceY)     -> iceY
            isWaterAt(waterY) -> waterY
            else -> continue
        }
        if (depth == 0) return 0
        for (y in hitY + 1..0) {
            if (collidesAt(y)) return null
        }
        return depth
    }
    return null
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
            // toward the side the boat already sits on; if it is centered, counter
            // the bow's yaw drift to straighten the bow (a straight bow defaults
            // to +NUDGE_STEP). Sign note: a positive nudge steps along (-lx, -lz),
            // where lx is the code "left" step, which actually points to the
            // player's RIGHT - so callers see positive = player-left.
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
