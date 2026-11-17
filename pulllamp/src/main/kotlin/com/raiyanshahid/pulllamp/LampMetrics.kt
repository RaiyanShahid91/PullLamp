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

/** Sizes of the lamp's parts, in pixels. [px] is the number of pixels in one dp times the style scale. */
internal class LampMetrics(val px: Float) {
    val width = 128f * px
    val height = 300f * px
    val pivotX = width / 2f

    val cableLength = 40f * px
    val shadeTop = cableLength
    val shadeHeight = 62f * px
    val shadeBottom = shadeTop + shadeHeight
    val shadeTopHalfWidth = 18f * px
    val shadeBottomHalfWidth = 50f * px
    val rimHeight = 10f * px

    val bulbX = pivotX
    val bulbY = shadeBottom + 2f * px
    val bulbRadius = 11f * px

    /** Where the cord leaves the lamp, relative to the pivot at the top of the cable. */
    val anchorOffsetX = 36f * px
    val anchorOffsetY = shadeBottom - 2f * px

    val cordLength = 118f * px
    val handleRadius = 7f * px
    val grabRadius = 26f * px
    val cordWidth = 2.2f * px
}
