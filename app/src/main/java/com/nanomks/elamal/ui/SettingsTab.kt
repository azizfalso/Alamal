package com.nanomks.elamal.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalSecurity
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@Composable
private fun CategorySectionHeader(title: String, subtitle: String, icon: ImageVector, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.SettingsTab() {
    val context = LocalContext.current
    val lastBackupMillis = prefs.getLong("last_backup_time", 0L)
    val backupAgeHours = if (lastBackupMillis > 0L) (System.currentTimeMillis() - lastBackupMillis) / 3600_000L else -1L
    var backupFilesList by remember(statusBanner) { mutableStateOf(BackupSafManager.listAvailableBackups(context)) }
    var encEnabled by remember { mutableStateOf(prefs.getBoolean("backup_encrypt_enabled", false)) }
    var backupPass by remember { mutableStateOf(ElamalSecurity.getBackupPassword(prefs)) }
    var newSsidInput by remember { mutableStateOf("") }

    var rateInput by remember(prefsRefreshCounter) {
        val cleanStr = if (hourlyRate % 1.0 == 0.0) hourlyRate.toInt().toString() else hourlyRate.toString()
        mutableStateOf(cleanStr)
    }

    val ssidList = workSsids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val mainColors = listOf(
        "#059669" to "زمردي",
        "#2563EB" to "ملكي",
        "#7C3AED" to "بنفسجي"
    )
    
    val coldColors = listOf(
        "#0D9488" to "فيروزي بارد",
        "#0284C7" to "سماوي جليدي",
        "#4F46E5" to "نيلي هادئ",
        "#475569" to "فولاذي بارد"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        // =========================================================================
        // CATEGORY 1: المظهر، الألوان الباردة، الخط، وأيقونة التطبيق
        // =========================================================================
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, primaryCustom.copy(alpha = 0.22f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySectionHeader(
                        title = "قسم المظهر والتخصيص",
                        subtitle = "الألوان الأساسية والباردة، حجم الخط الشامل، وأيقونة التطبيق",
                        icon = Icons.Default.Palette,
                        accentColor = primaryCustom
                    )

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Text("وضع الإضاءة للتطبيق:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = themeMode == 0,
                            onClick = { themeMode = 0; prefs.edit().putInt("theme_mode", 0).apply() },
                            label = { Text("تلقائي", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == 1,
                            onClick = { themeMode = 1; prefs.edit().putInt("theme_mode", 1).apply() },
                            label = { Text("☀️ فاتح", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == 2,
                            onClick = { themeMode = 2; prefs.edit().putInt("theme_mode", 2).apply() },
                            label = { Text("🌙 داكن", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("الألوان الكلاسيكية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mainColors.forEach { (hex, name) ->
                            val btnCol = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = appColorHex.equals(hex, ignoreCase = true)
                            Button(
                                onClick = { updateAppColor(hex) },
                                colors = ButtonDefaults.buttonColors(containerColor = btnCol),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))
                                        else Modifier
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }

                    Text("الألوان الباردة الهادئة (4 ألوان باردة):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        coldColors.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (hex, name) ->
                                    val btnCol = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = appColorHex.equals(hex, ignoreCase = true)
                                    Button(
                                        onClick = { updateAppColor(hex) },
                                        colors = ButtonDefaults.buttonColors(containerColor = btnCol),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .then(
                                                if (isSelected) Modifier.border(2.5.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))
                                                else Modifier
                                            ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showCustomColorPickerDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(primaryCustom)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار لون مخصص يدوياً (${appColorHex})", fontWeight = FontWeight.Bold, fontSize = 12.5f.sp)
                    }

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Surface(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("مزامنة أيقونة التطبيق مع لون التطبيق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = if (syncIconWithColor) "مفعّل: الأيقونة تتغير تلقائياً مع اللون المختار"
                                        else "متوقف: الأيقونة محتفظة بآخر لون قبل الإيقاف",
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncIconWithColor,
                                    onCheckedChange = { toggleSyncIconWithColor(it) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حجم الخط في كامل التطبيق: ${String.format(Locale.US, "%.2fx", fontScale)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (fontScale != 1.0f) {
                            TextButton(onClick = {
                                fontScale = 1.0f
                                prefs.edit().putFloat("font_scale", 1.0f).apply()
                            }) {
                                Text("إعادة للطبيعي (1.0x)", fontSize = 11.5f.sp)
                            }
                        }
                    }
                    Slider(
                        value = fontScale,
                        onValueChange = {
                            fontScale = it
                            prefs.edit().putFloat("font_scale", it).apply()
                        },
                        valueRange = 0.85f..1.30f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ألوان النظام التلقائية (Material You)", fontSize = 13.sp)
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = {
                                dynamicColor = it
                                prefs.edit().putBoolean("dynamic_color", it).apply()
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 2: قسم العمل والحسابات والتنبيهات
        // =========================================================================
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySectionHeader(
                        title = "قسم العمل والحسابات",
                        subtitle = "سعر الساعة، تنبيه نسيان الخروج، والبحث في الدفتر",
                        icon = Icons.Default.Calculate,
                        accentColor = Color(0xFF059669)
                    )

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { newText ->
                            if (newText.isEmpty() || newText.matches(Regex("""^\d*\.?\d*$"""))) {
                                rateInput = newText
                                newText.toDoubleOrNull()?.let { r ->
                                    if (r > 0.0) {
                                        hourlyRate = r
                                        prefs.edit().putFloat("hourly_rate", r.toFloat()).apply()
                                    }
                                }
                            }
                        },
                        label = { Text("سعر الساعة الافتراضي (DH) - لا يغير الأرشيف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("تنبيه نسيان الخروج: بعد $forgotAlarmHours ساعة (من 0 إلى 48)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
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
                        Text("إظهار شريط البحث والفلتر في الدفتر", fontSize = 13.sp)
                        Switch(
                            checked = searchFilterEnabled,
                            onCheckedChange = {
                                searchFilterEnabled = it
                                prefs.edit().putBoolean("search_filter_enabled", it).apply()
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 3: قسم التتبع الذكي بالواي فاي والودجت
        // =========================================================================
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySectionHeader(
                        title = "قسم التتبع الذكي والودجت",
                        subtitle = "رصد شبكات العمل بالرووت وودجت الشاشة الرئيسية",
                        icon = Icons.Default.Wifi,
                        accentColor = Color(0xFF2563EB)
                    )

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل تتبع شبكات العمل بالرووت", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                        Switch(
                            checked = rootWifiEnabled,
                            onCheckedChange = {
                                rootWifiEnabled = it
                                prefs.edit().putBoolean("root_wifi_enabled", it).apply()
                                ElamalUtils.syncPeriodicWifiPollSchedule(context)
                            }
                        )
                    }

                    if (rootWifiEnabled) {
                        Text(
                            text = "الشبكة المقروءة الآن: ${detectedRootSsid ?: "اضغط فحص لطلب صلاحية الرووت (su)"}",
                            fontSize = 12.5f.sp,
                            color = if (detectedRootSsid != null) Color(0xFF059669) else colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModelScope.launch {
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
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("فحص بالرووت الآن", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val current = detectedRootSsid
                                    if (current != null && ssidList.size < 6 && !ssidList.contains(current)) {
                                        val updated = (ssidList + current).joinToString(",")
                                        workSsids = updated
                                        prefs.edit().putString("work_ssids", updated).apply()
                                        ElamalUtils.syncPeriodicWifiPollSchedule(context)
                                        viewModelScope.launch(Dispatchers.IO) { ElamalUtils.pollAndMergeRootWifi(context, dao) }
                                    } else {
                                        Toast.makeText(context, "افحص الشبكة بالرووت أولاً أو أضف اسمها بالأسفل", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إضافة الشبكة الحالية", fontSize = 12.sp)
                            }
                        }

                        Text("قائمة شبكات العمل (${ssidList.size} / 6):", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

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
                                        ElamalUtils.syncPeriodicWifiPollSchedule(context)
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
                                            ElamalUtils.syncPeriodicWifiPollSchedule(context)
                                            newSsidInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("إضافة") }
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل ودجت الشاشة الرئيسية", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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

        // =========================================================================
        // CATEGORY 4: قسم الأمان وقفل التطبيق (PIN بدون كيبورد)
        // =========================================================================
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySectionHeader(
                        title = "قسم الأمان والخصوصية",
                        subtitle = "حماية التطبيق برمز PIN مدمج (PBKDF2) والبصمة",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFF7C3AED)
                    )

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (savedPin.isBlank()) "قفل التطبيق غير مفعّل حالياً" else "قفل التطبيق مفعّل ومحمي ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5f.sp,
                                color = if (savedPin.isBlank()) colorScheme.onSurfaceVariant else Color(0xFF059669)
                            )
                            Text(
                                text = "يستخدم لوحة أرقام مدمجة داخل التطبيق بدون الحاجة لكيبورد الهاتف",
                                fontSize = 11.5f.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showPinSetupDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (savedPin.isBlank()) "تعيين رمز" else "إدارة الرمز", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 5: قسم النسخ الاحتياطي الذكي والاستعادة (SAF & Keystore)
        // =========================================================================
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySectionHeader(
                        title = "قسم النسخ الاحتياطي والاستعادة",
                        subtitle = "الحفظ التلقائي الآمن عبر SAF وتشفير AES-256",
                        icon = Icons.Default.CloudSync,
                        accentColor = Color(0xFFD97706)
                    )

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تشفير النسخ الاحتياطية بكلمة مرور (AES-256)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                ElamalSecurity.saveBackupPassword(prefs, it)
                            },
                            label = { Text("كلمة مرور تشفير واستعادة النسخ (Keystore)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Text(
                        text = if (backupAgeHours >= 0) "⏱ عمر آخر نسخة احتياطية: منذ $backupAgeHours ساعة"
                        else "⚠️ تذكير: لم يتم أخذ نسخة احتياطية بعد!",
                        fontSize = 12.5f.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (backupAgeHours >= 0) Color(0xFF059669) else Color(0xFFD97706)
                    )

                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
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
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("نسخ يدوي الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = {
                                confirmRestoreFileName = "elamal_auto_backup.json"
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("استعادة التلقائية", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }

                    if (backupFilesList.isNotEmpty()) {
                        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
                        Text("النسخ المتوفرة في المجلد (${backupFilesList.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        for (bf in backupFilesList.take(8)) {
                            Surface(
                                color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(bf, fontSize = 11.5f.sp, fontWeight = FontWeight.Medium)
                                    TextButton(onClick = { confirmRestoreFileName = bf }) {
                                        Text("استعادة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
