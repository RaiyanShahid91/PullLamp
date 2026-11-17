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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Holds whether the lamp is on, and runs the light switching animation.
 *
 * "On" means light mode and "off" means dark mode. Read [isOn] (or [isDark]) when you choose your
 * theme. Create one with [rememberLampState] and hand it to [LampScene] or [PullLamp].
 */
@Stable
public class LampState internal constructor(
    initialOn: Boolean,
    private val revealMillis: Int,
) {
    /** True while the light is on (light mode). Changes the moment the cord is pulled. */
    public var isOn: Boolean by mutableStateOf(initialOn)
        private set

    /** True while the light is off (dark mode). */
    public val isDark: Boolean get() = !isOn

    /** True while the light is spreading across, or fading from, the screen. */
    public var isTransitioning: Boolean by mutableStateOf(false)
        private set

    /** How bright the bulb is, 0 to 1. Flickers briefly when it comes on. */
    internal val light = Animatable(if (initialOn) 1f else 0f)

    /** Progress of the circle of light spreading from the bulb, 0 to 1. */
    internal val reveal = Animatable(1f)

    /** The screen as it looked just before the switch, shown outside the spreading circle. */
    internal var snapshot: ImageBitmap? by mutableStateOf(null)

    internal var bulbInWindow: Offset by mutableStateOf(Offset.Unspecified)
    internal var sceneInWindow: Offset by mutableStateOf(Offset.Unspecified)
    internal var layer: GraphicsLayer? = null

    /** The bulb's position inside the scene, or [Offset.Unspecified] if it is not known yet. */
    internal fun origin(): Offset =
        if (bulbInWindow.isSpecified && sceneInWindow.isSpecified) bulbInWindow - sceneInWindow else Offset.Unspecified

    /**
     * Switches the light. Does nothing if a switch is already in progress.
     *
     * If the lamp is inside a [LampScene] the new look spreads out from the bulb. Otherwise the change
     * is immediate and only the bulb animates.
     */
    public suspend fun toggle() {
        if (isTransitioning) return
        isTransitioning = true
        try {
            val target = !isOn
            val shot = snapshotOrNull()
            snapshot = shot
            isOn = target
            reveal.snapTo(if (shot != null) 0f else 1f)
            coroutineScope {
                launch { animateLight(target) }
                if (shot != null) {
                    launch { reveal.animateTo(1f, tween(revealMillis, easing = SpreadEasing)) }
                }
            }
        } finally {
            snapshot = null
            reveal.snapTo(1f)
            isTransitioning = false
        }
    }

    /** Puts the light on or off, animating only if that is a change. */
    public suspend fun setOn(on: Boolean) {
        if (on != isOn) toggle()
    }

    private suspend fun snapshotOrNull(): ImageBitmap? {
        val l = layer ?: return null
        if (!origin().isSpecified || revealMillis <= 0) return null
        return try {
            l.toImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun animateLight(on: Boolean) {
        if (on) {
            light.animateTo(
                1f,
                keyframes {
                    durationMillis = 420
                    0.85f at 50
                    0.2f at 110
                    0.95f at 180
                    0.5f at 250
                    1f at 420
                },
            )
        } else {
            light.animateTo(0f, tween(220))
        }
    }

    private companion object {
        val SpreadEasing = CubicBezierEasing(0.25f, 0.05f, 0.2f, 1f)
    }
}

/**
 * Creates and remembers a [LampState].
 *
 * @param initialOn whether the light starts on (light mode).
 * @param revealMillis how long the new look takes to spread across the screen. Use 0 to switch
 * instantly.
 */
@Composable
public fun rememberLampState(initialOn: Boolean = true, revealMillis: Int = 900): LampState {
    require(revealMillis >= 0) { "revealMillis must not be negative, was $revealMillis" }
    return remember { LampState(initialOn, revealMillis) }
}
