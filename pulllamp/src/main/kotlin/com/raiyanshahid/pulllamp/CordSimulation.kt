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

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * A hanging cord simulated as a chain of points (position-based Verlet integration).
 *
 * The first point is the anchor and is moved by the caller. The last point is the handle: it can be
 * grabbed and dragged. A cord is slightly elastic, so a pulled cord stretches a little and springs back
 * when it is let go. All values are in the caller's units (the Compose layer uses pixels); gravity and
 * thresholds scale with [length], so the cord feels the same at any size.
 *
 * This class has no Android dependencies and is deterministic, which is why it is tested with plain
 * JUnit.
 */
internal class CordSimulation(
    /** Natural length of the cord from anchor to handle. Must be positive. */
    val length: Float,
    val segmentCount: Int = 14,
    /** Gravity in cord lengths per second squared. */
    private val gravityLengths: Float = 16f,
    /** How quickly motion fades, per second. */
    private val dampingPerSecond: Float = 2.8f,
    /** 0 = a rope that stretches freely, 1 = an inextensible rope. */
    private val stretchStiffness: Float = 0.22f,
    /** How far past [length] the handle may be pulled, as a fraction of [length]. */
    val maxPullFraction: Float = 0.5f,
    /** How far past [length] the handle must be pulled for the switch to click, as a fraction of [length]. */
    val pullThresholdFraction: Float = 0.2f,
) {
    init {
        require(length > 0f && length.isFinite()) { "length must be positive, was $length" }
        require(segmentCount >= 2) { "segmentCount must be at least 2, was $segmentCount" }
        require(stretchStiffness in 0.01f..1f) { "stretchStiffness must be in 0.01..1" }
        require(maxPullFraction > pullThresholdFraction) { "maxPullFraction must exceed pullThresholdFraction" }
    }

    private val pointCount = segmentCount + 1
    private val segmentLength = length / segmentCount

    val x = FloatArray(pointCount)
    val y = FloatArray(pointCount)
    private val prevX = FloatArray(pointCount)
    private val prevY = FloatArray(pointCount)

    private var grabbed = false
    private var targetX = 0f
    private var targetY = 0f
    private var armed = true
    private var pendingPulls = 0
    private var accumulator = 0f

    /** True while the handle is held. */
    val isGrabbed: Boolean get() = grabbed

    val handleX: Float get() = x[pointCount - 1]
    val handleY: Float get() = y[pointCount - 1]

    /** Distance the cord is stretched beyond its natural length (never negative). */
    val stretch: Float
        get() = max(0f, hypot(x[pointCount - 1] - x[0], y[pointCount - 1] - y[0]) - length)

    /** Stretch needed for the click, in the same units as [length]. */
    val pullThreshold: Float get() = pullThresholdFraction * length

    /** Hangs the cord straight down from the given anchor and stops all motion. */
    fun reset(anchorX: Float, anchorY: Float) {
        for (i in 0 until pointCount) {
            x[i] = anchorX
            y[i] = anchorY + i * segmentLength
            prevX[i] = x[i]
            prevY[i] = y[i]
        }
        grabbed = false
        armed = true
        pendingPulls = 0
        accumulator = 0f
    }

    /** Moves the anchor; the rest of the cord follows through the constraints. */
    fun setAnchor(anchorX: Float, anchorY: Float) {
        if (!anchorX.isFinite() || !anchorY.isFinite()) return
        x[0] = anchorX
        y[0] = anchorY
        prevX[0] = anchorX
        prevY[0] = anchorY
    }

    /** Takes hold of the handle and moves it to the given point. */
    fun grab(px: Float, py: Float) {
        if (!px.isFinite() || !py.isFinite()) return
        grabbed = true
        targetX = px
        targetY = py
    }

    /** Moves the held handle. Ignored if the handle is not held. */
    fun drag(px: Float, py: Float) {
        if (!grabbed || !px.isFinite() || !py.isFinite()) return
        targetX = px
        targetY = py
    }

    /** Lets go of the handle. */
    fun release() {
        grabbed = false
    }

    /** Returns true once for every click that happened since the last call. */
    fun consumePull(): Boolean {
        if (pendingPulls <= 0) return false
        pendingPulls--
        return true
    }

    /** Advances the simulation by [dtSeconds] (any size; it is sliced into fixed steps). */
    fun step(dtSeconds: Float) {
        if (!dtSeconds.isFinite() || dtSeconds <= 0f) return
        accumulator += min(dtSeconds, MAX_FRAME)
        var guard = 0
        while (accumulator >= SUBSTEP && guard < MAX_SUBSTEPS) {
            substep(SUBSTEP)
            accumulator -= SUBSTEP
            guard++
        }
        if (guard == MAX_SUBSTEPS) accumulator = 0f
    }

    private fun substep(dt: Float) {
        val damping = exp(-dampingPerSecond * dt)
        val g = gravityLengths * length * dt * dt
        for (i in 1 until pointCount) {
            val vx = (x[i] - prevX[i]) * damping
            val vy = (y[i] - prevY[i]) * damping
            prevX[i] = x[i]
            prevY[i] = y[i]
            x[i] += vx + g * SIDE_BIAS
            y[i] += vy + g
        }
        val last = pointCount - 1
        if (grabbed) pinHandle()
        for (iteration in 0 until ITERATIONS) {
            if (iteration % 2 == 0) {
                for (i in 0 until segmentCount) solve(i, last)
            } else {
                for (i in segmentCount - 1 downTo 0) solve(i, last)
            }
            for (i in 0 until pointCount - 2) bend(i, last)
            if (grabbed) pinHandle()
        }
        updateClick()
    }

    private fun pinHandle() {
        val last = pointCount - 1
        var dx = targetX - x[0]
        var dy = targetY - y[0]
        val reach = length * (1f + maxPullFraction)
        val d = hypot(dx, dy)
        if (d > reach) {
            val k = reach / d
            dx *= k
            dy *= k
        }
        x[last] = x[0] + dx
        y[last] = y[0] + dy
        prevX[last] = x[last]
        prevY[last] = y[last]
    }

    private fun solve(i: Int, last: Int) {
        val j = i + 1
        val dx = x[j] - x[i]
        val dy = y[j] - y[i]
        val d = hypot(dx, dy)
        if (d < 1e-6f) return
        val diff = (d - segmentLength) / d
        val stiff = if (d > segmentLength) stretchStiffness else 1f
        val w0 = if (i == 0) 0f else 1f
        val w1 = if (j == last && grabbed) 0f else 1f
        val wSum = w0 + w1
        if (wSum == 0f) return
        val cx = dx * diff * stiff
        val cy = dy * diff * stiff
        x[i] += cx * (w0 / wSum)
        y[i] += cy * (w0 / wSum)
        x[j] -= cx * (w1 / wSum)
        y[j] -= cy * (w1 / wSum)
    }

    /**
     * Keeps a point from folding back onto the one two places before it. Without this a cord that is
     * perfectly vertical can settle with its tip doubled over, which a real cord never does.
     */
    private fun bend(i: Int, last: Int) {
        val j = i + 2
        val dx = x[j] - x[i]
        val dy = y[j] - y[i]
        val d = hypot(dx, dy)
        val minimum = segmentLength * MIN_SPAN
        if (d >= minimum) return
        val nx: Float
        val ny: Float
        if (d < 1e-6f) {
            // Exactly on top of each other: push apart along a fixed direction so it is deterministic.
            nx = 0.0f
            ny = 1f
        } else {
            nx = dx / d
            ny = dy / d
        }
        val push = (minimum - d) * BEND_STIFFNESS
        val w0 = if (i == 0) 0f else 1f
        val w1 = if (j == last && grabbed) 0f else 1f
        val wSum = w0 + w1
        if (wSum == 0f) return
        x[i] -= nx * push * (w0 / wSum)
        y[i] -= ny * push * (w0 / wSum)
        x[j] += nx * push * (w1 / wSum)
        y[j] += ny * push * (w1 / wSum)
    }

    private fun updateClick() {
        val s = stretch
        if (grabbed && armed && s >= pullThreshold) {
            armed = false
            pendingPulls++
        } else if (!armed && s < pullThreshold * REARM) {
            armed = true
        }
    }

    /** Largest distance any point moved in the last step; handy for "has it settled?" checks. */
    fun maxSpeedPerStep(): Float {
        var m = 0f
        for (i in 1 until pointCount) {
            m = max(m, max(abs(x[i] - prevX[i]), abs(y[i] - prevY[i])))
        }
        return m
    }

    private companion object {
        const val SUBSTEP = 1f / 240f
        const val MAX_SUBSTEPS = 24
        const val MAX_FRAME = 0.1f
        const val ITERATIONS = 12
        const val REARM = 0.5f
        const val MIN_SPAN = 1.4f
        // A whisper of sideways pull so a perfectly vertical cord can buckle instead of folding in line.
        const val SIDE_BIAS = 0.004f
        const val BEND_STIFFNESS = 0.5f
    }
}
