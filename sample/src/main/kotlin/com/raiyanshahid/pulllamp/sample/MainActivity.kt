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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.raiyanshahid.pulllamp.LampScene
import com.raiyanshahid.pulllamp.rememberLampState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

private val LightColors = lightColorScheme(
    background = Color(0xFFFFFBF3),
    surface = Color(0xFFFFFBF3),
    surfaceVariant = Color(0xFFF1E9D8),
    onSurface = Color(0xFF2A2620),
    primary = Color(0xFF2E7D6B),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF0E1013),
    surface = Color(0xFF0E1013),
    surfaceVariant = Color(0xFF1B1F26),
    onSurface = Color(0xFFE7E3DA),
    primary = Color(0xFF7FD1BC),
)

@Composable
private fun SampleApp() {
    val lamp = rememberLampState(initialOn = true)

    // The lamp scene lays the light over this content and keeps the pull cord on top.
    LampScene(state = lamp, modifier = Modifier.fillMaxSize()) {
        // Your theme follows the lamp. It is composed once, so state like the counter below survives.
        MaterialTheme(colorScheme = if (lamp.isOn) LightColors else DarkColors) {
            Surface(Modifier.fillMaxSize()) { Home() }
        }
    }
}

@Composable
private fun Home() {
    var taps by remember { mutableIntStateOf(0) }
    var notifications by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Keep the corner under the lamp free of buttons: the lamp takes the touches there.
        Column(Modifier.fillMaxWidth().height(250.dp).padding(start = 20.dp, end = 150.dp, top = 24.dp)) {
            Text("Good evening", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("Pull the cord to change the light.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = notifications, onCheckedChange = { notifications = it })
                Spacer(Modifier.padding(6.dp))
                Text("Notifications")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { taps++ }) { Text("Taps: $taps") }
        }

        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2E7D6B), Color(0xFFE0A24B)))),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text("Your day", Modifier.padding(16.dp), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(listOf("Morning run", "Design review", "Lunch with Sam", "Read 20 pages", "Plan the week")) { title ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text("Tap to open", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
