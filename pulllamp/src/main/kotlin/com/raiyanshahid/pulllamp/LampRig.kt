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

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A lamp hanging from a ceiling cable. It swings like a pendulum and drives the cord anchored to it.
 *
 * Pure Kotlin so it can be tested without a device. Angles are in radians, positive is clockwise.
 */
internal class LampPendulum(
    /** Pull back to rest, per second squared. Higher swings faster. */
    private val stiffness: Float = 34f,
    /** How quickly the swing dies down, per second. */
    private val dampingPerSecond: Float = 1.4f,
    private val maxAngle: Float = 0.45f,
) {
    var angle: Float = 0f
        private set
    var velocity: Float = 0f
        private set

    /** The angle the lamp is being held at (for example by a sideways tug on the cord). */
    var bias: Float = 0f
        set(value) {
            field = if (value.isFinite()) value.coerceIn(-maxAngle, maxAngle) else 0f
        }

    /** Gives the lamp a push; positive swings clockwise. */
    fun kick(impulse: Float) {
        if (impulse.isFinite()) velocity += impulse
    }

    fun reset() {
        angle = 0f
        velocity = 0f
        bias = 0f
    }

    fun step(dtSeconds: Float) {
        if (!dtSeconds.isFinite() || dtSeconds <= 0f) return
        var remaining = min(dtSeconds, 0.1f)
        while (remaining > 0f) {
            val dt = min(remaining, SUBSTEP)
            velocity += (-stiffness * (angle - bias)) * dt
            velocity *= exp(-dampingPerSecond * dt)
            angle = (angle + velocity * dt).coerceIn(-maxAngle, maxAngle)
            remaining -= dt
        }
    }

    private companion object {
        const val SUBSTEP = 1f / 240f
    }
}

/**
 * The lamp and its pull cord together: the cord hangs from a point on the lamp, so when the lamp
 * swings the cord is carried with it, and a sideways tug on the cord leans the lamp.
 *
 * Coordinates are in the lamp's own space with the pivot (where the ceiling cable meets the ceiling)
 * at [pivotX], [pivotY].
 */
internal class LampRig(
    cordLength: Float,
    val pivotX: Float,
    val pivotY: Float,
    /** Where the cord leaves the lamp, relative to the pivot, when the lamp hangs straight. */
    val anchorOffsetX: Float,
    val anchorOffsetY: Float,
) {
    val cord = CordSimulation(length = cordLength)
    val pendulum = LampPendulum()

    init {
        cord.reset(anchorWorldX(), anchorWorldY())
    }

    fun anchorWorldX(): Float = pivotX + anchorOffsetX * cos(pendulum.angle) - anchorOffsetY * sin(pendulum.angle)
    fun anchorWorldY(): Float = pivotY + anchorOffsetX * sin(pendulum.angle) + anchorOffsetY * cos(pendulum.angle)

    fun grab(px: Float, py: Float) = cord.grab(px, py)

    fun drag(px: Float, py: Float) = cord.drag(px, py)

    fun release() = cord.release()

    private var clicks = 0

    /** Returns true once per click of the switch. */
    fun consumePull(): Boolean {
        if (clicks <= 0) return false
        clicks--
        return true
    }

    fun step(dtSeconds: Float) {
        if (!dtSeconds.isFinite() || dtSeconds <= 0f) return
        // A held handle leans the lamp toward it; a free one lets the lamp swing back.
        pendulum.bias = if (cord.isGrabbed) {
            val lateral = (cord.handleX - cord.x[0]) / cord.length
            lateral * LEAN
        } else {
            0f
        }
        pendulum.step(dtSeconds)
        cord.setAnchor(anchorWorldX(), anchorWorldY())
        cord.step(dtSeconds)
        while (cord.consumePull()) {
            clicks++
            // The click is felt through the whole lamp.
            pendulum.kick(CLICK_KICK)
        }
    }

    /** True when nothing is moving any more, so the animation loop can go back to sleep. */
    fun isSettled(): Boolean =
        !cord.isGrabbed &&
            cord.maxSpeedPerStep() < SETTLE_SPEED &&
            kotlin.math.abs(pendulum.velocity) < SETTLE_SWING &&
            kotlin.math.abs(pendulum.angle) < SETTLE_SWING

    /** How taut the cord is, 0 (slack) to 1 (at the click point). Useful for animating the handle. */
    fun tension(): Float = min(1f, max(0f, cord.stretch / cord.pullThreshold))

    private companion object {
        const val LEAN = 0.5f
        const val SETTLE_SPEED = 0.02f
        const val SETTLE_SWING = 0.003f
        const val CLICK_KICK = 0.35f
    }
}
