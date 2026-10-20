package com.nanomks.elamal.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nanomks.elamal.data.*
import com.nanomks.elamal.data.ElamalDao
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.util.PdfReportGenerator
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.SettingsTab() {
                        // TAB 3: SETTINGS (Includes Root Wi-Fi 6 Networks Config, Appearance, Security & Smart SAF Backups)
                        val lastBackupMillis = prefs.getLong("last_backup_time", 0L)
                        val backupAgeHours = if (lastBackupMillis > 0L) (System.currentTimeMillis() - lastBackupMillis) / 3600_000L else -1L
                        var backupFilesList by remember { mutableStateOf(BackupSafManager.listAvailableBackups(context)) }
                        var encEnabled by remember { mutableStateOf(prefs.getBoolean("backup_encrypt_enabled", false)) }
                        var backupPass by remember { mutableStateOf(prefs.getString("backup_password", "") ?: "") }
                        var pinSetting by remember { mutableStateOf(savedPin) }
                        var newSsidInput by remember { mutableStateOf("") }

                        val ssidList = workSsids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Section 1: Root Wi-Fi Tracking inside Settings!
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Wifi, contentDescription = null, tint = primaryCustom)
                                            Text("تتبع شبكات العمل بالرووت (بدون GPS)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                        Text(
                                            text = "الشبكة المقروءة بالرووت الآن: ${detectedRootSsid ?: "اضغط فحص لطلب صلاحية الرووت (su)"}",
                                            fontSize = 13.sp,
                                            color = if (detectedRootSsid != null) Color(0xFF059669) else colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        val ssid = withContext(Dispatchers.IO) {
                                                            ElamalUtils.pollAndMergeRootWifi(context, dao)
                                                        }
                                                        detectedRootSsid = ssid
                                                        if (ssid != null) {
                                                            Toast.makeText(context, "تم قراءة الشبكة عبر الرووت: $ssid ✓", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "تأكد من منح صلاحية الرووت للتطبيق في Magisk/KernelSU", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("فحص بالرووت الآن")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val current = detectedRootSsid
                                                    if (current != null && ssidList.size < 6 && !ssidList.contains(current)) {
                                                        val updated = (ssidList + current).joinToString(",")
                                                        workSsids = updated
                                                        prefs.edit().putString("work_ssids", updated).apply()
                                                        scope.launch(Dispatchers.IO) { ElamalUtils.pollAndMergeRootWifi(context, dao) }
                                                    } else {
                                                        Toast.makeText(context, "افحص الشبكة بالرووت أولاً أو أضف اسمها بالأسفل", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("إضافة الشبكة الحالية")
                                            }
                                        }

                                        Text("قائمة شبكات العمل (${ssidList.size} / 6 شبكات لدعم الترددين 2.4G و 5G):", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ssidList.forEach { itemSsid ->
                                                InputChip(
                                                    selected = true,
                                                    onClick = {
                                                        val updated = ssidList.filter { it != itemSsid }.joinToString(",")
                                                        workSsids = updated
                                                        prefs.edit().putString("work_ssids", updated).apply()
                                                    },
                                                    label = { Text(itemSsid) },
                                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(15.dp)) }
                                                )
                                            }
                                        }

                                        if (ssidList.size < 6) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = newSsidInput,
                                                    onValueChange = { newSsidInput = it },
                                                    label = { Text("أضف اسم شبكة عمل يدوياً (SSID)") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        val clean = newSsidInput.trim()
                                                        if (clean.isNotEmpty() && ssidList.size < 6 && !ssidList.contains(clean)) {
                                                            val updated = (ssidList + clean).joinToString(",")
                                                            workSsids = updated
                                                            prefs.edit().putString("work_ssids", updated).apply()
                                                            newSsidInput = ""
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) { Text("إضافة") }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Calculation & Widget Settings
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, tint = primaryCustom)
                                            Text("الحساب، التنبيهات، والودجت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        var rateInput by remember { mutableStateOf(hourlyRate.toString()) }
                                        OutlinedTextField(
                                            value = rateInput,
                                            onValueChange = {
                                                rateInput = it
                                                it.toDoubleOrNull()?.let { r ->
                                                    hourlyRate = r
                                                    prefs.edit().putFloat("hourly_rate", r.toFloat()).apply()
                                                }
                                            },
                                            label = { Text("سعر الساعة الافتراضي (DH) - لا يغير الأرشيف") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Text("تنبيه نسيان الخروج: بعد $forgotAlarmHours ساعة (من 0 إلى 48)", fontWeight = FontWeight.Medium)
                                        Slider(
                                            value = forgotAlarmHours.toFloat(),
                                            onValueChange = {
                                                forgotAlarmHours = it.toInt()
                                                prefs.edit().putInt("forgot_alarm_hours", forgotAlarmHours).apply()
                                            },
                                            valueRange = 0f..48f
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("إظهار البحث والفلتر في الدفتر")
                                            Switch(
                                                checked = searchFilterEnabled,
                                                onCheckedChange = {
                                                    searchFilterEnabled = it
                                                    prefs.edit().putBoolean("search_filter_enabled", it).apply()
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تفعيل الودجت (الدخول الجاري لا يضيع)")
                                            Switch(
                                                checked = widgetEnabled,
                                                onCheckedChange = {
                                                    widgetEnabled = it
                                                    prefs.edit().putBoolean("widget_enabled", it).apply()
                                                    ElamalWidgetReceiver.refreshAllWidgets(context)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: Appearance, Colors & Icon
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Palette, contentDescription = null, tint = primaryCustom)
                                            Text("المظهر، الخط، وأيقونة التطبيق", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = themeMode == 0, onClick = { themeMode = 0; prefs.edit().putInt("theme_mode", 0).apply() }, label = { Text("تلقائي") })
                                            FilterChip(selected = themeMode == 1, onClick = { themeMode = 1; prefs.edit().putInt("theme_mode", 1).apply() }, label = { Text("فاتح (للـ PDF)") })
                                            FilterChip(selected = themeMode == 2, onClick = { themeMode = 2; prefs.edit().putInt("theme_mode", 2).apply() }, label = { Text("داكن (للـ PDF)") })
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { appColorHex = "#059669"; prefs.edit().putString("app_color_hex", "#059669").apply() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("زمردي") }

                                            Button(
                                                onClick = { appColorHex = "#2563EB"; prefs.edit().putString("app_color_hex", "#2563EB").apply() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("ملكي") }

                                            Button(
                                                onClick = { appColorHex = "#7C3AED"; prefs.edit().putString("app_color_hex", "#7C3AED").apply() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("بنفسجي") }
                                        }

                                        Text("حجم الخط: ${String.format(Locale.US, "%.1fx", fontScale)}")
                                        Slider(
                                            value = fontScale,
                                            onValueChange = {
                                                fontScale = it
                                                prefs.edit().putFloat("font_scale", it).apply()
                                            },
                                            valueRange = 0.85f..1.35f
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("ألوان النظام (Material You)")
                                            Switch(
                                                checked = dynamicColor,
                                                onCheckedChange = {
                                                    dynamicColor = it
                                                    prefs.edit().putBoolean("dynamic_color", it).apply()
                                                }
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val pm = context.packageManager
                                                val def = ComponentName(context, "com.nanomks.elamal.LauncherDefault")
                                                val alt = ComponentName(context, "com.nanomks.elamal.LauncherAlt")
                                                val isDefEnabled = pm.getComponentEnabledSetting(def) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                                if (isDefEnabled) {
                                                    pm.setComponentEnabledSetting(alt, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                                                    pm.setComponentEnabledSetting(def, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                                    Toast.makeText(context, "تم التبديل إلى الأيقونة الزرقاء ✓", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    pm.setComponentEnabledSetting(def, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                                                    pm.setComponentEnabledSetting(alt, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                                    Toast.makeText(context, "تم التبديل إلى الأيقونة الخضراء ✓", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("تبديل أيقونة التطبيق (أخضر / أزرق)")
                                        }
                                    }
                                }
                            }

                            // Section 4: Security & 5 Rotating Backups
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = primaryCustom)
                                            Text("الأمان والنسخ الاحتياطي الذكي (5 نسخ دوارة)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        OutlinedTextField(
                                            value = pinSetting,
                                            onValueChange = {
                                                if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                                                    pinSetting = it
                                                    if (it.isEmpty() || it.length >= 4) {
                                                        savedPin = it
                                                        prefs.edit().putString("security_pin", it).apply()
                                                    }
                                                }
                                            },
                                            label = { Text("رمز القفل (4 إلى 8 أرقام - اتركه فارغاً للإلغاء)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تشفير النسخ الاحتياطية بكلمة مرور")
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
                                                label = { Text("كلمة مرور تشفير النسخ الاحتياطية") },
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
                                                    scope.launch {
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
                                                    scope.launch {
                                                        val msg = BackupSafManager.restoreFromBackup(context, dao)
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
                                                        scope.launch {
                                                            val msg = BackupSafManager.restoreFromBackup(context, dao, bf)
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
                        }
}
