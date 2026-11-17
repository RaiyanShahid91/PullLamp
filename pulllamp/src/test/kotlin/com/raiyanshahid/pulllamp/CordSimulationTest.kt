/*
 * Copyright 2026 Raiyan Shahid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.raiyanshahid.pulllamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class CordSimulationTest {

    private val len = 100f

    private fun cord() = CordSimulation(length = len).also { it.reset(0f, 0f) }

    private fun run(c: CordSimulation, seconds: Float, fps: Int = 60) {
        repeat((seconds * fps).toInt()) { c.step(1f / fps) }
    }

    private fun allFinite(c: CordSimulation) = c.x.all { it.isFinite() } && c.y.all { it.isFinite() }

    @Test
    fun hangsStraightDownAtRest() {
        val c = cord()
        run(c, 3f)
        assertEquals(0f, c.handleX, 1f)
        assertEquals(len, c.handleY, len * 0.02f)
    }

    @Test
    fun rejectsBadLength() {
        try {
            CordSimulation(length = 0f)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun draggedHandleFollowsThePointer() {
        val c = cord()
        c.grab(0f, len)
        c.drag(0f, len * 1.3f)
        run(c, 1f)
        assertEquals(len * 1.3f, c.handleY, 0.5f)
        assertTrue(c.stretch > 0f)
    }

    @Test
    fun pullDistanceIsLimited() {
        val c = cord()
        c.grab(0f, len)
        c.drag(0f, len * 10f)
        run(c, 1f)
        val reach = hypot(c.handleX, c.handleY)
        assertTrue("reach was $reach", reach <= len * (1f + c.maxPullFraction) + 0.5f)
    }

    @Test
    fun springsBackAfterRelease() {
        val c = cord()
        c.grab(0f, len)
        c.drag(0f, len * 1.4f)
        run(c, 0.5f)
        c.release()
        run(c, 4f)
        assertEquals(len, c.handleY, len * 0.03f)
        assertEquals(0f, c.handleX, len * 0.03f)
    }

    @Test
    fun clicksOnceWhenPulledPastTheThreshold() {
        val c = cord()
        c.grab(0f, len)
        c.drag(0f, len * 1.4f)
        run(c, 1f)
        assertTrue(c.consumePull())
        assertFalse("a held pull must click only once", c.consumePull())
        // Holding it further down does not click again.
        c.drag(0f, len * 1.45f)
        run(c, 1f)
        assertFalse(c.consumePull())
    }

    @Test
    fun shortTugDoesNotClick() {
        val c = cord()
        c.grab(0f, len)
        c.drag(0f, len * 1.08f)
        run(c, 1f)
        c.release()
        run(c, 2f)
        assertFalse(c.consumePull())
    }

    @Test
    fun sidewaysTugDoesNotClick() {
        val c = cord()
        c.grab(0f, len)
        c.drag(len * 0.8f, len * 0.7f)
        run(c, 1f)
        c.release()
        run(c, 2f)
        assertFalse(c.consumePull())
    }

    @Test
    fun clicksAgainOnTheNextPull() {
        val c = cord()
        repeat(2) {
            c.grab(0f, len)
            c.drag(0f, len * 1.4f)
            run(c, 0.6f)
            assertTrue("pull ${it + 1} should click", c.consumePull())
            c.release()
            run(c, 3f)
        }
    }

    @Test
    fun staysFiniteWithHugeStepsAndWildInput() {
        val c = cord()
        c.grab(1e9f, -1e9f)
        c.step(10f)
        c.step(Float.MAX_VALUE)
        c.step(Float.NaN)
        c.drag(Float.NaN, Float.POSITIVE_INFINITY)
        c.step(0.016f)
        c.setAnchor(Float.NaN, 0f)
        c.step(0.016f)
        assertTrue(allFinite(c))
    }

    @Test
    fun isDeterministic() {
        fun trial(): Pair<Float, Float> {
            val c = cord()
            c.grab(0f, len)
            c.drag(30f, len * 1.3f)
            run(c, 0.7f)
            c.release()
            run(c, 1.2f)
            return c.handleX to c.handleY
        }
        assertEquals(trial(), trial())
    }

    @Test
    fun followsAMovingAnchor() {
        val c = cord()
        c.setAnchor(100f, 0f)
        run(c, 3f)
        assertEquals(100f, c.handleX, len * 0.03f)
        assertEquals(len, c.handleY, len * 0.03f)
    }

    @Test
    fun settlesToAStop() {
        val c = cord()
        c.grab(0f, len)
        c.drag(60f, len * 1.2f)
        run(c, 0.4f)
        c.release()
        run(c, 12f)
        assertTrue(c.maxSpeedPerStep() < 0.01f)
    }

    @Test
    fun segmentsKeepTheirLengthWhenSlack() {
        val c = cord()
        c.grab(0f, len)
        c.drag(40f, len * 0.6f)
        run(c, 1f)
        val seg = len / c.segmentCount
        for (i in 0 until c.segmentCount) {
            val d = hypot(c.x[i + 1] - c.x[i], c.y[i + 1] - c.y[i])
            assertTrue("segment $i is $d", abs(d - seg) < seg * 0.25f)
        }
    }
}
