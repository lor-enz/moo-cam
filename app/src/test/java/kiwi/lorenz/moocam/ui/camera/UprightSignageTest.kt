package kiwi.lorenz.moocam.ui.camera

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UprightSignageTest {
    @Test
    fun `a clear reading switches to the matching rotation`() {
        assertEquals(Surface.ROTATION_270, deviceRotationFor(90, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_180, deviceRotationFor(180, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_90, deviceRotationFor(270, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_0, deviceRotationFor(0, Surface.ROTATION_270))
    }

    @Test
    fun `a reading at the middle between two rotations keeps the current one`() {
        assertEquals(Surface.ROTATION_0, deviceRotationFor(45, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_270, deviceRotationFor(45, Surface.ROTATION_270))
    }

    @Test
    fun `switching needs the margin past the middle`() {
        assertEquals(Surface.ROTATION_0, deviceRotationFor(60, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_270, deviceRotationFor(61, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_270, deviceRotationFor(30, Surface.ROTATION_270))
        assertEquals(Surface.ROTATION_0, deviceRotationFor(29, Surface.ROTATION_270))
    }

    @Test
    fun `the margin holds across the wrap at 360 degrees`() {
        assertEquals(Surface.ROTATION_0, deviceRotationFor(300, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_90, deviceRotationFor(299, Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_90, deviceRotationFor(330, Surface.ROTATION_90))
        assertEquals(Surface.ROTATION_0, deviceRotationFor(331, Surface.ROTATION_90))
    }

    @Test
    fun `a jitter around the switch point changes the rotation only once`() {
        var rotation = Surface.ROTATION_0
        var switches = 0
        for (orientation in listOf(58, 62, 58, 62, 45, 62, 40, 58, 44)) {
            val next = deviceRotationFor(orientation, rotation)
            if (next != rotation) switches++
            rotation = next
        }
        assertEquals(1, switches)
        assertEquals(Surface.ROTATION_270, rotation)
    }

    @Test
    fun `turns take the short way round`() {
        assertEquals(90f, shortestTurn(270f, 0f), 0f)
        assertEquals(-90f, shortestTurn(0f, 270f), 0f)
        assertEquals(90f, shortestTurn(0f, 90f), 0f)
        assertEquals(-90f, shortestTurn(-270f, 0f - 360f), 0f)
    }

    @Test
    fun `only angles towards landscape are sideways`() {
        listOf(90f, 270f, -90f, 450f, 46f).forEach { assertTrue("$it", isSideways(it)) }
        listOf(0f, 180f, 360f, -180f, 44f).forEach { assertFalse("$it", isSideways(it)) }
    }

    @Test
    fun `a half turn stays a half turn`() {
        assertEquals(180f, kotlin.math.abs(shortestTurn(0f, 180f)), 0f)
    }
}
