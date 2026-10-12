package dev.danielclements.puck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

@Composable
fun SettingsScreen(state: UiState, vm: RemoteViewModel) {
    BackHandler { vm.closeSettings() }
    val scheme = MaterialTheme.colorScheme
    val c = Puck.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier.height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuckRoundButton(onClick = { vm.closeSettings() }, size = 44.dp, containerColor = c.raised, contentColor = scheme.onSurface) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settings_title), style = PuckText.screenTitle, color = scheme.onBackground)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.settings_section_control))
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(c.tray)
                .padding(6.dp),
        ) {
            SwitchRow(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_desc),
                checked = state.settings.hapticsEnabled,
                onCheckedChange = vm::setHaptics,
            )
            Spacer(Modifier.height(4.dp))
            SwitchRow(
                icon = Icons.Default.VolumeUp,
                title = stringResource(R.string.settings_button_sound),
                subtitle = stringResource(R.string.settings_button_sound_desc),
                checked = state.settings.buttonSoundEnabled,
                onCheckedChange = vm::setButtonSound,
            )
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.card)
                    .padding(14.dp, 14.dp, 14.dp, 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Default.Gamepad)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_remote_style), style = PuckText.rowTitle, color = scheme.onSurface)
                        Text(stringResource(R.string.settings_remote_style_desc), style = PuckText.caption, color = scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                PuckSegmentedPicker(
                    options = RemoteStyle.entries.toList(),
                    selected = state.settings.remoteStyle,
                    onSelect = vm::setRemoteStyle,
                    label = { style ->
                        when (style) {
                            RemoteStyle.TRACKPAD -> stringResource(R.string.settings_remote_style_trackpad)
                            RemoteStyle.DPAD -> stringResource(R.string.settings_remote_style_dpad)
                        }
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.card)
                    .padding(14.dp, 14.dp, 14.dp, 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Default.TouchApp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_sensitivity), style = PuckText.rowTitle, color = scheme.onSurface)
                        Text(stringResource(R.string.settings_sensitivity_desc), style = PuckText.caption, color = scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                PuckSegmentedPicker(
                    options = TouchSensitivity.entries.toList(),
                    selected = state.settings.sensitivity,
                    onSelect = vm::setSensitivity,
                    label = { level ->
                        when (level) {
                            TouchSensitivity.LOW -> stringResource(R.string.settings_sensitivity_low)
                            TouchSensitivity.MEDIUM -> stringResource(R.string.settings_sensitivity_medium)
                            TouchSensitivity.HIGH -> stringResource(R.string.settings_sensitivity_high)
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.settings_section_devices))
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(c.tray)
                .padding(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.card)
                    .clickable { vm.openManageDevices() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_manage_devices), style = PuckText.rowTitle, color = scheme.onSurface)
                    Text(
                        stringResource(R.string.settings_manage_devices_desc, state.managedDevices.size),
                        style = PuckText.caption,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.settings_footer),
            style = PuckText.caption,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, style = PuckText.label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Puck.colors.raised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Puck.colors.card)
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = PuckText.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = PuckText.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PuckSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
