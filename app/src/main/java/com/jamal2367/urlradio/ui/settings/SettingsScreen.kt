/*
 * SettingsScreen.kt
 * The settings screen. Replaces SettingsFragment and androidx.preference.
 *
 * Every entry writes to the same SharedPreferences key it used before, so an update keeps
 * the user's existing settings.
 *
 * This file is part of URL Radio
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package com.jamal2367.urlradio.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jamal2367.urlradio.Keys
import com.jamal2367.urlradio.R
import com.jamal2367.urlradio.helpers.ListenBrainzHelper
import com.jamal2367.urlradio.ui.dialogs.ConfirmDialog
import com.jamal2367.urlradio.ui.dialogs.WideDialogModifier
import com.jamal2367.urlradio.ui.dialogs.WideDialogProperties

data class SettingsCallbacks(
    val onThemeSelected: (String) -> Unit,
    val onUpdateStationImages: () -> Unit,
    val onRemoveAllStations: () -> Unit,
    val onImportPlaylist: () -> Unit,
    val onExportM3u: () -> Unit,
    val onExportPls: () -> Unit,
    val onBackup: () -> Unit,
    val onRestore: () -> Unit,
    val onLargeBufferChanged: (Boolean) -> Unit,
    val onEditStationsChanged: (Boolean) -> Unit,
    val onEditStreamUrisChanged: (Boolean) -> Unit,
    val onListenBrainzTokenChanged: (String) -> Unit,
    val onCoverArtChanged: (Boolean) -> Unit,
    val onScrobbleFormatChanged: (String) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onCopyVersion: (String) -> Unit,
)

@Composable
fun SettingsScreen(
    versionSummary: String,
    themeSelection: String,
    themeLabel: String,
    largeBuffer: Boolean,
    listenBrainzToken: String,
    scrobbleFormat: String,
    coverArtEnabled: Boolean,
    liveTitle: String,
    isPlaying: Boolean,
    editStations: Boolean,
    editStreamUris: Boolean,
    callbacks: SettingsCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var showRemoveAllDialog by remember { mutableStateOf(false) }
    var showListenBrainzDialog by remember { mutableStateOf(false) }
    var showScrobbleFormatDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.pref_app_version_title),
                    summary = versionSummary,
                    icon = R.drawable.ic_info_24dp,
                    onClick = { callbacks.onCopyVersion(versionSummary) },
                )
                SettingsRow(
                    title = stringResource(R.string.pref_license_title),
                    summary = stringResource(R.string.pref_license_summary),
                    icon = R.drawable.ic_library_24dp,
                    onClick = { callbacks.onOpenUrl(LICENSE_URL) },
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_general_title)) }
        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.pref_theme_selection_title),
                    summary = "${stringResource(R.string.pref_theme_selection_summary)} $themeLabel",
                    icon = R.drawable.ic_brush_24dp,
                    onClick = { showThemeDialog = true },
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_scrobbling_title)) }
        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.pref_listenbrainz_title),
                    summary = stringResource(
                        if (listenBrainzToken.isEmpty()) R.string.pref_listenbrainz_summary_disabled
                        else R.string.pref_listenbrainz_summary_enabled
                    ),
                    icon = R.drawable.ic_music_note_24dp,
                    onClick = { showListenBrainzDialog = true },
                )
                SettingsRow(
                    title = stringResource(R.string.pref_scrobble_format_title),
                    summary = scrobbleFormat.ifEmpty { ListenBrainzHelper.DEFAULT_FORMAT },
                    icon = R.drawable.ic_edit_24dp,
                    onClick = { showScrobbleFormatDialog = true },
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_cover_art_category)) }
        item {
            SettingsGroup {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_cover_art_title),
                    summary = stringResource(
                        if (coverArtEnabled) R.string.pref_cover_art_summary_enabled
                        else R.string.pref_cover_art_summary_disabled
                    ),
                    icon = R.drawable.ic_image_24dp,
                    checked = coverArtEnabled,
                    onCheckedChange = callbacks.onCoverArtChanged,
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_maintenance_title)) }
        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.pref_update_station_images_title),
                    summary = stringResource(R.string.pref_update_station_images_summary),
                    icon = R.drawable.ic_image_24dp,
                    onClick = callbacks.onUpdateStationImages,
                )
                SettingsRow(
                    title = stringResource(R.string.pref_remove_all_stations_title),
                    summary = stringResource(R.string.pref_remove_all_stations_summary),
                    icon = R.drawable.ic_remove_circle_24dp,
                    onClick = { showRemoveAllDialog = true },
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_backup_import_export_title)) }
        item {
            SettingsGroup {
                // One entry for both playlist formats. Which of the two is written is asked
                // in a dialog rather than through two near-identical rows.
                SettingsRow(
                    title = stringResource(R.string.pref_playlist_export_title),
                    summary = stringResource(R.string.pref_playlist_export_summary),
                    icon = R.drawable.ic_playlist_export_24dp,
                    onClick = { showExportFormatDialog = true },
                )
                SettingsRow(
                    title = stringResource(R.string.pref_playlist_import_title),
                    summary = stringResource(R.string.pref_playlist_import_summary),
                    icon = R.drawable.ic_playlist_add_24dp,
                    onClick = callbacks.onImportPlaylist,
                )
                SettingsRow(
                    title = stringResource(R.string.pref_station_export_title),
                    summary = stringResource(R.string.pref_station_export_summary),
                    icon = R.drawable.ic_download_24dp,
                    onClick = callbacks.onBackup,
                )
                SettingsRow(
                    title = stringResource(R.string.pref_station_restore_title),
                    summary = stringResource(R.string.pref_station_restore_summary),
                    icon = R.drawable.ic_upload_24dp,
                    onClick = callbacks.onRestore,
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_advanced_title)) }
        item {
            SettingsGroup {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_buffer_size_title),
                    summary = stringResource(
                        if (largeBuffer) R.string.pref_buffer_size_summary_enabled
                        else R.string.pref_buffer_size_summary_disabled
                    ),
                    icon = R.drawable.ic_network_check_24dp,
                    checked = largeBuffer,
                    onCheckedChange = callbacks.onLargeBufferChanged,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_edit_station_title),
                    summary = stringResource(
                        if (editStations) R.string.pref_edit_station_summary_enabled
                        else R.string.pref_edit_station_summary_disabled
                    ),
                    icon = R.drawable.ic_edit_24dp,
                    checked = editStations,
                    onCheckedChange = callbacks.onEditStationsChanged,
                )
                // Editing stream addresses only makes sense while editing is on at all --
                // the old screen disabled and unchecked this entry in the same way.
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_edit_station_stream_title),
                    summary = stringResource(
                        if (editStreamUris) R.string.pref_edit_station_stream_summary_enabled
                        else R.string.pref_edit_station_stream_summary_disabled
                    ),
                    icon = R.drawable.ic_music_note_24dp,
                    checked = editStreamUris,
                    enabled = editStations,
                    onCheckedChange = callbacks.onEditStreamUrisChanged,
                )
            }
        }

        item { CategoryHeader(stringResource(R.string.pref_links_title)) }
        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.pref_github_title),
                    summary = stringResource(R.string.pref_github_summary),
                    icon = R.drawable.ic_github_24dp,
                    onClick = { callbacks.onOpenUrl(GITHUB_URL) },
                )
                SettingsRow(
                    title = stringResource(R.string.pref_codeberg_title),
                    summary = stringResource(R.string.pref_codeberg_summary),
                    icon = R.drawable.ic_codeberg_24dp,
                    onClick = { callbacks.onOpenUrl(CODEBERG_URL) },
                )
            }
        }
    }

    if (showThemeDialog) {
        // The dialog stays open on purpose - see ThemeChooserDialog.
        ThemeChooserDialog(
            current = themeSelection,
            onSelect = callbacks.onThemeSelected,
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showListenBrainzDialog) {
        ListenBrainzTokenDialog(
            current = listenBrainzToken,
            onSave = {
                callbacks.onListenBrainzTokenChanged(it)
                showListenBrainzDialog = false
            },
            onDismiss = { showListenBrainzDialog = false },
        )
    }

    if (showScrobbleFormatDialog) {
        ScrobbleFormatDialog(
            current = scrobbleFormat,
            liveTitle = if (isPlaying) liveTitle else "",
            onSave = {
                callbacks.onScrobbleFormatChanged(it)
                showScrobbleFormatDialog = false
            },
            onDismiss = { showScrobbleFormatDialog = false },
        )
    }

    if (showExportFormatDialog) {
        ExportFormatDialog(
            onExportM3u = {
                showExportFormatDialog = false
                callbacks.onExportM3u()
            },
            onExportPls = {
                showExportFormatDialog = false
                callbacks.onExportPls()
            },
            onDismiss = { showExportFormatDialog = false },
        )
    }

    if (showRemoveAllDialog) {
        // Wiping the collection is not undoable and there is no per-station swipe to fall
        // back on here, so it goes through the same confirmation the single delete uses.
        ConfirmDialog(
            title = stringResource(R.string.pref_remove_all_stations_title),
            message = stringResource(R.string.dialog_yes_no_message_remove_all_stations),
            confirmLabel = stringResource(R.string.dialog_yes_no_positive_button_remove_all_stations),
            onConfirm = callbacks.onRemoveAllStations,
            onDismiss = { showRemoveAllDialog = false },
        )
    }
}

/* Edits the ListenBrainz user token. Saving an empty value turns scrobbling off. */
@Composable
private fun ListenBrainzTokenDialog(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var token by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_listenbrainz_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pref_listenbrainz_dialog_message))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.pref_listenbrainz_token_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(token) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/* Edits the global stream title format, with a preview against the title playing right now. */
@Composable
private fun ScrobbleFormatDialog(
    current: String,
    liveTitle: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var format by remember { mutableStateOf(current) }
    val valid = format.isBlank() || ListenBrainzHelper.isValidFormat(format)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_scrobble_format_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pref_scrobble_format_dialog_message))
                OutlinedTextField(
                    value = format,
                    onValueChange = { format = it },
                    label = { Text(stringResource(R.string.dialog_edit_scrobble_format)) },
                    placeholder = { Text(ListenBrainzHelper.DEFAULT_FORMAT) },
                    isError = !valid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                ScrobblePreview(
                    liveTitle = liveTitle,
                    format = ListenBrainzHelper.effectiveFormat(format, ""),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(format) }, enabled = valid) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/*
 * Shows what would be scrobbled for the title the stream is sending right now. Empty
 * liveTitle means nothing is playing, so the user is told to start the station.
 */
