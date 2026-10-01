package net.cardinalboats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoatMathTest {

    // ---- roundYRot ----

    @Test
    fun `roundYRot snaps to the nearest 90`() {
        assertEquals(0f, roundYRot(0f, 90))
        assertEquals(90f, roundYRot(89f, 90))
        assertEquals(90f, roundYRot(91f, 90))
        assertEquals(180f, roundYRot(179f, 90))
        assertEquals(-90f, roundYRot(-89f, 90))
        assertEquals(360f, roundYRot(359f, 90)) // 360 == 0 mod 360
    }

    @Test
    fun `roundYRot snaps to the nearest 45`() {
        assertEquals(45f, roundYRot(44f, 45))
        assertEquals(45f, roundYRot(46f, 45))
        assertEquals(0f, roundYRot(-22f, 45))
        assertEquals(-45f, roundYRot(-46f, 45))
    }

    @Test
    fun `roundYRot tie-breaks like Math round`() {
        // Halfway cases follow Math.round: .5 rounds up for positive halves.
        assertEquals(180f, roundYRot(135f, 90))
        assertEquals(-90f, roundYRot(-135f, 90))
    }

    // ---- isCardinallyAligned ----

    @Test
    fun `isCardinallyAligned allows a small epsilon`() {
        assertTrue(isCardinallyAligned(0f))
        assertTrue(isCardinallyAligned(90f))
        assertTrue(isCardinallyAligned(-90f))
        assertTrue(isCardinallyAligned(90.4f))
        assertTrue(isCardinallyAligned(359.8f)) // ~0°/360°
        assertTrue(isCardinallyAligned(-0.4f))
    }

    @Test
    fun `isCardinallyAligned rejects diagonals and larger drift`() {
        assertFalse(isCardinallyAligned(45f))
        assertFalse(isCardinallyAligned(-45f))
        assertFalse(isCardinallyAligned(90.6f))
        assertFalse(isCardinallyAligned(0.6f))
    }

    // ---- dodgeNudge ----
    // Sign convention: negative nudge steps left, positive steps right.

    @Test
    fun `dodge steps toward the only open side`() {
        assertEquals(-0.2, dodgeNudge(leftBlocked = false, rightBlocked = true, sideOffset = 0.0, yawDiff = 0f))
        assertEquals(0.2, dodgeNudge(leftBlocked = true, rightBlocked = false, sideOffset = 0.0, yawDiff = 0f))
        assertEquals(0.0, dodgeNudge(leftBlocked = true, rightBlocked = true, sideOffset = 0.0, yawDiff = 0f))
    }

    @Test
    fun `dodge with both lanes blocked steps toward the recessed wall`() {
        // Left wall flush with the lane, right wall recessed -> step right into the gap.
        assertEquals(0.2, dodgeNudge(true, true, sideOffset = 0.0, yawDiff = 0f,
                                     leftExtendsToEdge = true, rightExtendsToEdge = false))
        // Right wall flush, left wall recessed -> step left.
        assertEquals(-0.2, dodgeNudge(true, true, sideOffset = 0.0, yawDiff = 0f,
                                      leftExtendsToEdge = false, rightExtendsToEdge = true))
        // Both flush or both recessed -> unchanged: no nudge, multi-block centering decides.
        assertEquals(0.0, dodgeNudge(true, true, sideOffset = 0.0, yawDiff = 0f,
                                     leftExtendsToEdge = true, rightExtendsToEdge = true))
        assertEquals(0.0, dodgeNudge(true, true, sideOffset = 0.0, yawDiff = 0f,
                                     leftExtendsToEdge = false, rightExtendsToEdge = false))
        // The edge flags only matter when both lanes are blocked.
        assertEquals(-0.2, dodgeNudge(false, true, sideOffset = 0.0, yawDiff = 0f,
                                      leftExtendsToEdge = true, rightExtendsToEdge = true))
        assertEquals(0.2, dodgeNudge(true, false, sideOffset = 0.0, yawDiff = 0f,
                                     leftExtendsToEdge = true, rightExtendsToEdge = true))
    }

    @Test
    fun `dodge leans toward the side the boat sits on when both lanes are open`() {
        assertEquals(-0.2, dodgeNudge(false, false, sideOffset = 0.1, yawDiff = 0f))
        assertEquals(0.2, dodgeNudge(false, false, sideOffset = -0.1, yawDiff = 0f))
    }

    @Test
    fun `dodge uses bow drift only when centered`() {
        assertEquals(-0.2, dodgeNudge(false, false, sideOffset = 0.0, yawDiff = -2f))
        assertEquals(0.2, dodgeNudge(false, false, sideOffset = 0.0, yawDiff = 2f))
        // Straight bow defaults to the right.
        assertEquals(0.2, dodgeNudge(false, false, sideOffset = 0.0, yawDiff = 0f))
    }

    @Test
    fun `centered threshold is 0_02 blocks`() {
        // 0.019 counts as centered -> bow drift decides.
        assertEquals(-0.2, dodgeNudge(false, false, sideOffset = 0.019, yawDiff = -2f))
        // 0.021 counts as off-center -> in-block position decides.
        assertEquals(-0.2, dodgeNudge(false, false, sideOffset = 0.021, yawDiff = 2f))
        assertEquals(0.2, dodgeNudge(false, false, sideOffset = -0.021, yawDiff = -2f))
    }
}
