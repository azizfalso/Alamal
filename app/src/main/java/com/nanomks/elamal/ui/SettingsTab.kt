package com.nanomks.elamal.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.MainActivity
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalSecurity
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Unified, harmonious Settings Switch Card:
 * Ensures every toggle row has the exact same container style, internal padding,
 * `Modifier.weight(1f)` on the text column so text NEVER collides with the Switch,
 * and clickable row behavior.
 */
@Composable
private fun UnifiedSettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5f.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }
    }
}

/**
 * Harmonious, legible Segmented Choice Button used for Theme Mode, Widget Background, and Widget Text Color.
 * Prevents cramped tiny fonts or truncated labels.
 */
@Composable
private fun ChoiceOptionButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpandableCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isExpanded) accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, fontSize = 11.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "طي/توسيع",
                    tint = accentColor
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.SettingsTab(activity: FragmentActivity? = null) {
    val context = LocalContext.current
    val lastBackupMillis = prefs.getLong("last_backup_time", 0L)
    val backupAgeHours = if (lastBackupMillis > 0L) (System.currentTimeMillis() - lastBackupMillis) / 3600_000L else -1L
    var backupFilesList by remember(statusBanner) { mutableStateOf(BackupSafManager.listAvailableBackups(context)) }
    var encEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("backup_encrypt_enabled", false)) }
    var backupPass by rememberSaveable { mutableStateOf(ElamalSecurity.getBackupPassword(prefs)) }
    var keystoreError by remember(statusBanner, backupPass) { mutableStateOf(ElamalSecurity.hasKeystoreDecryptionError(prefs)) }
    var newSsidInput by rememberSaveable { mutableStateOf("") }

    var expandedCatIndex by rememberSaveable { mutableIntStateOf(0) }

    var rateInput by rememberSaveable(prefsRefreshCounter) {
        val cleanStr = if (hourlyRate % 1.0 == 0.0) hourlyRate.toInt().toString() else hourlyRate.toString()
        mutableStateOf(cleanStr)
    }
    var currencyInput by rememberSaveable(prefsRefreshCounter) {
        mutableStateOf(currencySymbol)
    }

    val ssidList = remember(workSsids) {
        workSsids.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

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

    val samplePreviewEntry = remember(hourlyRate, currencySymbol) {
        val now = System.currentTimeMillis()
        WorkEntry(
            id = 1,
            type = EntryType.HOURLY,
            startTime = now - 8 * 3600_000L,
            endTime = now,
            breakMinutes = 30,
            hourlyRateAtEntry = hourlyRate,
            calculatedAmount = ElamalUtils.calculateHourlyPay(now - 8 * 3600_000L, now, 30, hourlyRate),
            note = "معاينة حية لحجم الخط والمظهر"
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        // =========================================================================
        // CATEGORY 0: المظهر والتخصيص
        // =========================================================================
        item {
            ExpandableCategoryCard(
                title = "قسم المظهر والتخصيص",
                subtitle = "الألوان، معاينة حجم الخط، وأيقونة التطبيق",
                icon = Icons.Default.Palette,
                accentColor = primaryCustom,
                isExpanded = expandedCatIndex == 0,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 0) -1 else 0 }
            ) {
                Text("وضع الإضاءة للتطبيق:", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceOptionButton(
                        label = "تلقائي",
                        selected = themeMode == 0,
                        accentColor = primaryCustom,
                        modifier = Modifier.weight(1f),
                        onClick = { themeMode = 0; prefs.edit().putInt("theme_mode", 0).apply() }
                    )
                    ChoiceOptionButton(
                        label = "☀️ فاتح",
                        selected = themeMode == 1,
                        accentColor = primaryCustom,
                        modifier = Modifier.weight(1f),
                        onClick = { themeMode = 1; prefs.edit().putInt("theme_mode", 1).apply() }
                    )
                    ChoiceOptionButton(
                        label = "🌙 داكن",
                        selected = themeMode == 2,
                        accentColor = primaryCustom,
                        modifier = Modifier.weight(1f),
                        onClick = { themeMode = 2; prefs.edit().putInt("theme_mode", 2).apply() }
                    )
                }

                Text("الألوان الكلاسيكية:", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
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
                                    if (isSelected) Modifier.border(2.5.dp, colorScheme.onSurface, RoundedCornerShape(12.dp))
                                    else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(name, fontSize = 12.5f.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                    }
                }

                Text("الألوان الباردة الهادئة:", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
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
                                            if (isSelected) Modifier.border(2.5.dp, colorScheme.onSurface, RoundedCornerShape(12.dp))
                                            else Modifier
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text(name, fontSize = 12.5f.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
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
                    Text("منتقي الألوان التفاعلي (${appColorHex})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                UnifiedSettingSwitchRow(
                    title = "مزامنة أيقونة التطبيق مع لون التطبيق",
                    subtitle = if (syncIconWithColor) "مفعّل: الأيقونة تتغير تلقائياً" else "متوقف: الأيقونة ثابتة",
                    checked = syncIconWithColor,
                    accentColor = primaryCustom,
                    onCheckedChange = { toggleSyncIconWithColor(it) }
                )

                UnifiedSettingSwitchRow(
                    title = "ألوان النظام التلقائية (Material You)",
                    subtitle = "استخدام لوحة ألوان خلفية الهاتف في أندرويد 12+",
                    checked = dynamicColor,
                    accentColor = primaryCustom,
                    onCheckedChange = {
                        dynamicColor = it
                        prefs.edit().putBoolean("dynamic_color", it).apply()
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حجم الخط الشامل: ${(fontScale * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                    if (fontScale != 1.0f) {
                        TextButton(onClick = {
                            fontScale = 1.0f
                            prefs.edit().putFloat("font_scale", 1.0f).apply()
                        }) {
                            Text("الحجم الافتراضي (100%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                EntryRowCard(
                    item = samplePreviewEntry,
                    fontScale = fontScale,
                    onEdit = {},
                    onDelete = {}
                )

                Slider(
                    value = fontScale,
                    onValueChange = {
                        fontScale = it
                        prefs.edit().putFloat("font_scale", it).apply()
                    },
                    valueRange = 0.85f..1.30f
                )
            }
        }

        // =========================================================================
        // CATEGORY 1: قسم ودجت الشاشة الرئيسية
        // =========================================================================
        item {
            val widgetAccent = Color(0xFF0D9488)
            ExpandableCategoryCard(
                title = "قسم ودجت الشاشة الرئيسية",
                subtitle = "تخصيص خلفية الودجت، لون النص، والتأكيد المزدوج",
                icon = Icons.Default.Widgets,
                accentColor = widgetAccent,
                isExpanded = expandedCatIndex == 1,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 1) -1 else 1 }
            ) {
                UnifiedSettingSwitchRow(
                    title = "تفعيل ودجت الشاشة الرئيسية",
                    subtitle = "عند الإيقاف يظهر «معطّل» ولا يستجيب للمس",
                    checked = widgetEnabled,
                    accentColor = widgetAccent,
                    onCheckedChange = {
                        widgetEnabled = it
                        prefs.edit().putBoolean("widget_enabled", it).apply()
                        ElamalWidgetReceiver.refreshAllWidgets(context)
                    }
                )

                Text("خلفية الودجت:", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                // 2x2 grid for widget background options so font is large, clear, and symmetrical
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceOptionButton(
                            label = "تلقائي (حسب الوضع)",
                            selected = widgetBgStyle == 0,
                            accentColor = widgetAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { updateWidgetAppearance(bg = 0) }
                        )
                        ChoiceOptionButton(
                            label = "شفاف زجاجي",
                            selected = widgetBgStyle == 1,
                            accentColor = widgetAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { updateWidgetAppearance(bg = 1) }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceOptionButton(
                            label = "بلا خلفية",
                            selected = widgetBgStyle == 2,
                            accentColor = widgetAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { updateWidgetAppearance(bg = 2) }
                        )
                        ChoiceOptionButton(
                            label = "داكن دائماً",
                            selected = widgetBgStyle == 3,
                            accentColor = widgetAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { updateWidgetAppearance(bg = 3) }
                        )
                    }
                }

                Text("لون نص الودجت:", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceOptionButton(
                        label = "تلقائي",
                        selected = widgetFgStyle == 0,
                        accentColor = widgetAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { updateWidgetAppearance(fg = 0) }
                    )
                    ChoiceOptionButton(
                        label = "أبيض دائماً",
                        selected = widgetFgStyle == 1,
                        accentColor = widgetAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { updateWidgetAppearance(fg = 1) }
                    )
                    ChoiceOptionButton(
                        label = "أسود دائماً",
                        selected = widgetFgStyle == 2,
                        accentColor = widgetAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { updateWidgetAppearance(fg = 2) }
                    )
                }

                UnifiedSettingSwitchRow(
                    title = "إظهار العنوان الفرعي أعلى حالة الودجت",
                    subtitle = "يعرض عبارة «العمل • تتبع الساعات» فوق زر الحالة",
                    checked = widgetShowSubtitle,
                    accentColor = widgetAccent,
                    onCheckedChange = { updateWidgetAppearance(showSub = it) }
                )
            }
        }

        // =========================================================================
        // CATEGORY 2: قسم العمل والحسابات
        // =========================================================================
        item {
            val workAccent = Color(0xFF059669)
            ExpandableCategoryCard(
                title = "قسم العمل والحسابات",
                subtitle = "العملة، سعر الساعة، وتنبيه نسيان الخروج",
                icon = Icons.Default.Calculate,
                accentColor = workAccent,
                isExpanded = expandedCatIndex == 2,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 2) -1 else 2 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { newCur ->
                            if (newCur.length <= 6) {
                                currencyInput = newCur
                                updateCurrencySymbol(newCur)
                            }
                        },
                        label = { Text("العملة") },
                        placeholder = { Text("DH") },
                        modifier = Modifier.weight(0.75f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { newText ->
                            if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d*$"))) {
                                rateInput = newText
                                newText.toDoubleOrNull()?.let { r ->
                                    if (r > 0.0) {
                                        hourlyRate = r
                                        prefs.edit().putFloat("hourly_rate", r.toFloat()).apply()
                                    }
                                }
                            }
                        },
                        label = { Text("سعر الساعة ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.25f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("تنبيه نسيان الخروج: بعد $forgotAlarmHours ساعة (0 للإيقاف)", fontWeight = FontWeight.SemiBold, fontSize = 13.5f.sp)
                Slider(
                    value = forgotAlarmHours.toFloat(),
                    onValueChange = {
                        forgotAlarmHours = it.toInt()
                        prefs.edit().putInt("forgot_alarm_hours", forgotAlarmHours).apply()
                    },
                    valueRange = 0f..48f
                )

                UnifiedSettingSwitchRow(
                    title = "إظهار شريط البحث والفلتر في الدفتر",
                    subtitle = "يسمح بالبحث الفوري في الملاحظات وتصفية السجلات",
                    checked = searchFilterEnabled,
                    accentColor = workAccent,
                    onCheckedChange = {
                        searchFilterEnabled = it
                        prefs.edit().putBoolean("search_filter_enabled", it).apply()
                    }
                )
            }
        }

        // =========================================================================
        // CATEGORY 3: قسم التتبع الذكي والواي فاي
        // =========================================================================
        item {
            val wifiAccent = Color(0xFF2563EB)
            ExpandableCategoryCard(
                title = "قسم التتبع الذكي والواي فاي",
                subtitle = "يعمل بدون صلاحية الموقع (بالروت أو التتبع العام)",
                icon = Icons.Default.Wifi,
                accentColor = wifiAccent,
                isExpanded = expandedCatIndex == 3,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 3) -1 else 3 }
            ) {
                UnifiedSettingSwitchRow(
                    title = "تفعيل تتبع شبكات العمل تلقائياً",
                    subtitle = "يرصد فترات الاتصال بالواي فاي ويقترحها في الدفتر",
                    checked = rootWifiEnabled,
                    accentColor = wifiAccent,
                    onCheckedChange = {
                        rootWifiEnabled = it
                        prefs.edit().putBoolean("root_wifi_enabled", it).apply()
                        ElamalUtils.syncPeriodicWifiPollSchedule(context)
                    }
                )

                if (rootWifiEnabled) {
                    UnifiedSettingSwitchRow(
                        title = "استخدام أوامر الروت (su) لمعرفة اسم الشبكة",
                        subtitle = "عند إيقافه: يسجّل فترات اتصال الواي فاي بدون روت وبدون صلاحية الموقع",
                        checked = wifiUseRootSu,
                        accentColor = wifiAccent,
                        onCheckedChange = {
                            wifiUseRootSu = it
                            prefs.edit().putBoolean("wifi_use_root_su", it).apply()
                            ElamalUtils.syncPeriodicWifiPollSchedule(context)
                        }
                    )

                    UnifiedSettingSwitchRow(
                        title = "منبه موقظ (RTC_WAKEUP) للفحص الدوري",
                        subtitle = "يمنع ضياع جلسات الواي فاي في وضع التوفير العميق Doze",
                        checked = wifiWakeAlarm,
                        accentColor = wifiAccent,
                        onCheckedChange = {
                            wifiWakeAlarm = it
                            prefs.edit().putBoolean("wifi_wake_alarm", it).apply()
                            ElamalUtils.syncPeriodicWifiPollSchedule(context)
                        }
                    )

                    if (wifiUseRootSu) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الشبكة المقروءة الآن:",
                                fontSize = 12.5f.sp,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = detectedRootSsid ?: "اضغط فحص لقراءة الـ SSID",
                                    fontSize = 12.5f.sp,
                                    color = if (detectedRootSsid != null) Color(0xFF059669) else colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModelScope.launch {
                                        val ssid = withContext(Dispatchers.IO) {
                                            ElamalUtils.pollAndMergeRootWifi(context, dao, forceFresh = true)
                                        }
                                        detectedRootSsid = ssid
                                        if (ssid != null) {
                                            showNotice("تم قراءة الشبكة: $ssid ✓")
                                        } else {
                                            showNotice("تعذر قراءة اسم الشبكة؛ تأكد من صلاحية الروت أو أضفها يدوياً")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("فحص الشبكة الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        showNotice("تمت إضافة الشبكة $current ✓")
                                    } else {
                                        showNotice("افحص الشبكة أولاً أو أضف اسمها بالأسفل")
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إضافة الشبكة الحالية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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
                                    label = {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(itemSsid)
                                        }
                                    },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "حذف الشبكة", modifier = Modifier.size(15.dp)) }
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
                                            showNotice("تمت إضافة $clean ✓")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("إضافة", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 4: قسم الأمان والخصوصية
        // =========================================================================
        item {
            val secAccent = Color(0xFF7C3AED)
            ExpandableCategoryCard(
                title = "قسم الأمان والخصوصية",
                subtitle = "حماية التطبيق برمز PIN، البصمة، وحجب لقطة الشاشة",
                icon = Icons.Default.Security,
                accentColor = secAccent,
                isExpanded = expandedCatIndex == 4,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 4) -1 else 4 }
            ) {
                Surface(
                    color = colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (savedPin.isBlank()) "قفل التطبيق غير مفعّل حالياً" else "قفل التطبيق مفعّل ومحمي ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5f.sp,
                                color = if (savedPin.isBlank()) colorScheme.onSurface else Color(0xFF059669)
                            )
                            Text("يقفل تلقائياً بعد 60 ثانية بالخلفية", fontSize = 11.5f.sp, color = colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showPinSetupDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secAccent)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = "إدارة الرمز", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (savedPin.isBlank()) "تعيين رمز" else "إدارة الرمز", fontSize = 12.5f.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                UnifiedSettingSwitchRow(
                    title = "حجب الأرقام في التطبيقات الأخيرة",
                    subtitle = "تفعيل حماية الشاشة (FLAG_SECURE) لمنع المعاينة واللقطات",
                    checked = flagSecureEnabled,
                    accentColor = secAccent,
                    onCheckedChange = { enabled ->
                        flagSecureEnabled = enabled
                        prefs.edit().putBoolean("flag_secure_enabled", enabled).apply()
                        (activity as? MainActivity)?.applyFlagSecureFromPrefs()
                    }
                )
            }
        }

        // =========================================================================
        // CATEGORY 5: قسم النسخ الاحتياطي والاستعادة
        // =========================================================================
        item {
            val bkAccent = Color(0xFFD97706)
            ExpandableCategoryCard(
                title = "قسم النسخ الاحتياطي والاستعادة",
                subtitle = "الحفظ التلقائي، النسخ اليومية، وتشفير AES-256",
                icon = Icons.Default.CloudSync,
                accentColor = bkAccent,
                isExpanded = expandedCatIndex == 5,
                onToggle = { expandedCatIndex = if (expandedCatIndex == 5) -1 else 5 }
            ) {
                UnifiedSettingSwitchRow(
                    title = "تشفير النسخ الاحتياطية بكلمة مرور",
                    subtitle = "تشفير قوي بمعيار AES-256-GCM مع حماية Keystore",
                    checked = encEnabled,
                    accentColor = bkAccent,
                    onCheckedChange = {
                        encEnabled = it
                        prefs.edit().putBoolean("backup_encrypt_enabled", it).apply()
                    }
                )

                if (encEnabled) {
                    if (keystoreError) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "⚠️ تعذر قراءة كلمة المرور من Keystore! أعد إدخال كلمة المرور أدناه.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.5f.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = backupPass,
                        onValueChange = {
                            backupPass = it
                            val ok = ElamalSecurity.saveBackupPassword(prefs, it)
                            keystoreError = !ok
                        },
                        label = { Text("كلمة مرور تشفير واستعادة النسخ") },
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
                    Icon(Icons.Default.FolderOpen, contentDescription = "اختيار مجلد النسخ")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (backupUriStr.isBlank()) "اختيار مجلد النسخ الاحتياطي (SAF)" else "تغيير مجلد النسخ الاحتياطي ✓", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            viewModelScope.launch {
                                val msg = BackupSafManager.performSmartBackup(context, dao, isManual = true)
                                statusBanner = msg
                                backupFilesList = BackupSafManager.listAvailableBackups(context)
                                showNotice(msg)
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("نسخ يدوي الآن", fontSize = 12.5f.sp, fontWeight = FontWeight.Bold) }

                    OutlinedButton(
                        onClick = { confirmRestoreFileName = "elamal_auto_backup.json" },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("استعادة التلقائية", fontSize = 12.5f.sp, fontWeight = FontWeight.Bold) }
                }

                if (backupFilesList.isNotEmpty()) {
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
                    Text("النسخ المتوفرة في المجلد (${backupFilesList.size}):", fontWeight = FontWeight.Bold, fontSize = 13.5f.sp)
                    for (bf in backupFilesList.take(10)) {
                        Surface(
                            color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = BackupSafManager.formatBackupFileLabel(bf),
                                        fontSize = 12.5f.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(bf, fontSize = 10.5f.sp, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { confirmRestoreFileName = bf }) {
                                        Text("استعادة", fontSize = 12.5f.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = {
                                        val ok = BackupSafManager.deleteBackupFile(context, bf)
                                        if (ok) {
                                            backupFilesList = BackupSafManager.listAvailableBackups(context)
                                            showNotice("تم حذف النسخة بنجاح")
                                        }
                                    }) {
                                        Text("حذف", fontSize = 12.5f.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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
