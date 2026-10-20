package com.nanomks.elamal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nanomks.elamal.util.BackupSafManager

@Composable
fun AppState.SettingsTab() {
    val context = LocalContext.current
    val backupFilesListState = remember { mutableStateOf(BackupSafManager.listAvailableBackups(context)) }
    val encEnabledState = remember { mutableStateOf(prefs.getBoolean("backup_encrypt_enabled", false)) }
    val backupPassState = remember { mutableStateOf(prefs.getString("backup_password", "") ?: "") }
    val pinSettingState = remember { mutableStateOf("") }
    val newSsidInputState = remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item { SettingsWifiSection(newSsidInputState) }
        item { SettingsCalcSection() }
        item { SettingsAppearanceSection() }
        item { SettingsSecuritySection(backupFilesListState, encEnabledState, backupPassState, pinSettingState) }
    }
}
