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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2

/**
 * A lamp that hangs from the top edge and switches [state] when its cord is pulled.
 *
 * Drag the bead at the end of the cord down; at the click point the light switches and the lamp gives
 * a little swing. Let go and the cord springs back. A tap on the shade or the bead pulls the cord for
 * you, and screen readers see the lamp as a switch, so nobody needs to drag.
 *
 * Use [LampScene] to get the spreading light and the cone of light across your content. On its own
 * this composable only draws the lamp. The lamp's rectangle takes touches, so keep buttons out of that
 * corner.
 */
@Composable
public fun PullLamp(
    state: LampState,
    modifier: Modifier = Modifier,
    style: LampStyle = LampStyle(),
    haptics: Boolean = true,
) {
    val density = LocalDensity.current
    val metrics = remember(density.density, style.scale) { LampMetrics(density.density * style.scale) }
    val rig = remember(metrics) {
        LampRig(
            cordLength = metrics.cordLength,
            pivotX = metrics.pivotX,
            pivotY = 0f,
            anchorOffsetX = metrics.anchorOffsetX,
            anchorOffsetY = metrics.anchorOffsetY,
        )
    }
    var frame by remember { mutableIntStateOf(0) }
    var awake by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled by rememberUpdatedState(haptics)

    // Runs only while something moves, so an idle lamp costs nothing.
    LaunchedEffect(rig, awake) {
        var last = 0L
        var calmFrames = 0
        while (calmFrames < CALM_FRAMES) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) FIRST_FRAME else ((now - last) / 1_000_000_000f).coerceAtMost(MAX_FRAME)
                last = now
                rig.step(dt)
                if (rig.consumePull()) {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { state.toggle() }
                }
                frame++
                calmFrames = if (rig.isSettled()) calmFrames + 1 else 0
            }
        }
    }

    fun yank() {
        scope.launch {
            if (rig.cord.isGrabbed) return@launch
            rig.grab(rig.cord.handleX, rig.cord.handleY)
            awake++
            rig.drag(rig.cord.x[0], rig.cord.y[0] + rig.cord.length * (1f + YANK_PULL))
            delay(YANK_HOLD_MILLIS)
            rig.release()
            awake++
        }
    }

    val widthDp = with(density) { metrics.width.toDp() }
    val heightDp = with(density) { metrics.height.toDp() }
    val description = style.contentDescription
    val stateText = if (state.isOn) style.onDescription else style.offDescription

    Canvas(
        modifier
            .size(widthDp, heightDp)
            .onGloballyPositioned { coords ->
                state.bulbInWindow = coords.positionInWindow() + Offset(metrics.bulbX, metrics.bulbY)
            }
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = description
                stateDescription = stateText
                toggleableState = ToggleableState(state.isOn)
                onClick(label = "Pull the cord") {
                    yank()
                    true
                }
            }
            .pointerInput(rig, metrics) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val onHandle = (down.position - Offset(rig.cord.handleX, rig.cord.handleY)).getDistance() <= metrics.grabRadius
                    if (!onHandle) {
                        // A tap on the shade pulls the cord for you; anywhere else is ignored.
                        val onShade = kotlin.math.abs(down.position.x - metrics.pivotX) <= metrics.shadeBottomHalfWidth &&
                            down.position.y <= metrics.shadeBottom + metrics.rimHeight
                        if (onShade) {
                            val up = waitForUpOrCancellation()
                            if (up != null) yank()
                        }
                        return@awaitEachGesture
                    }
                    down.consume()
                    rig.grab(down.position.x, down.position.y)
                    awake++
                    var farthest = 0f
                    drag(down.id) { change ->
                        change.consume()
                        rig.drag(change.position.x, change.position.y)
                        farthest = maxOf(farthest, (change.position - down.position).getDistance())
                    }
                    rig.release()
                    awake++
                    // A tap on the bead that never moved counts as a pull.
                    if (farthest < viewConfiguration.touchSlop) yank()
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val redrawTick = frame // reading it makes the canvas redraw each frame while the lamp moves
        drawLamp(rig, metrics, style, state.light.value)
    }
}

private const val CALM_FRAMES = 20
private const val FIRST_FRAME = 1f / 60f
private const val MAX_FRAME = 0.05f
private const val YANK_PULL = 0.34f
private const val YANK_HOLD_MILLIS = 150L

