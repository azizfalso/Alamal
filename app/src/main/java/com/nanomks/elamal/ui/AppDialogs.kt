package com.nanomks.elamal.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalUtils
import java.util.*

@Composable
fun AppState.AppDialogs() {
    val context = LocalContext.current

    // Overlap Warning Confirmation Dialog
    if (overlapWarningMessage != null) {
        AlertDialog(
            onDismissRequest = {
                overlapWarningMessage = null
                pendingForcedAction = null
            },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706)) },
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
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = primaryCustom) },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("سيتم حفظ نسخة أمان من بياناتك الحالية أولاً، ثم استبدال السجلات والدفعات بالنسخة:\n($targetFile)\nهل تود المتابعة؟")
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

    // Custom Date Range PDF Dialog
    if (showCustomPeriodPdfDialog) {
        var fromDateStr by remember { mutableStateOf(ElamalUtils.formatDate(System.currentTimeMillis() - 14 * 86400_000L)) }
        var toDateStr by remember { mutableStateOf(ElamalUtils.formatDate(System.currentTimeMillis())) }

        AlertDialog(
            onDismissRequest = { showCustomPeriodPdfDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = primaryCustom) },
            title = { Text("تقرير PDF لفترة مخصصة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromDateStr,
                        onValueChange = { fromDateStr = it },
                        label = { Text("من تاريخ (yyyy-MM-dd)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = toDateStr,
                        onValueChange = { toDateStr = it },
                        label = { Text("إلى تاريخ (yyyy-MM-dd)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startT = ElamalUtils.parseStrictDate(fromDateStr)
                        val endBase = ElamalUtils.parseStrictDate(toDateStr)
                        if (startT == null || endBase == null || endBase < startT) {
                            Toast.makeText(context, "تأكد من صحة التاريخ بالصيغة (yyyy-MM-dd)", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val endT = endBase + 86399_000L
                        val rangeEntries = allEntries.filter { it.startTime in startT..endT }
                        exportPdfReportAsync("فترة $fromDateStr إلى $toDateStr", rangeEntries, settlements)
                        showCustomPeriodPdfDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تصدير ومشاركة PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPeriodPdfDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Add Manual Past Hourly Session Dialog
    if (showManualHourlyDialog) {
        val now = System.currentTimeMillis()
        var startStr by remember { mutableStateOf(ElamalUtils.formatDateTime(now - 4 * 3600_000L)) }
        var endStr by remember { mutableStateOf(ElamalUtils.formatDateTime(now)) }
        var breakStr by remember { mutableStateOf("0") }
        var noteStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualHourlyDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = primaryCustom) },
            title = { Text("إضافة جلسة ساعات يدوية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("وقت البداية (yyyy-MM-dd HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("وقت النهاية (yyyy-MM-dd HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = breakStr,
                        onValueChange = { breakStr = it },
                        label = { Text("الاستراحة (دقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("الملاحظة (اختياري)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sTime = ElamalUtils.parseStrictDateTime(startStr)
                        val eTime = ElamalUtils.parseStrictDateTime(endStr)
                        if (sTime == null || eTime == null || eTime <= sTime) {
                            Toast.makeText(context, "تأكد من صيغة التاريخ الصحيحة وأن النهاية بعد البداية", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val brk = breakStr.toIntOrNull() ?: 0
                        saveManualHourlyEntry(sTime, eTime, brk, noteStr, forceIgnoreOverlap = false)
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
        var startStr by remember(target.id) { mutableStateOf(ElamalUtils.formatDateTime(target.startTime)) }
        var endStr by remember(target.id) { mutableStateOf(ElamalUtils.formatDateTime(target.endTime ?: target.startTime)) }
        var noteStr by remember(target.id) { mutableStateOf(target.note) }
        var breakStr by remember(target.id) { mutableStateOf(target.breakMinutes.toString()) }
        var amountStr by remember(target.id) { mutableStateOf(kotlin.math.abs(target.calculatedAmount).toString()) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = primaryCustom) },
            title = { Text("تعديل السجل وتفاصيل الوقت", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text(if (target.type == EntryType.HOURLY) "وقت البداية (yyyy-MM-dd HH:mm)" else "التاريخ والوقت (yyyy-MM-dd HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (target.type == EntryType.HOURLY) {
                        OutlinedTextField(
                            value = endStr,
                            onValueChange = { endStr = it },
                            label = { Text("وقت النهاية (yyyy-MM-dd HH:mm)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = breakStr,
                            onValueChange = { breakStr = it },
                            label = { Text("الاستراحة (دقائق)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("المبلغ (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("الملاحظة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newStart = ElamalUtils.parseStrictDateTime(startStr)
                        val newEnd = if (target.type == EntryType.HOURLY) ElamalUtils.parseStrictDateTime(endStr) else newStart
                        if (newStart == null || newEnd == null || (target.type == EntryType.HOURLY && newEnd <= newStart)) {
                            Toast.makeText(context, "تأكد من صيغة التاريخ الصحيحة وأن النهاية بعد البداية", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val brk = breakStr.toIntOrNull() ?: target.breakMinutes
                        val rawAmt = kotlin.math.abs(amountStr.toDoubleOrNull() ?: target.calculatedAmount)
                        saveEditedEntry(target, newStart, newEnd, brk, rawAmt, noteStr, forceIgnoreOverlap = false)
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
        var paidInput by remember(target.id) { mutableStateOf(target.amountPaid.toString()) }
        var noteInput by remember(target.id) { mutableStateOf(target.note) }
        var dateInput by remember(target.id) { mutableStateOf(ElamalUtils.formatDate(target.timestamp)) }

        AlertDialog(
            onDismissRequest = { editingSettlement = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("تعديل دفعة قديمة (مع تحديث المرحّل)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("التاريخ (yyyy-MM-dd)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = paidInput,
                        onValueChange = { paidInput = it },
                        label = { Text("المبلغ المستلم (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("الملاحظة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedTime = ElamalUtils.parseStrictDate(dateInput)
                        if (parsedTime == null) {
                            Toast.makeText(context, "صيغة التاريخ غير صحيحة (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newPaid = paidInput.toDoubleOrNull() ?: target.amountPaid
                        saveEditedSettlement(target, parsedTime, newPaid, noteInput)
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
        var breakMinStr by remember { mutableStateOf("0") }
        var noteStr by remember { mutableStateOf("") }
        val elapsedMin = if (activeClockIn > 0L) ((System.currentTimeMillis() - activeClockIn) / 60000L).coerceAtLeast(1L) else 0L

        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("تسجيل الخروج وإنهاء الجلسة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = primaryCustom.copy(alpha = 0.12f),
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
                        onValueChange = { breakMinStr = it },
                        label = { Text("مدة الاستراحة (بالدقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة الجلسة (اختياري)") },
                        singleLine = true,
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

    if (showRemoteDialog) {
        var amountStr by remember { mutableStateOf("") }
        var noteStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRemoteDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إضافة عمل عن بعد (أجرة ثابتة)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ الثابت (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة العمل") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) saveFixedEntry(EntryType.REMOTE, amt, noteStr)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showCreditDialog) {
        var creditStr by remember { mutableStateOf("") }
        var noteStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إضافة كريدي / سلفة (يُخصم من الصافي)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = creditStr,
                        onValueChange = { creditStr = it },
                        label = { Text("قيمة الكريدي (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = kotlin.math.abs(creditStr.toDoubleOrNull() ?: 0.0)
                        if (amt > 0) saveFixedEntry(EntryType.CREDIT, -amt, noteStr)
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
        val entriesSum = entries.sumOf { it.calculatedAmount }
        val netDue = entriesSum + prevCarry
        var paidStr by remember(netDue) { mutableStateOf(String.format(Locale.US, "%.2f", netDue)) }
        var noteStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("استلام دفعة وأرشفة السجلات", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = primaryCustom.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("المجموع الصافي المستحق:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text(ElamalUtils.formatMoney(netDue), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = primaryCustom)
                        }
                    }
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it },
                        label = { Text("المبلغ المستلم فعلياً (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظة الدفعة") },
                        singleLine = true,
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
}



