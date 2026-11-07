package dev.danielclements.puck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.atvremote.protocol.discovery.AppleTvDevice

/**
 * Picks one already-paired Apple TV — shared by [DeviceWidgetConfigActivity]
 * (add-time widget binding) and [TileConfigActivity] (long-press tile binding).
 */
@Composable
fun DevicePicker(
    devices: List<AppleTvDevice>,
    titleRes: Int,
    emptyRes: Int,
    onPick: (AppleTvDevice) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            .padding(24.dp),
    ) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (devices.isEmpty()) {
            Text(
                stringResource(emptyRes),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(devices, key = { it.credentialKey }) { device ->
                ListItem(
                    headlineContent = { Text(device.name) },
                    supportingContent = { Text(device.model ?: "Apple TV") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(device) },
                )
            }
        }
    }
}
