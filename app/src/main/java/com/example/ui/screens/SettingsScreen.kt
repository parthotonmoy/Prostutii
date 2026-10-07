package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.Snapshot
import com.example.model.UserAvailability
import com.example.model.UserSettings
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.NotebookBackground
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.util.DateUtil
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsScreen(
    settings: UserSettings,
    availability: UserAvailability,
    snapshots: List<Snapshot>,
    onUpdateSettings: (UserSettings) -> Unit,
    onUpdateAvailability: (UserAvailability) -> Unit,
    onRestoreSnapshot: (String) -> Unit,
    onLoadSampleData: () -> Unit,
    onRemoveSampleData: () -> Unit,
    onResetAllData: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackupData: (json: String, mode: String) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    var resetInputText by remember { mutableStateOf("") }

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackup(uri)
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                reader.close()
                pendingImportJson = json
                showImportConfirmDialog = true
            } catch (e: Exception) {
                // Read error handled
            }
        }
    }

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.ink)
                }
                Text("Settings", style = ProstutiTypography.h2, color = colors.ink, modifier = Modifier.padding(start = 8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: APPEARANCE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("APPEARANCE", style = ProstutiTypography.caption, color = colors.ink3)

                        val themeOptions = listOf("System", "Light", "Dark")
                        val currentThemeIndex = when (settings.theme.lowercase()) {
                            "light" -> 1
                            "dark" -> 2
                            else -> 0
                        }
                        SegmentedControl(
                            options = themeOptions,
                            selectedIndex = currentThemeIndex,
                            onOptionSelected = { idx ->
                                val selected = when (idx) {
                                    1 -> "light"
                                    2 -> "dark"
                                    else -> "system"
                                }
                                onUpdateSettings(settings.copy(theme = selected))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: STUDY HOURS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("STUDY HOURS", style = ProstutiTypography.caption, color = colors.ink3)

                        // 7 stepper rows for Sat..Fri
                        val dayNames = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
                        dayNames.forEachIndexed { index, name ->
                            val currentMinutes = availability.weekdayMinutes.getOrElse(index) { 180 }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = ProstutiTypography.bodyMedium, color = colors.ink)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DraftingOutlinedButton(
                                        text = "-",
                                        onClick = {
                                            val newMinutes = (currentMinutes - 30).coerceAtLeast(0)
                                            val updated = availability.weekdayMinutes.toMutableList()
                                            updated[index] = newMinutes
                                            onUpdateAvailability(availability.copy(weekdayMinutes = updated))
                                        },
                                        modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                    )
                                    Text(
                                        text = DateUtil.formatDuration(currentMinutes),
                                        style = ProstutiTypography.monoSmall,
                                        color = colors.ink,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    DraftingOutlinedButton(
                                        text = "+",
                                        onClick = {
                                            val newMinutes = currentMinutes + 30
                                            val updated = availability.weekdayMinutes.toMutableList()
                                            updated[index] = newMinutes
                                            onUpdateAvailability(availability.copy(weekdayMinutes = updated))
                                        },
                                        modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: PLANNING
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PLANNING", style = ProstutiTypography.caption, color = colors.ink3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Revision buffer days (${settings.bufferDays})", style = ProstutiTypography.bodyMedium, color = colors.ink)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DraftingOutlinedButton(
                                    text = "-",
                                    onClick = {
                                        val newBuf = (settings.bufferDays - 1).coerceAtLeast(0)
                                        onUpdateSettings(settings.copy(bufferDays = newBuf))
                                    },
                                    modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                )
                                DraftingOutlinedButton(
                                    text = "+",
                                    onClick = {
                                        val newBuf = (settings.bufferDays + 1).coerceAtMost(5)
                                        onUpdateSettings(settings.copy(bufferDays = newBuf))
                                    },
                                    modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: POMODORO
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("POMODORO", style = ProstutiTypography.caption, color = colors.ink3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sound alert", style = ProstutiTypography.bodyMedium, color = colors.ink)
                            Switch(
                                checked = settings.soundOn,
                                onCheckedChange = { onUpdateSettings(settings.copy(soundOn = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.blue, checkedTrackColor = colors.blueSoft)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Vibrate", style = ProstutiTypography.bodyMedium, color = colors.ink)
                            Switch(
                                checked = settings.vibrateOn,
                                onCheckedChange = { onUpdateSettings(settings.copy(vibrateOn = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.blue, checkedTrackColor = colors.blueSoft)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: REMINDER
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("REMINDER", style = ProstutiTypography.caption, color = colors.ink3)
                        Text(
                            text = "Reminders work while the app is installed and allowed to notify. Some phones delay them.",
                            style = ProstutiTypography.caption,
                            color = colors.ink3
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: DATA
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("DATA", style = ProstutiTypography.caption, color = colors.ink3)
                        Text(
                            text = "Everything stays on this phone. There is no account and no server.",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DraftingOutlinedButton(
                                text = "Export backup",
                                onClick = {
                                    val dateStr = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
                                    createDocLauncher.launch("prostuti-backup-$dateStr.json")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DraftingOutlinedButton(
                                text = "Import backup",
                                onClick = { openDocLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DraftingOutlinedButton(
                                text = "Load sample data",
                                onClick = onLoadSampleData,
                                modifier = Modifier.weight(1f)
                            )
                            DraftingOutlinedButton(
                                text = "Remove sample data",
                                onClick = onRemoveSampleData,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Snapshots list
                        if (snapshots.isNotEmpty()) {
                            Text("Recent safety snapshots:", style = ProstutiTypography.caption, color = colors.ink3)
                            snapshots.take(5).forEach { snap ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(snap.reason, style = ProstutiTypography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                                    DraftingOutlinedButton(
                                        text = "Restore",
                                        onClick = { onRestoreSnapshot(snap.id) }
                                    )
                                }
                            }
                        }

                        DraftingOutlinedButton(
                            text = "About Prostuti",
                            onClick = onOpenAbout,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                    }
                }

                // Section: DANGER ZONE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DANGER ZONE", style = ProstutiTypography.caption, color = colors.vermilion)
                        DraftingOutlinedButton(
                            text = "Reset everything",
                            onClick = { showResetDialog = true },
                            borderColor = colors.vermilion,
                            textColor = colors.vermilion,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Reset Everything Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset everything", style = ProstutiTypography.h3, color = colors.vermilion) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "This will delete all subjects, topics, flashcards, sessions and logs. A safety snapshot is saved first.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink
                        )
                        Text(
                            text = "Type RESET to confirm:",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )
                        OutlinedTextField(
                            value = resetInputText,
                            onValueChange = { resetInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("RESET") }
                        )
                    }
                },
                confirmButton = {
                    DraftingPrimaryButton(
                        text = "Reset all data",
                        enabled = resetInputText == "RESET",
                        onClick = {
                            onResetAllData()
                            showResetDialog = false
                            resetInputText = ""
                        }
                    )
                },
                dismissButton = {
                    DraftingOutlinedButton(
                        text = "Cancel",
                        onClick = {
                            showResetDialog = false
                            resetInputText = ""
                        }
                    )
                },
                containerColor = colors.card
            )
        }

        // Import Backup Mode Dialog ("Replace" or "Merge")
        if (showImportConfirmDialog && pendingImportJson != null) {
            AlertDialog(
                onDismissRequest = {
                    showImportConfirmDialog = false
                    pendingImportJson = null
                },
                title = { Text("Import backup", style = ProstutiTypography.h3, color = colors.ink) },
                text = {
                    Text(
                        text = "Choose how to import this backup file. A safety snapshot will be created before applying changes.",
                        style = ProstutiTypography.bodyLarge,
                        color = colors.ink
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DraftingOutlinedButton(
                            text = "Merge",
                            onClick = {
                                pendingImportJson?.let { onImportBackupData(it, "merge") }
                                showImportConfirmDialog = false
                                pendingImportJson = null
                            }
                        )
                        DraftingPrimaryButton(
                            text = "Replace",
                            onClick = {
                                pendingImportJson?.let { onImportBackupData(it, "replace") }
                                showImportConfirmDialog = false
                                pendingImportJson = null
                            }
                        )
                    }
                },
                dismissButton = {
                    DraftingOutlinedButton(
                        text = "Cancel",
                        onClick = {
                            showImportConfirmDialog = false
                            pendingImportJson = null
                        }
                    )
                },
                containerColor = colors.card
            )
        }
    }
}