@Composable
fun ScrobblePreview(liveTitle: String, format: String) {
    val track = if (liveTitle.isEmpty()) null else ListenBrainzHelper.parseStreamTitle(liveTitle, format)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        if (liveTitle.isEmpty()) {
            Text(
                text = stringResource(R.string.scrobble_preview_not_playing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.scrobble_preview_stream_title, liveTitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (track != null) {
                    stringResource(R.string.scrobble_preview_result, track.artist, track.title)
                } else {
                    stringResource(R.string.scrobble_preview_no_match)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (track != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

/* Asks which playlist format to write, standing in for the two separate export entries. */
@Composable
private fun ExportFormatDialog(
    onExportM3u: () -> Unit,
    onExportPls: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = WideDialogModifier,
        properties = WideDialogProperties,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_playlist_export_24dp),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.pref_playlist_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormatRow(
                    title = stringResource(R.string.pref_m3u_export_title),
                    summary = stringResource(R.string.pref_m3u_export_summary),
                    icon = R.drawable.ic_save_m3u_24dp,
                    onClick = onExportM3u,
                )
                ExportFormatRow(
                    title = stringResource(R.string.pref_pls_export_title),
                    summary = stringResource(R.string.pref_pls_export_summary),
                    icon = R.drawable.ic_save_pls_24dp,
                    onClick = onExportPls,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_generic_button_cancel))
            }
        },
    )
}

@Composable
private fun ExportFormatRow(title: String, summary: String, icon: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* Groups related entries into one rounded card, replacing the flat divider-separated list. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    summary: String,
    icon: Int,
    onClick: () -> Unit,
) {
    SettingsRowLayout(
        title = title,
        summary = summary,
        icon = icon,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingsRowLayout(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
    )
}

@Composable
private fun SettingsRowLayout(
    title: String,
    summary: String,
    icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = contentAlpha),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = contentAlpha),
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        }
        trailing?.invoke()
    }
}

