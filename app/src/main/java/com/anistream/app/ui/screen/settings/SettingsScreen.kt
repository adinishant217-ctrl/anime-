package com.anistream.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anistream.app.domain.model.ThemeMode
import com.anistream.app.ui.component.ScreenHeader
import com.anistream.app.ui.theme.AmoledBackground
import com.anistream.app.ui.theme.DarkBackground
import com.anistream.app.ui.theme.MidnightBackground

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onPreferHighestQuality: (Boolean) -> Unit,
    onClearSearchHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SettingsSection(title = "Appearance") {
                ThemeMode.values().forEach { mode ->
                    ThemeOptionRow(
                        mode = mode,
                        selected = state.settings.themeMode == mode,
                        onSelect = { onThemeMode(mode) }
                    )
                }
            }

            SettingsSection(title = "Playback") {
                SwitchRow(
                    title = "Autoplay next episode",
                    subtitle = "Start the next episode when playback finishes.",
                    checked = state.settings.autoplayNext,
                    onCheckedChange = onAutoplayNext
                )
                SwitchRow(
                    title = "Prefer highest quality",
                    subtitle = "Choose the highest direct source quality when available.",
                    checked = state.settings.preferHighestQuality,
                    onCheckedChange = onPreferHighestQuality
                )
            }

            SettingsSection(title = "Search") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recent searches",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (state.settings.searchHistory.isEmpty()) {
                                "No recent searches"
                            } else {
                                "${state.settings.searchHistory.size} saved on this device"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onClearSearchHistory,
                        enabled = state.settings.searchHistory.isNotEmpty()
                    ) {
                        Text("Clear")
                    }
                }
            }

            SettingsSection(title = "About") {
                Text(
                    text = "AniStream 1.0.0 · Native Android",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Catalogue data comes from ShivraAPI (Otakudesu source), verified " +
                        "against the anichi-cli implementation. AniStream does not host or " +
                        "re-encode video. Only direct, compatible sources are offered to the " +
                        "native player; third-party embed players are never bypassed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val swatch = when (mode) {
        ThemeMode.MIDNIGHT -> MidnightBackground
        ThemeMode.DARK -> DarkBackground
        ThemeMode.AMOLED -> AmoledBackground
    }
    val title = when (mode) {
        ThemeMode.MIDNIGHT -> "Midnight"
        ThemeMode.DARK -> "Dark"
        ThemeMode.AMOLED -> "AMOLED black"
    }
    val subtitle = when (mode) {
        ThemeMode.MIDNIGHT -> "Deep navy with cyan accents"
        ThemeMode.DARK -> "Charcoal surfaces"
        ThemeMode.AMOLED -> "Pure black for OLED screens"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(swatch)
                .border(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
