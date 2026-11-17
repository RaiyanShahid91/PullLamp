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
import org.junit.Assert.assertTrue
import org.junit.Test

class LampStyleTest {

    private fun rejects(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun defaultsAreValid() {
        val s = LampStyle()
        assertEquals(1f, s.scale, 0f)
        assertTrue(s.coneAlpha in 0f..1f)
        assertEquals("Theme lamp", s.contentDescription)
    }

    @Test
    fun rejectsBadScale() = rejects { LampStyle(scale = 0f) }

    @Test
    fun rejectsBadAlpha() = rejects { LampStyle(coneAlpha = 1.5f) }

    @Test
    fun metricsScaleWithDensity() {
        val a = LampMetrics(1f)
        val b = LampMetrics(2f)
        assertEquals(a.width * 2f, b.width, 1e-3f)
        assertEquals(a.cordLength * 2f, b.cordLength, 1e-3f)
    }
}