private data class ThemeOption(
    val value: String,
    val label: String,
    val summary: String,
    val icon: Int,
)

/*
 * Picking a theme applies it straight away and leaves the dialog open, so the three options
 * can be compared against the live app before settling on one. The old dialog closed on the
 * first tap and only offered a "cancel" button that did not undo anything.
 */
@Composable
private fun ThemeChooserDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        ThemeOption(
            value = Keys.STATE_THEME_FOLLOW_SYSTEM,
            label = stringResource(R.string.pref_theme_selection_mode_device_default),
            summary = stringResource(R.string.pref_theme_selection_mode_device_default_summary),
            icon = R.drawable.ic_smartphone_24dp,
        ),
        ThemeOption(
            value = Keys.STATE_THEME_LIGHT_MODE,
            label = stringResource(R.string.pref_theme_selection_mode_light),
            summary = stringResource(R.string.pref_theme_selection_mode_light_summary),
            icon = R.drawable.ic_light_mode_24dp,
        ),
        ThemeOption(
            value = Keys.STATE_THEME_DARK_MODE,
            label = stringResource(R.string.pref_theme_selection_mode_dark),
            summary = stringResource(R.string.pref_theme_selection_mode_dark_summary),
            icon = R.drawable.ic_dark_mode_24dp,
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = WideDialogModifier,
        properties = WideDialogProperties,
        icon = {
            Icon(painter = painterResource(R.drawable.ic_brush_24dp), contentDescription = null)
        },
        title = { Text(stringResource(R.string.pref_theme_selection_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    ThemeOptionRow(
                        option = option,
                        selected = option.value == current,
                        onClick = { onSelect(option.value) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_generic_button_done))
            }
        },
    )
}

@Composable
private fun ThemeOptionRow(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent,
        label = "themeOptionBackground",
    )
    val iconBackground = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val iconTint = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = option.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

private const val GITHUB_URL = "https://github.com/jamal2362/URL-Radio"
private const val CODEBERG_URL = "https://codeberg.org/y20k/transistor"
private const val LICENSE_URL = "https://github.com/jamal2362/URL-Radio/blob/master/LICENSE.md"
