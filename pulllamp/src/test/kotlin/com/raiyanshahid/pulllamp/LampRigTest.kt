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

class LampRigTest {

    private fun rig() = LampRig(
        cordLength = 100f,
        pivotX = 60f,
        pivotY = 0f,
        anchorOffsetX = 30f,
        anchorOffsetY = 90f,
    )

    private fun run(r: LampRig, seconds: Float, fps: Int = 60) {
        repeat((seconds * fps).toInt()) { r.step(1f / fps) }
    }

    @Test
    fun hangsStillAtRest() {
        val r = rig()
        run(r, 3f)
        assertEquals(0f, r.pendulum.angle, 1e-3f)
        assertEquals(90f, r.cord.x[0], 1f)
        assertEquals(190f, r.cord.handleY, 4f)
    }

    @Test
    fun clickKicksTheLampAndItSettlesAgain() {
        val r = rig()
        r.grab(r.cord.handleX, r.cord.handleY)
        r.drag(r.cord.x[0], r.cord.y[0] + 100f * 1.4f)
        run(r, 0.5f)
        assertTrue(r.consumePull())
        assertFalse(r.consumePull())
        assertTrue("lamp should have moved", abs(r.pendulum.angle) > 1e-3f || abs(r.pendulum.velocity) > 1e-3f)
        r.release()
        run(r, 8f)
        assertEquals(0f, r.pendulum.angle, 0.01f)
    }

    @Test
    fun sidewaysTugLeansTheLamp() {
        val r = rig()
        r.grab(r.cord.handleX, r.cord.handleY)
        r.drag(r.cord.x[0] + 80f, r.cord.y[0] + 60f)
        run(r, 1f)
        assertTrue("lamp should lean toward the pull, angle=${r.pendulum.angle}", r.pendulum.angle > 0.02f)
        r.release()
        run(r, 8f)
        assertEquals(0f, r.pendulum.angle, 0.01f)
    }

    @Test
    fun cordAnchorFollowsTheSwingingLamp() {
        val r = rig()
        r.pendulum.kick(2f)
        var moved = false
        repeat(60) {
            r.step(1f / 60)
            if (abs(r.cord.x[0] - 90f) > 1f) moved = true
            assertEquals(r.anchorWorldX(), r.cord.x[0], 0.01f)
        }
        assertTrue(moved)
    }

    @Test
    fun goesBackToSleepAfterAPull() {
        val r = rig()
        assertTrue(r.isSettled())
        r.grab(r.cord.handleX, r.cord.handleY)
        r.drag(r.cord.x[0], r.cord.y[0] + 100f * 1.4f)
        run(r, 0.5f)
        assertFalse(r.isSettled())
        r.release()
        run(r, 12f)
        assertTrue(r.isSettled())
    }

    @Test
    fun pendulumStaysWithinItsLimit() {
        val p = LampPendulum()
        p.kick(1000f)
        repeat(600) { p.step(1f / 60) }
        assertTrue(abs(p.angle) <= 0.45f + 1e-4f)
        assertTrue(p.angle.isFinite() && p.velocity.isFinite())
    }

    @Test
    fun pendulumIgnoresBadInput() {
        val p = LampPendulum()
        p.kick(Float.NaN)
        p.bias = Float.NaN
        p.step(Float.NaN)
        p.step(-1f)
        assertEquals(0f, p.angle, 0f)
    }
}
