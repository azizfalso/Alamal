package com.nanomks.elamal.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.SettingsSecuritySection(
    backupFilesListState: MutableState<List<String>>,
    encEnabledState: MutableState<Boolean>,
    backupPassState: MutableState<String>,
    pinSettingState: MutableState<String>
) {
    val context = LocalContext.current
    var backupFilesList by backupFilesListState
    var encEnabled by encEnabledState
    var backupPass by backupPassState
    var pinSetting by pinSettingState
    val lastBackupMillis = prefs.getLong("last_backup_time", 0L)
    val backupAgeHours = if (lastBackupMillis > 0L) (System.currentTimeMillis() - lastBackupMillis) / 3600_000L else -1L

    // Section 4: Security (SHA-256 PIN) & 5 Rotating Backups
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = primaryCustom)
                Text("الأمان والنسخ الاحتياطي الذكي (5 نسخ دوارة)", fontWeight = FontWeight.Bold, fontSize = 15.5f.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pinSetting,
                    onValueChange = {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                            pinSetting = it
                        }
                    },
                    label = { Text(if (savedPin.isBlank()) "تعيين رمز قفل (4-8 أرقام)" else "تغيير رمز القفل المحمي ✓") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (pinSetting.isEmpty()) {
                            savedPin = ""
                            prefs.edit().putString("security_pin", "").apply()
                            Toast.makeText(context, "تم إلغاء رمز القفل ✓", Toast.LENGTH_SHORT).show()
                        } else if (pinSetting.length >= 4) {
                            val hashed = ElamalUtils.hashPin(pinSetting)
                            savedPin = hashed
                            prefs.edit().putString("security_pin", hashed).apply()
                            pinSetting = ""
                            Toast.makeText(context, "تم تشفير وحفظ رمز القفل بنجاح ✓", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "أدخل 4 أرقام على الأقل أو اتركه فارغاً للإلغاء", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (pinSetting.isEmpty() && savedPin.isNotBlank()) "إلغاء القفل" else "حفظ")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تشفير النسخ الاحتياطية بكلمة مرور (AES-256)")
                Switch(
                    checked = encEnabled,
                    onCheckedChange = {
                        encEnabled = it
                        prefs.edit().putBoolean("backup_encrypt_enabled", it).apply()
                    }
                )
            }

            if (encEnabled) {
                OutlinedTextField(
                    value = backupPass,
                    onValueChange = {
                        backupPass = it
                        prefs.edit().putString("backup_password", it).apply()
                    },
                    label = { Text("كلمة مرور تشفير واستعادة النسخ الاحتياطية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text(
                text = if (backupAgeHours >= 0) "⏱ عمر آخر نسخة احتياطية: منذ $backupAgeHours ساعة"
                else "⚠️ تذكير: لم يتم أخذ نسخة احتياطية بعد!",
                fontSize = 13.sp,
                color = if (backupAgeHours >= 0) Color(0xFF059669) else Color(0xFFD97706)
            )

            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (backupUriStr.isBlank()) "اختيار مجلد النسخ الاحتياطي (SAF)" else "تغيير مجلد النسخ الاحتياطي ✓")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        viewModelScope.launch {
                            val msg = BackupSafManager.performSmartBackup(context, dao, isManual = true)
                            statusBanner = msg
                            backupFilesList = BackupSafManager.listAvailableBackups(context)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("نسخ يدوي الآن") }

                OutlinedButton(
                    onClick = {
                        viewModelScope.launch {
                            val msg = BackupSafManager.restoreFromBackup(context, dao)
                            refreshPrefsState()
                            statusBanner = msg
                            backupFilesList = BackupSafManager.listAvailableBackups(context)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("استعادة التلقائية") }
            }

            if (backupFilesList.isNotEmpty()) {
                Text("النسخ المتوفرة في المجلد (${backupFilesList.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                for (bf in backupFilesList.take(8)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bf, fontSize = 12.sp)
                        TextButton(onClick = {
                            viewModelScope.launch {
                                val msg = BackupSafManager.restoreFromBackup(context, dao, bf)
                                refreshPrefsState()
                                statusBanner = msg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("استعادة") }
                    }
                }
            }
        }
    }
}
