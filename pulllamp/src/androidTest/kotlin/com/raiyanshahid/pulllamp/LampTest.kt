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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LampTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toggleFlipsTheLightWithoutAScene() {
        lateinit var lamp: LampState
        rule.setContent {
            lamp = rememberLampState(initialOn = true)
            LaunchedEffect(Unit) { lamp.toggle() }
            PullLamp(lamp)
        }
        rule.waitUntil(timeoutMillis = 5_000) { !lamp.isOn && !lamp.isTransitioning }
        assertTrue(lamp.isDark)
    }

    @Test
    fun setOnDoesNothingWhenAlreadyThere() {
        lateinit var lamp: LampState
        rule.setContent {
            lamp = rememberLampState(initialOn = false)
            LaunchedEffect(Unit) { lamp.setOn(false) }
            PullLamp(lamp)
        }
        rule.waitForIdle()
        assertFalse(lamp.isOn)
        assertFalse(lamp.isTransitioning)
    }

    @Test
    fun sceneSwitchEndsCleanly() {
        lateinit var lamp: LampState
        rule.setContent {
            lamp = rememberLampState(initialOn = true, revealMillis = 300)
            LaunchedEffect(Unit) { lamp.toggle() }
            LampScene(lamp, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize()) { Text("content") } }
        }
        rule.waitUntil(timeoutMillis = 5_000) { !lamp.isOn && !lamp.isTransitioning }
        assertEquals(false, lamp.isOn)
    }

    @Test
    fun lampIsASwitchForAccessibility() {
        rule.setContent {
            val lamp = rememberLampState(initialOn = true)
            PullLamp(lamp, style = LampStyle(contentDescription = "Lamp"))
        }
        rule.onNodeWithContentDescription("Lamp").assertIsOn()
        rule.onNodeWithContentDescription("Lamp").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            try {
                rule.onNodeWithContentDescription("Lamp").assertIsOff()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    @Test
    fun negativeRevealIsRejected() {
        var rejected = false
        rule.setContent {
            try {
                rememberLampState(revealMillis = -1)
            } catch (e: IllegalArgumentException) {
                rejected = true
            }
        }
        rule.waitForIdle()
        assertTrue(rejected)
    }
}
