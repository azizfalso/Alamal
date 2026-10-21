package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalSecurity
import com.nanomks.elamal.util.ElamalUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveDateTimeSelector(
    label: String,
    timestampMillis: Long,
    accentColor: Color,
    includeTime: Boolean = true,
    onTimestampChange: (Long) -> Unit
) {
    var showComposeDatePicker by rememberSaveable { mutableStateOf(false) }
    var showComposeTimePicker by rememberSaveable { mutableStateOf(false) }

    val cal = remember(timestampMillis) {
        Calendar.getInstance().apply { timeInMillis = timestampMillis }
    }

    if (showComposeDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestampMillis)
        DatePickerDialog(
            onDismissRequest = { showComposeDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtc ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = timestampMillis
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            onTimestampChange(updated.timeInMillis)
                        }
                        showComposeDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد التاريخ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showComposeDatePicker = false }) {
                    Text("إلغاء")
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = accentColor,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = accentColor,
                    selectedDayContentColor = Color.White,
                    todayContentColor = accentColor,
                    todayDateBorderColor = accentColor
                )
            )
        }
    }

    if (showComposeTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showComposeTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "اختر الوقت", tint = accentColor)
                    Text("اختر الوقت (24 ساعة)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                selectorColor = accentColor,
                                containerColor = MaterialTheme.colorScheme.surface,
                                timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                timeSelectorSelectedContentColor = accentColor,
                                timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = timestampMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        onTimestampChange(updated.timeInMillis)
                        showComposeTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الوقت", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showComposeTimePicker = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        modifier = Modifier.clickable {
                            val nowCal = Calendar.getInstance()
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = timestampMillis
                                set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, nowCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH))
                            }
                            onTimestampChange(updated.timeInMillis)
                        }
                    ) {
                        Text(
                            "اليوم",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.clickable {
                            val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = timestampMillis
                                set(Calendar.YEAR, yestCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, yestCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, yestCal.get(Calendar.DAY_OF_MONTH))
                            }
                            onTimestampChange(updated.timeInMillis)
                        }
                    ) {
                        Text(
                            "أمس",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showComposeDatePicker = true },
                    modifier = Modifier.weight(1.25f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "التاريخ", modifier = Modifier.size(16.dp), tint = accentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = ElamalUtils.formatDate(timestampMillis),
                            fontSize = 12.5f.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                if (includeTime) {
                    OutlinedButton(
                        onClick = { showComposeTimePicker = true },
                        modifier = Modifier.weight(0.95f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "الوقت", modifier = Modifier.size(16.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = ElamalUtils.formatTime(timestampMillis),
                                fontSize = 12.5f.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppState.AppDialogs() {

    // 1. Delete Entry Confirmation Dialog (#1)
    if (pendingDeleteEntry != null) {
        val item = pendingDeleteEntry!!
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = "تأكيد الحذف", tint = Color(0xFFDC2626)) },
            title = { Text("تأكيد حذف السجل", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل تريد نقل هذا السجل (${ElamalUtils.formatMoney(item.calculatedAmount)} بتاريخ ${ElamalUtils.formatDate(item.startTime)}) إلى المحذوفات؟\nيمكنك التراجع عن الحذف من الشريط العلوي."
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmSoftDeleteEntry() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("نعم، حذف السجل") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 2. Undo Latest Settlement Confirmation Dialog (#2)
    if (confirmUndoLatestSettlement) {
        val latest = settlements.firstOrNull()
        AlertDialog(
            onDismissRequest = { confirmUndoLatestSettlement = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Undo, contentDescription = "تأكيد التراجع عن الدفعة", tint = Color(0xFFDC2626)) },
            title = { Text("تأكيد التراجع عن آخر دفعة", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من إلغاء الدفعة الأخيرة (${ElamalUtils.formatMoney(latest?.amountPaid ?: 0.0)}) وإعادة جميع سجلاتها المؤرشفة إلى الدفتر النشط؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = { performConfirmedUndoLatestSettlement() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("نعم، التراجع وإعادة السجلات") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUndoLatestSettlement = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit Active Running Clock-In Start Time Dialog
    if (showEditActiveClockInDialog && activeClockIn > 0L) {
        var newActiveStart by rememberSaveable(activeClockIn) { mutableLongStateOf(activeClockIn) }

        AlertDialog(
            onDismissRequest = { showEditActiveClockInDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.HistoryToggleOff, contentDescription = "تعديل البداية", tint = primaryCustom) },
            title = { Text("تعديل وقت بداية الجلسة الجارية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "إذا نسيت تسجيل الدخول في بداية دوامك، يمكنك تعديل وقت البداية الفعلي هنا:",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    InteractiveDateTimeSelector(
                        label = "وقت البداية الفعلي:",
                        timestampMillis = newActiveStart,
                        accentColor = primaryCustom,
                        includeTime = true,
                        onTimestampChange = { newActiveStart = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newActiveStart > System.currentTimeMillis()) {
                            showNotice("لا يمكن أن يكون وقت البداية في المستقبل")
                            return@Button
                        }
                        updateActiveClockInStart(newActiveStart)
                        showEditActiveClockInDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ وتحديث العداد") }
            },
            dismissButton = {
                TextButton(onClick = { showEditActiveClockInDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Overlap Warning Confirmation Dialog
    if (overlapWarningMessage != null) {
        AlertDialog(
            onDismissRequest = {
                overlapWarningMessage = null
                pendingForcedAction = null
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = "تحذير تداخل", tint = Color(0xFFD97706)) },
            title = { Text("تنبيه تداخل في الأوقات", fontWeight = FontWeight.Bold) },
            text = { Text(overlapWarningMessage!!) },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingForcedAction
                        overlapWarningMessage = null
                        pendingForcedAction = null
                        action?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة على أي حال") }
            },
            dismissButton = {
                TextButton(onClick = {
                    overlapWarningMessage = null
                    pendingForcedAction = null
                }) { Text("إلغاء") }
            }
        )
    }

    // Confirm Restore Backup Dialog
    if (confirmRestoreFileName != null) {
        val targetFile = confirmRestoreFileName!!
        AlertDialog(
            onDismissRequest = { confirmRestoreFileName = null },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Restore, contentDescription = "استعادة نسخة", tint = primaryCustom) },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("سيتم حفظ نسخة أمان من بياناتك الحالية وفحصها أولاً، ثم استبدال السجلات والدفعات داخل معاملة آمنة بالنسخة:\n($targetFile)\nهل تود المتابعة؟")
            },
            confirmButton = {
                Button(
                    onClick = { performConfirmedRestore(targetFile) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("نعم، استعادة الآن") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreFileName = null }) { Text("إلغاء") }
            }
        )
    }

    // Custom Date Range Dialog
    if (showCustomPeriodPdfDialog) {
        var fromMillis by rememberSaveable { mutableLongStateOf(customReportStartMillis) }
        var toMillis by rememberSaveable { mutableLongStateOf(customReportEndMillis) }

        AlertDialog(
            onDismissRequest = { showCustomPeriodPdfDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.DateRange, contentDescription = "فترة مخصصة", tint = primaryCustom) },
            title = { Text("تحديد فترة مخصصة للتقارير", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = "من تاريخ البداية:",
                        timestampMillis = fromMillis,
                        accentColor = primaryCustom,
                        includeTime = false,
                        onTimestampChange = { fromMillis = it }
                    )
                    InteractiveDateTimeSelector(
                        label = "إلى تاريخ النهاية:",
                        timestampMillis = toMillis,
                        accentColor = primaryCustom,
                        includeTime = false,
                        onTimestampChange = { toMillis = it }
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val startDay = ElamalUtils.parseStrictDate(ElamalUtils.formatDate(fromMillis)) ?: fromMillis
                            val endDay = (ElamalUtils.parseStrictDate(ElamalUtils.formatDate(toMillis)) ?: toMillis) + 86399_000L
                            if (endDay < startDay) {
                                showNotice("تاريخ النهاية يجب أن يكون بعد البداية")
                                return@OutlinedButton
                            }
                            customReportStartMillis = startDay
                            customReportEndMillis = endDay
                            selectedReportScope = 5
                            showCustomPeriodPdfDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("عرض بالملخص") }

                    Button(
                        onClick = {
                            val startDay = ElamalUtils.parseStrictDate(ElamalUtils.formatDate(fromMillis)) ?: fromMillis
                            val endDay = (ElamalUtils.parseStrictDate(ElamalUtils.formatDate(toMillis)) ?: toMillis) + 86399_000L
                            if (endDay < startDay) {
                                showNotice("تاريخ النهاية يجب أن يكون بعد البداية")
                                return@Button
                            }
                            customReportStartMillis = startDay
                            customReportEndMillis = endDay
                            selectedReportScope = 5
                            val fromStr = ElamalUtils.formatDate(startDay)
                            val toStr = ElamalUtils.formatDate(endDay)
                            val rangeEntries = allEntries.filter { it.startTime in startDay..endDay }
                            exportPdfReportAsync("فترة $fromStr إلى $toStr", rangeEntries, settlements)
                            showCustomPeriodPdfDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("مشاركة PDF") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPeriodPdfDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Add Manual Past Hourly Session Dialog (#22 rememberSaveable & #23 imePadding)
    if (showManualHourlyDialog) {
        val now = System.currentTimeMillis()
        var startMillis by rememberSaveable { mutableLongStateOf(now - 4 * 3600_000L) }
        var endMillis by rememberSaveable { mutableLongStateOf(now) }
        var breakStr by rememberSaveable { mutableStateOf("0") }
        var noteStr by rememberSaveable { mutableStateOf("") }

        val breakMinutes = breakStr.toIntOrNull() ?: 0
        val netDurationMin = (((endMillis - startMillis) / 60000L) - breakMinutes).coerceAtLeast(0L)
        val previewAmount = if (endMillis > startMillis) {
            ElamalUtils.calculateHourlyPay(startMillis, endMillis, breakMinutes, hourlyRate)
        } else 0.0

        AlertDialog(
            onDismissRequest = { showManualHourlyDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.MoreTime, contentDescription = "إضافة ساعات يدوية", tint = primaryCustom) },
            title = { Text("إضافة جلسة ساعات يدوية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = "وقت بداية الجلسة:",
                        timestampMillis = startMillis,
                        accentColor = primaryCustom,
                        includeTime = true,
                        onTimestampChange = { newStart ->
                            startMillis = newStart
                            if (endMillis <= newStart) {
                                endMillis = newStart + 3600_000L
                            }
                        }
                    )

                    InteractiveDateTimeSelector(
                        label = "وقت نهاية الجلسة:",
                        timestampMillis = endMillis,
                        accentColor = primaryCustom,
                        includeTime = true,
                        onTimestampChange = { endMillis = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مدة سريعة:", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        listOf(2 to "ساعتان", 4 to "4 ساعات", 8 to "8 ساعات").forEach { (hrs, title) ->
                            SuggestionChip(
                                onClick = { endMillis = startMillis + hrs * 3600_000L },
                                label = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }

                    Surface(
                        color = if (endMillis > startMillis) primaryCustom.copy(alpha = 0.14f) else Color(0xFFEF4444).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (endMillis > startMillis) "⏱ الصافي: ${ElamalUtils.formatDurationArabic(netDurationMin)}" else "⚠️ وقت النهاية يجب أن يلي البداية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (endMillis > startMillis) primaryCustom else Color(0xFFEF4444)
                            )
                            if (endMillis > startMillis) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = ElamalUtils.formatMoney(previewAmount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryCustom
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = breakStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) breakStr = it },
                        label = { Text("الاستراحة (دقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("الملاحظة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (endMillis <= startMillis) {
                            showNotice("تأكد أن وقت النهاية بعد وقت البداية")
                            return@Button
                        }
                        val brk = breakStr.toIntOrNull() ?: 0
                        saveManualHourlyEntry(startMillis, endMillis, brk, noteStr, forceIgnoreOverlap = false)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة السجل") }
            },
            dismissButton = {
                TextButton(onClick = { showManualHourlyDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit Existing Entry Dialog
    if (editingEntry != null) {
        val target = editingEntry!!
        var startMillis by rememberSaveable(target.id) { mutableLongStateOf(target.startTime) }
        var endMillis by rememberSaveable(target.id) { mutableLongStateOf(target.endTime ?: target.startTime) }
        var noteStr by rememberSaveable(target.id) { mutableStateOf(target.note) }
        var breakStr by rememberSaveable(target.id) { mutableStateOf(target.breakMinutes.toString()) }
        var amountStr by rememberSaveable(target.id) { mutableStateOf(kotlin.math.abs(target.calculatedAmount).toString()) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.Edit, contentDescription = "تعديل السجل", tint = primaryCustom) },
            title = { Text("تعديل السجل وتفاصيل الوقت", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = if (target.type == EntryType.HOURLY) "وقت البداية:" else "التاريخ والوقت:",
                        timestampMillis = startMillis,
                        accentColor = primaryCustom,
                        includeTime = true,
                        onTimestampChange = { startMillis = it }
                    )

                    if (target.type == EntryType.HOURLY) {
                        InteractiveDateTimeSelector(
                            label = "وقت النهاية:",
                            timestampMillis = endMillis,
                            accentColor = primaryCustom,
                            includeTime = true,
                            onTimestampChange = { endMillis = it }
                        )
                        OutlinedTextField(
                            value = breakStr,
                            onValueChange = { if (it.all { c -> c.isDigit() }) breakStr = it },
                            label = { Text("الاستراحة (دقائق)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("المبلغ (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("الملاحظة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val effectiveEnd = if (target.type == EntryType.HOURLY) endMillis else startMillis
                        if (target.type == EntryType.HOURLY && effectiveEnd <= startMillis) {
                            showNotice("تأكد أن وقت النهاية بعد وقت البداية")
                            return@Button
                        }
                        val brk = breakStr.toIntOrNull() ?: target.breakMinutes
                        val rawAmt = kotlin.math.abs(amountStr.toDoubleOrNull() ?: target.calculatedAmount)
                        saveEditedEntry(target, startMillis, effectiveEnd, brk, rawAmt, noteStr, forceIgnoreOverlap = false)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ التعديلات") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) { Text("إلغاء") }
            }
        )
    }

    if (confirmIgnoreAllWifi) {
        AlertDialog(
            onDismissRequest = { confirmIgnoreAllWifi = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("تأكيد تجاهل الكل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من تجاهل جميع فترات الواي فاي المقترحة؟") },
            confirmButton = {
                Button(
                    onClick = { ignoreAllPendingWifiSessions() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("نعم، تجاهل الكل") }
            },
            dismissButton = {
                TextButton(onClick = { confirmIgnoreAllWifi = false }) { Text("إلغاء") }
            }
        )
    }

    if (editingSettlement != null) {
        val target = editingSettlement!!
        var paidInput by rememberSaveable(target.id) { mutableStateOf(target.amountPaid.toString()) }
        var noteInput by rememberSaveable(target.id) { mutableStateOf(target.note) }
        var settlementTimeMillis by rememberSaveable(target.id) { mutableLongStateOf(target.timestamp) }

        AlertDialog(
            onDismissRequest = { editingSettlement = null },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("تعديل دفعة قديمة (مع تحديث المرحّل)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = "تاريخ ووقت الدفعة:",
                        timestampMillis = settlementTimeMillis,
                        accentColor = primaryCustom,
                        includeTime = true,
                        onTimestampChange = { settlementTimeMillis = it }
                    )
                    OutlinedTextField(
                        value = paidInput,
                        onValueChange = { paidInput = it },
                        label = { Text("المبلغ المستلم (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("الملاحظة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPaid = paidInput.toDoubleOrNull() ?: target.amountPaid
                        saveEditedSettlement(target, settlementTimeMillis, newPaid, noteInput)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ وإعادة حساب المرحّل") }
            },
            dismissButton = {
                TextButton(onClick = { editingSettlement = null }) { Text("إلغاء") }
            }
        )
    }

    if (showClockOutDialog) {
        var breakMinStr by rememberSaveable { mutableStateOf("0") }
        var noteStr by rememberSaveable { mutableStateOf("") }
        val elapsedMin = if (activeClockIn > 0L) ((System.currentTimeMillis() - activeClockIn) / 60000L).coerceAtLeast(1L) else 0L

        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("تسجيل الخروج وإنهاء الجلسة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = primaryCustom.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⏱ المدة الإجمالية قبل الاستراحة: ${ElamalUtils.formatDurationArabic(elapsedMin)}",
                            color = primaryCustom,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = breakMinStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) breakMinStr = it },
                        label = { Text("مدة الاستراحة (بالدقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة الجلسة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val brk = breakMinStr.toIntOrNull() ?: 0
                        completeClockOut(brk, noteStr)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ الخروج") }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Remote Work Dialog
    if (showRemoteDialog) {
        var amountStr by rememberSaveable { mutableStateOf("") }
        var noteStr by rememberSaveable { mutableStateOf("") }
        var selectedTimeMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

        AlertDialog(
            onDismissRequest = { showRemoteDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.Computer, contentDescription = "عمل عن بعد", tint = Color(0xFF2563EB)) },
            title = { Text("إضافة عمل عن بعد (أجرة ثابتة)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = "تاريخ ووقت العمل عن بعد:",
                        timestampMillis = selectedTimeMillis,
                        accentColor = Color(0xFF2563EB),
                        includeTime = true,
                        onTimestampChange = { selectedTimeMillis = it }
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ الثابت (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة العمل (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            saveFixedEntry(EntryType.REMOTE, amt, noteStr, selectedTimeMillis)
                        } else {
                            showNotice("أدخل مبلغاً صحيحاً أكبر من صفر")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة للدفتر") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Credit / Advance Dialog
    if (showCreditDialog) {
        var creditStr by rememberSaveable { mutableStateOf("") }
        var noteStr by rememberSaveable { mutableStateOf("") }
        var selectedTimeMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "إضافة كريدي", tint = Color(0xFFDC2626)) },
            title = { Text("إضافة كريدي / سلفة (يُخصم من الصافي)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveDateTimeSelector(
                        label = "تاريخ ووقت الكريدي:",
                        timestampMillis = selectedTimeMillis,
                        accentColor = Color(0xFFDC2626),
                        includeTime = true,
                        onTimestampChange = { selectedTimeMillis = it }
                    )
                    OutlinedTextField(
                        value = creditStr,
                        onValueChange = { creditStr = it },
                        label = { Text("قيمة الكريدي (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = kotlin.math.abs(creditStr.toDoubleOrNull() ?: 0.0)
                        if (amt > 0) {
                            saveFixedEntry(EntryType.CREDIT, -amt, noteStr, selectedTimeMillis)
                        } else {
                            showNotice("أدخل قيمة الكريدي")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ بالسالب") }
            },
            dismissButton = {
                TextButton(onClick = { showCreditDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showSettleDialog) {
        val prevCarry = settlements.firstOrNull()?.nextCarryOver ?: 0.0
        val entriesSum = ElamalUtils.roundMoney(entries.sumOf { it.calculatedAmount })
        val netDue = ElamalUtils.roundMoney(entriesSum + prevCarry)
        var paidStr by rememberSaveable(netDue) { mutableStateOf(String.format(Locale.US, "%.2f", netDue)) }
        var noteStr by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("استلام دفعة وأرشفة السجلات", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = primaryCustom.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("المجموع الصافي المستحق:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(ElamalUtils.formatMoney(netDue), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = primaryCustom)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it },
                        label = { Text("المبلغ المستلم فعلياً (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة الدفعة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paid = paidStr.toDoubleOrNull() ?: 0.0
                        createNewSettlement(entriesSum, prevCarry, paid, noteStr)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تأكيد وترحيل الفرق") }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Manual RGB / Hex Color Picker Dialog
    if (showCustomColorPickerDialog) {
        val initialInt = runCatching { android.graphics.Color.parseColor(appColorHex) }
            .getOrDefault(android.graphics.Color.parseColor("#059669"))
        var rVal by rememberSaveable { mutableFloatStateOf(android.graphics.Color.red(initialInt).toFloat()) }
        var gVal by rememberSaveable { mutableFloatStateOf(android.graphics.Color.green(initialInt).toFloat()) }
        var bVal by rememberSaveable { mutableFloatStateOf(android.graphics.Color.blue(initialInt).toFloat()) }
        val composedHex = String.format(Locale.US, "#%02X%02X%02X", rVal.toInt(), gVal.toInt(), bVal.toInt())
        val previewColor = Color(rVal.toInt(), gVal.toInt(), bVal.toInt())

        AlertDialog(
            onDismissRequest = { showCustomColorPickerDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.ColorLens, contentDescription = "اختيار لون", tint = previewColor) },
            title = { Text("اختيار لون مخصص يدوياً", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(composedHex, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    Text("الأحمر (R: ${rVal.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    Slider(
                        value = rVal,
                        onValueChange = { rVal = it },
                        valueRange = 0f..220f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                    )

                    Text("الأخضر (G: ${gVal.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Slider(
                        value = gVal,
                        onValueChange = { gVal = it },
                        valueRange = 0f..220f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )

                    Text("الأزرق (B: ${bVal.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    Slider(
                        value = bVal,
                        onValueChange = { bVal = it },
                        valueRange = 0f..235f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateAppColor(composedHex)
                        showCustomColorPickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = previewColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("اعتماد اللون") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomColorPickerDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // In-App PIN Setup Dialog
    if (showPinSetupDialog) {
        var newPin by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    if (savedPin.isBlank()) "تعيين رمز قفل جديد (4-8 أرقام)" else "تحديث رمز القفل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = primaryCustom.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (newPin.isEmpty()) "اضغط الأرقام بالأسفل" else "• ".repeat(newPin.length).trim(),
                                fontSize = if (newPin.isEmpty()) 13.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryCustom
                            )
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9")
                        )
                        rows.forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                r.forEach { d ->
                                    FilledTonalButton(
                                        onClick = { if (newPin.length < 8) newPin += d },
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(d, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { newPin = "" },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("C", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { if (newPin.length < 8) newPin += "0" },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { if (newPin.isNotEmpty()) newPin = newPin.dropLast(1) },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "مسح رقم", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length >= 4) {
                            val hashed = ElamalSecurity.hashPinPbkdf2(newPin)
                            savedPin = hashed
                            flagSecureEnabled = true
                            prefs.edit()
                                .putString("security_pin", hashed)
                                .putInt("security_pin_length", newPin.length)
                                .putBoolean("flag_secure_enabled", true)
                                .apply()
                            showNotice("تم حفظ وتشفير رمز القفل بنجاح ✓")
                            showPinSetupDialog = false
                        } else {
                            showNotice("أدخل 4 أرقام على الأقل")
                        }
                    },
                    enabled = newPin.length >= 4,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ الرمز") }
            },
            dismissButton = {
                Row {
                    if (savedPin.isNotBlank()) {
                        TextButton(onClick = {
                            savedPin = ""
                            prefs.edit()
                                .putString("security_pin", "")
                                .putInt("security_pin_length", 0)
                                .apply()
                            showNotice("تم إلغاء قفل التطبيق ✓")
                            showPinSetupDialog = false
                        }) {
                            Text("إلغاء القفل", color = Color(0xFFEF4444))
                        }
                    }
                    TextButton(onClick = { showPinSetupDialog = false }) { Text("إغلاق") }
                }
            }
        )
    }
}
