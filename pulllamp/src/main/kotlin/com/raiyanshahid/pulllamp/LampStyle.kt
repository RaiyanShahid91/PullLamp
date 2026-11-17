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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * How the lamp looks and what it says to a screen reader.
 *
 * Every value has a default, so `LampStyle()` is a good starting point. Use named arguments to change
 * only what you want.
 */
@Immutable
public class LampStyle(
    /** Main colour of the lampshade. */
    public val shadeColor: Color = Color(0xFF2E7D6B),
    /** Metal parts: the trim on the shade and the bead on the end of the cord. */
    public val brassColor: Color = Color(0xFFC9A24B),
    /** The cable the lamp hangs from. */
    public val cableColor: Color = Color(0xFF3B3B42),
    /** The pull cord. */
    public val cordColor: Color = Color(0xFFD9C8A4),
    /** The colour of the light. */
    public val lightColor: Color = Color(0xFFFFD58A),
    /** How strong the cone of light below the lamp is, from 0 to 1. */
    public val coneAlpha: Float = 0.32f,
    /** How strong the glow around the bulb is, from 0 to 1. */
    public val glowAlpha: Float = 0.22f,
    /**
     * How the light is laid over your content. [BlendMode.SrcOver] tints the screen warm and shows up on
     * any background. [BlendMode.Screen] behaves like real light on a dark design but is almost
     * invisible on a bright one.
     */
    public val lightBlendMode: BlendMode = BlendMode.SrcOver,
    /** Scales the whole lamp. 1 is about 130 dp wide and 300 dp tall. */
    public val scale: Float = 1f,
    /** Read out by screen readers as the lamp's name. */
    public val contentDescription: String = "Theme lamp",
    /** Read out by screen readers while the light is on. */
    public val onDescription: String = "Light on",
    /** Read out by screen readers while the light is off. */
    public val offDescription: String = "Light off",
) {
    init {
        require(scale > 0f && scale.isFinite()) { "scale must be positive, was $scale" }
        require(coneAlpha in 0f..1f) { "coneAlpha must be in 0..1, was $coneAlpha" }
        require(glowAlpha in 0f..1f) { "glowAlpha must be in 0..1, was $glowAlpha" }
    }
}
