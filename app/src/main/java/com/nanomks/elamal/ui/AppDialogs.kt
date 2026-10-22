package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalSecurity
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@Composable
private fun QuickBreakField(
    breakInput: String,
    accentColor: Color,
    onBreakChange: (String) -> Unit
) {
    val presets = listOf(0, 15, 30, 45, 60)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = breakInput,
            onValueChange = { if (it.all { ch -> ch.isDigit() }) onBreakChange(it) },
            label = { Text("وقت الاستراحة (بالدقائق)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { mins ->
                val isSelected = (breakInput.toIntOrNull() ?: 0) == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBreakChange(mins.toString()) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = if (mins == 0) "بدون" else "${ElamalUtils.localizeDigits(mins.toString())}د",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerSection(
    label: String,
    timestampMillis: Long,
    accentColor: Color,
    showQuickDays: Boolean = true,
    onTimestampChange: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var useKeyboardTimeInput by remember { mutableStateOf(false) }

    val dayName = remember(timestampMillis) { ElamalUtils.getArabicDayName(timestampMillis) }
    val dateStr = remember(timestampMillis) { ElamalUtils.formatDate(timestampMillis) }
    val timeStr = remember(timestampMillis) { ElamalUtils.formatTime(timestampMillis) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label ($dayName)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (showQuickDays) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nowCal = Calendar.getInstance()
                                    val targetCal = Calendar.getInstance().apply {
                                        timeInMillis = timestampMillis
                                        set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
                                        set(Calendar.MONTH, nowCal.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH))
                                    }
                                    onTimestampChange(targetCal.timeInMillis)
                                }
                        ) {
                            Text(
                                text = "اليوم",
                                fontSize = 10.5f.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                    val targetCal = Calendar.getInstance().apply {
                                        timeInMillis = timestampMillis
                                        set(Calendar.YEAR, yestCal.get(Calendar.YEAR))
                                        set(Calendar.MONTH, yestCal.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, yestCal.get(Calendar.DAY_OF_MONTH))
                                    }
                                    onTimestampChange(targetCal.timeInMillis)
                                }
                        ) {
                            Text(
                                text = "أمس",
                                fontSize = 10.5f.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "اختيار التاريخ",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = dateStr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .weight(0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showTimePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "اختيار الوقت",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = timeStr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5f.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestampMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                timeInMillis = timestampMillis
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            onTimestampChange(localCal.timeInMillis)
                        }
                        showDatePicker = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("تأكيد التاريخ") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = remember(timestampMillis) {
            Calendar.getInstance().apply { timeInMillis = timestampMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { useKeyboardTimeInput = !useKeyboardTimeInput }) {
                        Icon(
                            imageVector = if (useKeyboardTimeInput) Icons.Default.Schedule else Icons.Default.Keyboard,
                            contentDescription = "تبديل طريقة الإدخال",
                            tint = accentColor
                        )
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        if (useKeyboardTimeInput) {
                            TimeInput(state = timePickerState)
                        } else {
                            TimePicker(state = timePickerState)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = timestampMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onTimestampChange(updatedCal.timeInMillis)
                        showTimePicker = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("تأكيد الوقت") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("إلغاء") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOnlyPickerButton(
    label: String,
    dateMillis: Long,
    accentColor: Color,
    showQuickDays: Boolean = false,
    onDateChange: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dayName = remember(dateMillis) { ElamalUtils.getArabicDayName(dateMillis) }
    val dateStr = remember(dateMillis) { ElamalUtils.formatDate(dateMillis) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$label ($dayName)", fontSize = 12.5f.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(text = dateStr, fontWeight = FontWeight.ExtraBold, fontSize = 13.5f.sp)
                    }
                    Icon(Icons.Default.CalendarMonth, contentDescription = label, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showQuickDays) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDateChange(System.currentTimeMillis()) }
                ) {
                    Text(
                        text = "اليوم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                            onDateChange(yest)
                        }
                ) {
                    Text(
                        text = "أمس",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val nowCal = Calendar.getInstance()
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDateChange(localCal.timeInMillis)
                        }
                        showDatePicker = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppState.AppDialogs() {
    val context = LocalContext.current

    // 1. Clock-Out Dialog (With >16h / >24h protection & editable end time)
    if (showClockOutDialog) {
        var breakInput by rememberSaveable { mutableStateOf("0") }
        var noteInput by rememberSaveable { mutableStateOf("") }
        val rawElapsedMins = if (activeClockIn > 0L) ((System.currentTimeMillis() - activeClockIn) / 60000L).coerceAtLeast(0L) else 0L
        val isOver24h = rawElapsedMins > 24 * 60L
        val isOver16h = rawElapsedMins > 16 * 60L

        var customEndMillis by rememberSaveable(activeClockIn) {
            val defaultEnd = if (isOver24h) activeClockIn + 8 * 3600_000L else System.currentTimeMillis()
            mutableLongStateOf(defaultEnd)
        }

        val bMinPreview = breakInput.toIntOrNull() ?: 0
        val effectiveMins = (((customEndMillis - activeClockIn) / 60000L) - bMinPreview).coerceAtLeast(0L)

        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تسجيل الخروج وإنهاء الجلسة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isOver16h) {
                        Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isOver24h) "⚠️ مضى أكثر من 24 ساعة على الدخول! تم ضبط وقت خروج مقترح (8 ساعات)، يرجى مراجعته وتعديله."
                                else "⚠️ مضى وقت طويل على الدخول (${ElamalUtils.formatDurationArabic(rawElapsedMins)}). تأكد من وقت الخروج.",
                                color = Color(0xFFD97706),
                                fontSize = 11.5f.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    DateTimePickerSection(
                        label = "وقت الخروج الفعلي",
                        timestampMillis = customEndMillis,
                        accentColor = primaryCustom,
                        onTimestampChange = { customEndMillis = it }
                    )

                    Text("المدة الصافية المحسوبة: ${ElamalUtils.formatDurationArabic(effectiveMins)}", fontWeight = FontWeight.Bold, color = primaryCustom)

                    QuickBreakField(
                        breakInput = breakInput,
                        accentColor = primaryCustom,
                        onBreakChange = { breakInput = it }
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("ملاحظة الجلسة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bMin = breakInput.toIntOrNull() ?: 0
                        completeClockOut(bMin, noteInput, customEndMillis)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ وإنهاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 2. Edit Active Clock-In Start Dialog
    if (showEditActiveClockInDialog) {
        var startMillis by rememberSaveable(activeClockIn) { mutableLongStateOf(activeClockIn) }
        AlertDialog(
            onDismissRequest = { showEditActiveClockInDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تعديل وقت بداية الجلسة الجارية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateTimePickerSection(
                        label = "وقت بداية الجلسة",
                        timestampMillis = startMillis,
                        accentColor = primaryCustom,
                        onTimestampChange = { startMillis = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (startMillis <= System.currentTimeMillis()) {
                            updateActiveClockInStart(startMillis)
                            showEditActiveClockInDialog = false
                        } else {
                            showNotice("⚠️ لا يمكن أن يكون وقت البداية في المستقبل!")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تحديث") }
            },
            dismissButton = {
                TextButton(onClick = { showEditActiveClockInDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 3. Manual Hourly Entry Dialog (Supports prefillDialogDateMillis from Calendar)
    if (showManualHourlyDialog) {
        val baseDayMillis = prefillDialogDateMillis ?: System.currentTimeMillis()
        var startMillis by rememberSaveable(baseDayMillis) {
            val c = Calendar.getInstance().apply {
                timeInMillis = baseDayMillis
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            mutableLongStateOf(c.timeInMillis)
        }
        var endMillis by rememberSaveable(baseDayMillis) {
            val c = Calendar.getInstance().apply {
                timeInMillis = baseDayMillis
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            mutableLongStateOf(c.timeInMillis)
        }
        var breakInput by rememberSaveable { mutableStateOf("0") }
        var noteInput by rememberSaveable { mutableStateOf("") }

        val bMinPreview = breakInput.toIntOrNull() ?: 0
        val previewNetMins = (((endMillis - startMillis) / 60000L) - bMinPreview).coerceAtLeast(0L)
        val previewPay = if (endMillis > startMillis) {
            ElamalUtils.calculateHourlyPay(startMillis, endMillis, bMinPreview, hourlyRate)
        } else 0.0

        AlertDialog(
            onDismissRequest = {
                showManualHourlyDialog = false
                prefillDialogDateMillis = null
            },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة سجل ساعات يدوي", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateTimePickerSection(
                        label = "وقت البداية",
                        timestampMillis = startMillis,
                        accentColor = primaryCustom,
                        onTimestampChange = { newStart ->
                            startMillis = newStart
                            if (endMillis <= newStart) {
                                val sCal = Calendar.getInstance().apply { timeInMillis = newStart }
                                val eCal = Calendar.getInstance().apply {
                                    timeInMillis = endMillis
                                    set(Calendar.YEAR, sCal.get(Calendar.YEAR))
                                    set(Calendar.MONTH, sCal.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, sCal.get(Calendar.DAY_OF_MONTH))
                                }
                                endMillis = if (eCal.timeInMillis > newStart) eCal.timeInMillis else newStart + 8 * 3600_000L
                            }
                        }
                    )

                    DateTimePickerSection(
                        label = "وقت النهاية",
                        timestampMillis = endMillis,
                        accentColor = primaryCustom,
                        onTimestampChange = { endMillis = it }
                    )

                    if (endMillis > startMillis) {
                        Surface(
                            color = primaryCustom.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المدة الصافية: ${ElamalUtils.formatDurationArabic(previewNetMins)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryCustom
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = ElamalUtils.formatMoney(previewPay),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryCustom
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "⚠️ وقت النهاية يجب أن يكون بعد وقت البداية",
                            fontSize = 11.5f.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }

                    QuickBreakField(
                        breakInput = breakInput,
                        accentColor = primaryCustom,
                        onBreakChange = { breakInput = it }
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("ملاحظة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bMin = breakInput.toIntOrNull() ?: 0
                        if (endMillis > startMillis) {
                            saveManualHourlyEntry(startMillis, endMillis, bMin, noteInput, false)
                        } else {
                            showNotice("⚠️ وقت النهاية يجب أن يكون بعد وقت البداية.")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualHourlyDialog = false
                    prefillDialogDateMillis = null
                }) { Text("إلغاء") }
            }
        )
    }

    // 4. Remote Entry Dialog (Supports prefillDialogDateMillis)
    if (showRemoteDialog) {
        val initDate = prefillDialogDateMillis ?: System.currentTimeMillis()
        var amountInput by rememberSaveable { mutableStateOf("") }
        var noteInput by rememberSaveable { mutableStateOf("") }
        var entryTimestamp by rememberSaveable(initDate) { mutableLongStateOf(initDate) }

        AlertDialog(
            onDismissRequest = {
                showRemoteDialog = false
                prefillDialogDateMillis = null
            },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة عمل عن بعد (مبلغ ثابت)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateOnlyPickerButton(
                        label = "تاريخ العملية",
                        dateMillis = entryTimestamp,
                        accentColor = Color(0xFF2563EB),
                        showQuickDays = true,
                        onDateChange = { entryTimestamp = it }
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amountInput = it },
                        label = { Text("المبلغ ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("ملاحظة / وصف العمل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            saveFixedEntry(EntryType.REMOTE, amt, noteInput, entryTimestamp)
                        } else {
                            showNotice("⚠️ أدخل مبلغاً صحيحاً أكبر من صفر")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة للدفتر") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemoteDialog = false
                    prefillDialogDateMillis = null
                }) { Text("إلغاء") }
            }
        )
    }

    // 5. Credit Dialog (Supports prefillDialogDateMillis)
    if (showCreditDialog) {
        val initDate = prefillDialogDateMillis ?: System.currentTimeMillis()
        var amountInput by rememberSaveable { mutableStateOf("") }
        var noteInput by rememberSaveable { mutableStateOf("") }
        var entryTimestamp by rememberSaveable(initDate) { mutableLongStateOf(initDate) }

        AlertDialog(
            onDismissRequest = {
                showCreditDialog = false
                prefillDialogDateMillis = null
            },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة سلفة أو خصم (كريدي -)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateOnlyPickerButton(
                        label = "تاريخ السلفة / الخصم",
                        dateMillis = entryTimestamp,
                        accentColor = Color(0xFFDC2626),
                        showQuickDays = true,
                        onDateChange = { entryTimestamp = it }
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amountInput = it },
                        label = { Text("المبلغ المخصوم ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("سبب الخصم أو السلفة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            saveFixedEntry(EntryType.CREDIT, -amt, noteInput, entryTimestamp)
                        } else {
                            showNotice("⚠️ أدخل مبلغاً صحيحاً أكبر من صفر")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("خصم من الرصيد") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreditDialog = false
                    prefillDialogDateMillis = null
                }) { Text("إلغاء") }
            }
        )
    }

    // 6. Settlement Dialog
    if (showSettleDialog) {
        val prevCarry = settlements.firstOrNull()?.nextCarryOver ?: 0.0
        val entriesSum = remember(entries) { ElamalUtils.roundMoney(entries.sumOf { it.calculatedAmount }) }
        val netDue = ElamalUtils.roundMoney(entriesSum + prevCarry)
        var paidInput by rememberSaveable { mutableStateOf(if (netDue > 0.0) netDue.toString() else "0") }
        var noteInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("استلام دفعة جديدة وتصفية الحساب", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إجمالي السجلات الحالية: ${ElamalUtils.formatMoney(entriesSum)}")
                    Text("الرصيد المرحّل السابق: ${ElamalUtils.formatMoney(prevCarry)}")
                    Text("الصافي المستحق قبل الدفعة: ${ElamalUtils.formatMoney(netDue)}", fontWeight = FontWeight.Bold, color = primaryCustom)

                    OutlinedTextField(
                        value = paidInput,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) paidInput = it },
                        label = { Text("المبلغ المستلم فعلياً ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("ملاحظة الدفعة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paid = paidInput.toDoubleOrNull() ?: 0.0
                        createNewSettlement(entriesSum, prevCarry, paid, noteInput)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تأكيد الاستلام وأرشفة السجلات") }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 7. Edit Entry Dialog
    val itemToEdit = editingEntry
    if (itemToEdit != null) {
        var startMillis by rememberSaveable(itemToEdit.id) { mutableLongStateOf(itemToEdit.startTime) }
        var endMillis by rememberSaveable(itemToEdit.id) { mutableLongStateOf(itemToEdit.endTime ?: itemToEdit.startTime) }
        var breakStr by rememberSaveable(itemToEdit.id) { mutableStateOf(itemToEdit.breakMinutes.toString()) }
        var amountStr by rememberSaveable(itemToEdit.id) { mutableStateOf(kotlin.math.abs(itemToEdit.calculatedAmount).toString()) }
        var noteStr by rememberSaveable(itemToEdit.id) { mutableStateOf(itemToEdit.note) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تعديل السجل #${ElamalUtils.localizeDigits("${itemToEdit.id}")}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateTimePickerSection(
                        label = if (itemToEdit.type == EntryType.HOURLY) "وقت البداية" else "تاريخ ووقت العملية",
                        timestampMillis = startMillis,
                        accentColor = primaryCustom,
                        onTimestampChange = { startMillis = it }
                    )

                    if (itemToEdit.type == EntryType.HOURLY) {
                        DateTimePickerSection(
                            label = "وقت النهاية",
                            timestampMillis = endMillis,
                            accentColor = primaryCustom,
                            onTimestampChange = { endMillis = it }
                        )
                        QuickBreakField(
                            breakInput = breakStr,
                            accentColor = primaryCustom,
                            onBreakChange = { breakStr = it }
                        )
                    } else {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amountStr = it },
                            label = { Text("المبلغ ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("الملاحظة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bMin = breakStr.toIntOrNull() ?: 0
                        val amt = amountStr.toDoubleOrNull() ?: kotlin.math.abs(itemToEdit.calculatedAmount)
                        if (itemToEdit.type == EntryType.HOURLY && endMillis <= startMillis) {
                            showNotice("⚠️ وقت النهاية يجب أن يكون بعد وقت البداية!")
                        } else {
                            saveEditedEntry(itemToEdit, startMillis, if (itemToEdit.type == EntryType.HOURLY) endMillis else startMillis, bMin, amt, noteStr, false)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ التعديلات") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 7-B. Edit Settlement Dialog
    val settlementToEdit = editingSettlement
    if (settlementToEdit != null) {
        var sTimestamp by rememberSaveable(settlementToEdit.id) { mutableLongStateOf(settlementToEdit.timestamp) }
        var paidStr by rememberSaveable(settlementToEdit.id) { mutableStateOf(settlementToEdit.amountPaid.toString()) }
        var noteStr by rememberSaveable(settlementToEdit.id) { mutableStateOf(settlementToEdit.note) }

        AlertDialog(
            onDismissRequest = { editingSettlement = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تعديل الدفعة المؤرشفة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateTimePickerSection(
                        label = "تاريخ ووقت الدفعة",
                        timestampMillis = sTimestamp,
                        accentColor = primaryCustom,
                        onTimestampChange = { sTimestamp = it }
                    )
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) paidStr = it },
                        label = { Text("المبلغ المستلم فعلياً ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة الدفعة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPaid = paidStr.toDoubleOrNull() ?: settlementToEdit.amountPaid
                        saveEditedSettlement(settlementToEdit, sTimestamp, newPaid, noteStr)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ وإعادة حساب المرحّل") }
            },
            dismissButton = {
                TextButton(onClick = { editingSettlement = null }) { Text("إلغاء") }
            }
        )
    }

    // 8. Delete Confirmation Dialog
    if (pendingDeleteEntry != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("نقل السجل إلى المحذوفات؟", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم نقل هذا السجل إلى سلة المحذوفات المؤقتة ويمكنك استرجاعه لاحقاً.") },
            confirmButton = {
                Button(
                    onClick = { confirmSoftDeleteEntry() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("نقل للمحذوفات") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 9. Trash Bin Dialog
    if (showTrashBinDialog) {
        AlertDialog(
            onDismissRequest = { showTrashBinDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سلة المحذوفات المؤقتة", fontWeight = FontWeight.Bold)
                    if (deletedEntries.isNotEmpty()) {
                        TextButton(onClick = { emptyTrashBinPermanently() }) {
                            Text("إفراغ نهائي", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                if (deletedEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("سلة المحذوفات فارغة حالياً.", color = colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedEntries, key = { "trash_${it.id}" }) { deletedItem ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${deletedItem.type} • ${ElamalUtils.formatMoney(deletedItem.calculatedAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (deletedItem.note.isNotBlank()) {
                                            Text(text = deletedItem.note, fontSize = 11.5f.sp, color = colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { restoreDeletedEntry(deletedItem) }) {
                                            Text("استرجاع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { deleteEntryForever(deletedItem) }) {
                                            Text("حذف نهائي", fontSize = 12.sp, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTrashBinDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إغلاق") }
            }
        )
    }

    // 10. Undo Latest Settlement Dialog
    if (confirmUndoLatestSettlement) {
        AlertDialog(
            onDismissRequest = { confirmUndoLatestSettlement = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("التراجع عن آخر دفعة؟", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف هذه الدفعة وإعادة جميع سجلاتها المرتبطة إلى الدفتر الحالي وإعادة حساب الأرصدة.") },
            confirmButton = {
                Button(
                    onClick = { performConfirmedUndoLatestSettlement() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("تأكيد التراجع عن الدفعة") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUndoLatestSettlement = false }) { Text("إلغاء") }
            }
        )
    }

    // 11. Restore Backup Confirmation Dialog
    val restoreFile = confirmRestoreFileName
    if (restoreFile != null) {
        AlertDialog(
            onDismissRequest = { confirmRestoreFileName = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم استبدال البيانات الحالية محلياً بمحتوى النسخة ($restoreFile). سيتم عمل نسخة أمان تلقائية قبل البدء.") },
            confirmButton = {
                Button(
                    onClick = { performConfirmedRestore(restoreFile) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("بدء الاستعادة") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreFileName = null }) { Text("إلغاء") }
            }
        )
    }

    // 12. Ignore All Pending Wifi Dialog
    if (confirmIgnoreAllWifi) {
        AlertDialog(
            onDismissRequest = { confirmIgnoreAllWifi = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تجاهل كل فترات الواي فاي المعلقة؟", fontWeight = FontWeight.Bold) },
            text = { Text("لن يتم اعتماد هذه الفترات في الدفتر.") },
            confirmButton = {
                Button(
                    onClick = { ignoreAllPendingWifiSessions() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("تجاهل الكل") }
            },
            dismissButton = {
                TextButton(onClick = { confirmIgnoreAllWifi = false }) { Text("إلغاء") }
            }
        )
    }

    // 13. Custom Period PDF Dialog
    if (showCustomPeriodPdfDialog) {
        var startMillis by rememberSaveable { mutableLongStateOf(customReportStartMillis) }
        var endMillis by rememberSaveable { mutableLongStateOf(customReportEndMillis) }

        AlertDialog(
            onDismissRequest = { showCustomPeriodPdfDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("اختر فترة التقرير المخصصة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateOnlyPickerButton(
                        label = "من تاريخ",
                        dateMillis = startMillis,
                        accentColor = primaryCustom,
                        onDateChange = { startMillis = it }
                    )
                    DateOnlyPickerButton(
                        label = "إلى تاريخ",
                        dateMillis = endMillis,
                        accentColor = primaryCustom,
                        onDateChange = { endMillis = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startOfDayMillis = Calendar.getInstance().apply {
                            timeInMillis = startMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val endOfDayMillis = Calendar.getInstance().apply {
                            timeInMillis = endMillis
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        if (endOfDayMillis >= startOfDayMillis) {
                            customReportStartMillis = startOfDayMillis
                            customReportEndMillis = endOfDayMillis
                            selectedReportScope = 5
                            showCustomPeriodPdfDialog = false
                        } else {
                            showNotice("⚠️ تاريخ النهاية يجب أن يكون بعد أو يساوي تاريخ البداية")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("عرض التقرير") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPeriodPdfDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 14. Interactive HSV Color Picker Dialog (With Live Preview + Hue/Saturation/Value Sliders + Hex)
    if (showCustomColorPickerDialog) {
        val initialIntColor = remember(appColorHex) {
            runCatching { android.graphics.Color.parseColor(appColorHex) }
                .getOrDefault(android.graphics.Color.parseColor("#059669"))
        }
        val initialHsv = remember(initialIntColor) {
            FloatArray(3).also { android.graphics.Color.colorToHSV(initialIntColor, it) }
        }

        var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
        var sat by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
        var valueBri by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
        var hexInput by rememberSaveable { mutableStateOf(appColorHex.removePrefix("#")) }

        val currentColorInt = remember(hue, sat, valueBri) {
            android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valueBri))
        }
        val currentPreviewColor = Color(currentColorInt)

        fun syncHexFromHsv(h: Float, s: Float, v: Float) {
            val cInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
            hexInput = String.format(Locale.US, "%06X", 0xFFFFFF and cInt)
        }

        AlertDialog(
            onDismissRequest = { showCustomColorPickerDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("منتقي اللون المخصص", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentPreviewColor)
                            .border(1.dp, colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "#${hexInput.uppercase()}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Text("درجة اللون (Hue): ${hue.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            syncHexFromHsv(hue, sat, valueBri)
                        },
                        valueRange = 0f..360f
                    )

                    Text("التشبّع (Saturation): ${(sat * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = sat,
                        onValueChange = {
                            sat = it
                            syncHexFromHsv(hue, sat, valueBri)
                        },
                        valueRange = 0f..1f
                    )

                    Text("السطوع (Brightness): ${(valueBri * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = valueBri,
                        onValueChange = {
                            valueBri = it
                            syncHexFromHsv(hue, sat, valueBri)
                        },
                        valueRange = 0.15f..1f
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase()
                                hexInput = filtered
                                if (filtered.length == 6) {
                                    runCatching {
                                        val parsed = android.graphics.Color.parseColor("#$filtered")
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(parsed, hsv)
                                        hue = hsv[0]
                                        sat = hsv[1]
                                        valueBri = hsv[2]
                                    }
                                }
                            },
                            label = { Text("HEX") },
                            prefix = { Text("#") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val candidate = if (hexInput.length == 6) "#$hexInput" else String.format(Locale.US, "#%06X", 0xFFFFFF and currentColorInt)
                        val ok = runCatching { android.graphics.Color.parseColor(candidate) }.isSuccess
                        if (ok) {
                            updateAppColor(candidate)
                            showCustomColorPickerDialog = false
                        } else {
                            showNotice("⚠️ كود لون غير صالح")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تطبيق اللون") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomColorPickerDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 15. PIN Setup Dialog
    if (showPinSetupDialog) {
        var oldPin by rememberSaveable { mutableStateOf("") }
        var newPin by rememberSaveable { mutableStateOf("") }
        var confirmPin by rememberSaveable { mutableStateOf("") }
        var isCheckingPin by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إدارة رمز قفل التطبيق", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (savedPin.isNotBlank()) {
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) oldPin = it },
                            label = { Text("الرمز الحالي (مطلوب للتعديل أو الإلغاء)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Text(
                        if (savedPin.isBlank()) "أدخل رمز قفل جديد (من 4 إلى 8 أرقام):"
                        else "أدخل رمزاً جديداً (4 إلى 8 أرقام) أو اتركه فارغاً لإلغاء القفل:",
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text("الرمز الجديد") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (newPin.isNotEmpty()) {
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) confirmPin = it },
                            label = { Text("تأكيد الرمز الجديد") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isCheckingPin,
                    onClick = {
                        viewModelScope.launch {
                            isCheckingPin = true
                            if (savedPin.isNotBlank()) {
                                val oldOk = withContext(Dispatchers.Default) {
                                    ElamalSecurity.verifyPin(oldPin, savedPin)
                                }
                                if (!oldOk) {
                                    isCheckingPin = false
                                    showNotice("⚠️ الرمز الحالي غير صحيح!")
                                    return@launch
                                }
                            }
                            if (newPin.isBlank()) {
                                savedPin = ""
                                prefs.edit().remove("security_pin").remove("security_pin_length").apply()
                                isCheckingPin = false
                                showPinSetupDialog = false
                                showNotice("تم إلغاء قفل التطبيق ✓")
                            } else if (newPin.length in 4..8) {
                                if (newPin != confirmPin) {
                                    isCheckingPin = false
                                    showNotice("⚠️ تأكيد الرمز الجديد غير مطابق!")
                                    return@launch
                                }
                                val hashed = withContext(Dispatchers.Default) { ElamalSecurity.hashPinPbkdf2(newPin) }
                                savedPin = hashed
                                prefs.edit()
                                    .putString("security_pin", hashed)
                                    .putInt("security_pin_length", newPin.length)
                                    .apply()
                                isCheckingPin = false
                                showPinSetupDialog = false
                                showNotice("تم حفظ رمز القفل بنجاح ✓")
                            } else {
                                isCheckingPin = false
                                showNotice("⚠️ يجب أن يكون الرمز بين 4 و 8 أرقام")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ الرمز") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 16. Overlap Warning Dialog
    val overlapMsg = overlapWarningMessage
    if (overlapMsg != null) {
        AlertDialog(
            onDismissRequest = {
                overlapWarningMessage = null
                pendingForcedAction = null
            },
            shape = RoundedCornerShape(22.dp),
            title = { Text("⚠️ تداخل زمني في السجلات", fontWeight = FontWeight.Bold, color = Color(0xFFD97706)) },
            text = { Text(overlapMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingForcedAction
                        overlapWarningMessage = null
                        pendingForcedAction = null
                        action?.invoke()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) { Text("حفظ رغم التداخل") }
            },
            dismissButton = {
                TextButton(onClick = {
                    overlapWarningMessage = null
                    pendingForcedAction = null
                }) { Text("إلغاء") }
            }
        )
    }

    // 17. Unified Share Options Dialog (WhatsApp Text / PDF / Copy to Clipboard + Details Toggle)
    val sharePayload = pendingSharePayload
    if (sharePayload != null) {
        AlertDialog(
            onDismissRequest = { pendingSharePayload = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text(sharePayload.title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تضمين تفاصيل السجلات والأيام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (shareIncludeDetails) "يرسل الخلاصة مع جدول الأيام بالتفصيل" else "يرسل الخلاصة والمجموع الصافي فقط",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = shareIncludeDetails,
                                onCheckedChange = { updateShareIncludeDetails(it) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val txt = ElamalUtils.buildWhatsappFormattedText(
                                    entries = sharePayload.entries,
                                    totalNet = sharePayload.netTotal,
                                    carryOver = sharePayload.carryOver,
                                    title = sharePayload.title,
                                    includeDetails = shareIncludeDetails
                                )
                                pendingSharePayload = null
                                sharePlainTextIntent(context, txt, sharePayload.title)
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة نص", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة نص", fontWeight = FontWeight.Bold, fontSize = 12.5f.sp)
                        }

                        Button(
                            onClick = {
                                val withDet = shareIncludeDetails
                                pendingSharePayload = null
                                exportPdfReportAsync(
                                    title = sharePayload.title,
                                    reportEntries = sharePayload.entries,
                                    reportSettlements = sharePayload.settlements,
                                    carryOver = sharePayload.carryOver,
                                    includeDetails = withDet
                                )
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "مشاركة PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تقرير PDF", fontWeight = FontWeight.Bold, fontSize = 12.5f.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val txt = ElamalUtils.buildWhatsappFormattedText(
                                entries = sharePayload.entries,
                                totalNet = sharePayload.netTotal,
                                carryOver = sharePayload.carryOver,
                                title = sharePayload.title,
                                includeDetails = shareIncludeDetails
                            )
                            pendingSharePayload = null
                            copyTextToClipboard(txt)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ النص", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ النص إلى الحافظة", fontWeight = FontWeight.Bold, fontSize = 12.5f.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingSharePayload = null }) { Text("إلغاء") }
            }
        )
    }
}
