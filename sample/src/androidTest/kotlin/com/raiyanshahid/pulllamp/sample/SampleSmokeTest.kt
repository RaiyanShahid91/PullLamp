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


package com.raiyanshahid.pulllamp.sample

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SampleSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun lampIs(on: Boolean): Boolean = try {
        val node = rule.onNodeWithContentDescription("Theme lamp")
        if (on) node.assertIsOn() else node.assertIsOff()
        true
    } catch (e: AssertionError) {
        false
    }

    @Test
    fun startsWithTheLightOn() {
        rule.onNodeWithContentDescription("Theme lamp").assertIsOn()
        rule.onNodeWithText("Good evening").assertExists()
    }

    @Test
    fun activatingTheLampSwitchesTheLightOffAndOnAgain() {
        rule.onNodeWithContentDescription("Theme lamp").performClick()
        rule.waitUntil(timeoutMillis = 8_000) { lampIs(on = false) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Theme lamp").performClick()
        rule.waitUntil(timeoutMillis = 8_000) { lampIs(on = true) }
    }

    @Test
    fun contentKeepsItsStateAcrossAThemeSwitch() {
        rule.onNodeWithText("Taps: 0").performClick()
        rule.onNodeWithText("Taps: 1").assertExists()
        rule.onNodeWithContentDescription("Theme lamp").performClick()
        rule.waitUntil(timeoutMillis = 8_000) { lampIs(on = false) }
        rule.onNodeWithText("Taps: 1").assertExists()
    }
}