private fun DrawScope.drawLamp(rig: LampRig, m: LampMetrics, style: LampStyle, light: Float) {
    val px = m.px
    val darkBrass = lerp(style.brassColor, Color.Black, 0.45f)
    val lightBrass = lerp(style.brassColor, Color.White, 0.55f)

    // Ceiling canopy.
    drawRoundRect(
        color = style.cableColor,
        topLeft = Offset(m.pivotX - 14f * px, 0f),
        size = Size(28f * px, 7f * px),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * px, 3f * px),
    )

    // Everything attached to the cable swings together.
    rotate(degrees = Math.toDegrees(rig.pendulum.angle.toDouble()).toFloat(), pivot = Offset(m.pivotX, 0f)) {
        drawLine(
            color = style.cableColor,
            start = Offset(m.pivotX, 6f * px),
            end = Offset(m.pivotX, m.shadeTop),
            strokeWidth = 2f * px,
            cap = StrokeCap.Round,
        )

        // Warm bloom behind the bulb.
        if (light > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(style.lightColor.copy(alpha = 0.55f * light), Color.Transparent),
                    center = Offset(m.bulbX, m.bulbY),
                    radius = 62f * px,
                ),
                radius = 62f * px,
                center = Offset(m.bulbX, m.bulbY),
            )
        }

        // Bulb.
        val bulbOff = Color(0xFFB9BEC6)
        val bulbOn = Color(0xFFFFF3CC)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lerp(Color.White, bulbOn, light), lerp(bulbOff, style.lightColor, light)),
                center = Offset(m.bulbX - 3f * px, m.bulbY + 1f * px),
                radius = m.bulbRadius * 1.3f,
            ),
            radius = m.bulbRadius,
            center = Offset(m.bulbX, m.bulbY),
        )

        // Shade: a cone with a light edge down the middle-left and a dark edge on the right.
        val shade = Path().apply {
            moveTo(m.pivotX - m.shadeTopHalfWidth, m.shadeTop)
            lineTo(m.pivotX + m.shadeTopHalfWidth, m.shadeTop)
            lineTo(m.pivotX + m.shadeBottomHalfWidth, m.shadeBottom)
            lineTo(m.pivotX - m.shadeBottomHalfWidth, m.shadeBottom)
            close()
        }
        val base = style.shadeColor
        drawPath(
            path = shade,
            brush = Brush.horizontalGradient(
                0f to lerp(base, Color.Black, 0.35f),
                0.28f to base,
                0.42f to lerp(base, Color.White, 0.28f),
                0.7f to base,
                1f to lerp(base, Color.Black, 0.45f),
                startX = m.pivotX - m.shadeBottomHalfWidth,
                endX = m.pivotX + m.shadeBottomHalfWidth,
            ),
        )

        // Rim: its inside glows when the light is on.
        val rim = Rect(
            m.pivotX - m.shadeBottomHalfWidth,
            m.shadeBottom - m.rimHeight / 2f,
            m.pivotX + m.shadeBottomHalfWidth,
            m.shadeBottom + m.rimHeight / 2f,
        )
        drawOval(color = lerp(Color(0xFF17181B), style.lightColor, light), topLeft = rim.topLeft, size = rim.size)
        drawOval(color = darkBrass, topLeft = rim.topLeft, size = rim.size, style = Stroke(width = 1.4f * px))

        // Brass cap where the cable enters.
        drawRoundRect(
            color = style.brassColor,
            topLeft = Offset(m.pivotX - m.shadeTopHalfWidth - 2f * px, m.shadeTop - 4f * px),
            size = Size(m.shadeTopHalfWidth * 2f + 4f * px, 8f * px),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * px, 4f * px),
        )
        drawLine(
            color = lightBrass.copy(alpha = 0.8f),
            start = Offset(m.pivotX - m.shadeTopHalfWidth, m.shadeTop - 2f * px),
            end = Offset(m.pivotX + m.shadeTopHalfWidth * 0.2f, m.shadeTop - 2f * px),
            strokeWidth = 1.2f * px,
            cap = StrokeCap.Round,
        )
    }

    drawCord(rig.cord, m, style, darkBrass, lightBrass)
}

private fun DrawScope.drawCord(cord: CordSimulation, m: LampMetrics, style: LampStyle, darkBrass: Color, lightBrass: Color) {
    val n = cord.segmentCount
    val path = Path().apply {
        moveTo(cord.x[0], cord.y[0])
        for (i in 1 until n) {
            val mx = (cord.x[i] + cord.x[i + 1]) / 2f
            val my = (cord.y[i] + cord.y[i + 1]) / 2f
            quadraticTo(cord.x[i], cord.y[i], mx, my)
        }
        lineTo(cord.x[n], cord.y[n])
    }
    drawPath(path, color = lerp(style.cordColor, Color.Black, 0.35f), style = Stroke(width = m.cordWidth * 1.35f, cap = StrokeCap.Round))
    drawPath(path, color = style.cordColor, style = Stroke(width = m.cordWidth, cap = StrokeCap.Round))
    // Short diagonal dashes make it look like twisted string.
    drawPath(
        path,
        color = lerp(style.cordColor, Color.Black, 0.25f).copy(alpha = 0.7f),
        style = Stroke(
            width = m.cordWidth * 0.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * m.px, 3.2f * m.px)),
        ),
    )

    // Bead and tassel hang along the last segment.
    val ex = cord.x[n]
    val ey = cord.y[n]
    val angle = atan2(ey - cord.y[n - 1], ex - cord.x[n - 1])
    val dx = kotlin.math.cos(angle)
    val dy = kotlin.math.sin(angle)
    val beadX = ex + dx * m.handleRadius * 0.6f
    val beadY = ey + dy * m.handleRadius * 0.6f
    drawLine(
        color = style.cordColor,
        start = Offset(beadX, beadY),
        end = Offset(beadX + dx * 13f * m.px, beadY + dy * 13f * m.px),
        strokeWidth = 3.4f * m.px,
        cap = StrokeCap.Round,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lightBrass, style.brassColor, darkBrass),
            center = Offset(beadX - m.handleRadius * 0.3f, beadY - m.handleRadius * 0.3f),
            radius = m.handleRadius * 1.4f,
        ),
        radius = m.handleRadius,
        center = Offset(beadX, beadY),
    )
}
