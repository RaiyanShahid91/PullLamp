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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max

/**
 * Puts a [PullLamp] over [content] and makes the light real: a warm cone below the lamp, a glow around
 * the bulb, and, when the cord is pulled, the new look spreading across the screen from the bulb.
 *
 * Choose your theme from [LampState.isOn] (or [LampState.isDark]) inside [content]. The scene shows
 * the old look outside a growing circle while your theme switches underneath, so [content] is only
 * composed once and keeps all of its state.
 *
 * The lamp sits at the top of the scene (see [lampAlignment]) and takes touches; everything else in
 * [content] receives touches as usual.
 */
@Composable
public fun LampScene(
    state: LampState,
    modifier: Modifier = Modifier,
    lampAlignment: Alignment = Alignment.TopEnd,
    lampPadding: PaddingValues = PaddingValues(end = 24.dp),
    style: LampStyle = LampStyle(),
    haptics: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    DisposableEffect(state, layer) {
        state.layer = layer
        onDispose { if (state.layer === layer) state.layer = null }
    }
    val density = LocalDensity.current.density
    val metrics = remember(density, style.scale) { LampMetrics(density * style.scale) }

    Box(
        modifier.onGloballyPositioned { state.sceneInWindow = it.positionInWindow() },
    ) {
        // The content is drawn through a layer so a picture of the old look is ready at the switch.
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            content = content,
        )
        // The light lies on top of the content and under the lamp, and never takes touches.
        Box(
            Modifier
                .matchParentSize()
                .drawBehind { drawLight(state, metrics, style) },
        )
        PullLamp(
            state = state,
            modifier = Modifier
                .align(lampAlignment)
                .padding(lampPadding),
            style = style,
            haptics = haptics,
        )
    }
}

private fun DrawScope.drawLight(state: LampState, m: LampMetrics, style: LampStyle) {
    val origin = state.origin()
    if (!origin.isSpecified) return

    drawSpreadingLook(state, origin, style)

    val lit = state.light.value
    if (lit <= 0.01f) return

    // Cone of light under the shade.
    val topY = origin.y
    val bottomY = size.height
    if (bottomY > topY) {
        val spread = m.shadeBottomHalfWidth + (bottomY - topY) * CONE_SLOPE
        val cone = Path().apply {
            moveTo(origin.x - m.shadeBottomHalfWidth, topY)
            lineTo(origin.x + m.shadeBottomHalfWidth, topY)
            lineTo(origin.x + spread, bottomY)
            lineTo(origin.x - spread, bottomY)
            close()
        }
        drawPath(
            path = cone,
            brush = Brush.verticalGradient(
                colors = listOf(
                    style.lightColor.copy(alpha = style.coneAlpha * lit),
                    style.lightColor.copy(alpha = style.coneAlpha * 0.35f * lit),
                    Color.Transparent,
                ),
                startY = topY,
                endY = bottomY,
            ),
            blendMode = style.lightBlendMode,
        )
    }

    // Glow around the bulb.
    val glowRadius = max(size.width, size.height) * 0.9f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(style.lightColor.copy(alpha = style.glowAlpha * lit), Color.Transparent),
            center = origin,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = origin,
        blendMode = style.lightBlendMode,
    )
}

/** Shows the old look outside a circle growing from the bulb, with a soft warm edge. */
private fun DrawScope.drawSpreadingLook(state: LampState, origin: Offset, style: LampStyle) {
    val shot = state.snapshot ?: return
    val progress = state.reveal.value
    if (progress >= 1f) return
    val farthest = max(
        max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
        max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
    )
    val radius = farthest * progress
    val circle = Path().apply { addOval(Rect(center = origin, radius = radius)) }
    clipPath(circle, clipOp = ClipOp.Difference) {
        drawImage(shot)
    }
    if (radius > 1f) {
        val soft = 36.dp.toPx()
        val outer = radius + soft
        val inner = ((radius - soft) / outer).coerceIn(0f, 0.98f)
        val mid = (radius / outer).coerceIn(inner + 0.01f, 0.99f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    inner to Color.Transparent,
                    mid to style.lightColor.copy(alpha = 0.28f * (1f - progress)),
                    1f to Color.Transparent,
                ),
                center = origin,
                radius = outer,
            ),
            radius = outer,
            center = origin,
        )
    }
}

private const val CONE_SLOPE = 0.8f
