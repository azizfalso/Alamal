package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.ElamalSecurity
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppState.AppDialogs() {
    val dfDateTime = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // 1. Clock-Out Dialog
    if (showClockOutDialog) {
        var breakInput by rememberSaveable { mutableStateOf("0") }
        var noteInput by rememberSaveable { mutableStateOf("") }
        val elapsedMins = if (activeClockIn > 0L) ((System.currentTimeMillis() - activeClockIn) / 60000L).coerceAtLeast(0L) else 0L

        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تسجيل الخروج وإنهاء الجلسة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("مدة العمل المسجلة: ${ElamalUtils.formatDurationArabic(elapsedMins)}", fontWeight = FontWeight.Bold, color = primaryCustom)
                    OutlinedTextField(
                        value = breakInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) breakInput = it },
                        label = { Text("وقت الاستراحة (بالدقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                        completeClockOut(bMin, noteInput)
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
        var startStr by rememberSaveable(activeClockIn) { mutableStateOf(dfDateTime.format(Date(activeClockIn))) }
        AlertDialog(
            onDismissRequest = { showEditActiveClockInDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تعديل وقت بداية الجلسة الجارية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("أدخل وقت البداية بالتنسيق (yyyy-MM-dd HH:mm):", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("وقت البداية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedMillis = runCatching { dfDateTime.parse(startStr.trim())?.time }.getOrNull()
                        if (parsedMillis != null && parsedMillis <= System.currentTimeMillis()) {
                            updateActiveClockInStart(parsedMillis)
                            showEditActiveClockInDialog = false
                        } else {
                            showNotice("⚠️ صيغة التاريخ غير صحيحة أو الوقت في المستقبل!")
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

    // 3. Manual Hourly Entry Dialog
    if (showManualHourlyDialog) {
        var startStr by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd 09:00", Locale.US).format(Date())) }
        var endStr by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd 17:00", Locale.US).format(Date())) }
        var breakInput by rememberSaveable { mutableStateOf("0") }
        var noteInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualHourlyDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة سجل ساعات يدوي", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("وقت البداية (yyyy-MM-dd HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("وقت النهاية (yyyy-MM-dd HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = breakInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) breakInput = it },
                        label = { Text("استراحة (دقائق)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                        val sMillis = runCatching { dfDateTime.parse(startStr.trim())?.time }.getOrNull()
                        val eMillis = runCatching { dfDateTime.parse(endStr.trim())?.time }.getOrNull()
                        val bMin = breakInput.toIntOrNull() ?: 0
                        if (sMillis != null && eMillis != null && eMillis > sMillis) {
                            saveManualHourlyEntry(sMillis, eMillis, bMin, noteInput, false)
                        } else {
                            showNotice("⚠️ أوقات غير صالحة؛ تأكد من صيغة التاريخ.")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showManualHourlyDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 4. Remote Entry Dialog
    if (showRemoteDialog) {
        var amountInput by rememberSaveable { mutableStateOf("") }
        var noteInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRemoteDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة عمل عن بعد (مبلغ ثابت)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.matches(Regex("""^\d*\.?\d*$"""))) amountInput = it },
                        label = { Text("المبلغ (DH)") },
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
                            saveFixedEntry(EntryType.REMOTE, amt, noteInput)
                        } else {
                            showNotice("⚠️ أدخل مبلغاً صحيحاً أكبر من صفر")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة للدفتر") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 5. Credit (Advance/Deduction) Dialog
    if (showCreditDialog) {
        var amountInput by rememberSaveable { mutableStateOf("") }
        var noteInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إضافة سلفة أو خصم (كريدي -)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.matches(Regex("""^\d*\.?\d*$"""))) amountInput = it },
                        label = { Text("المبلغ المخصوم (DH)") },
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
                            saveFixedEntry(EntryType.CREDIT, amt, noteInput)
                        } else {
                            showNotice("⚠️ أدخل مبلغاً صحيحاً أكبر من صفر")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("خصم من الرصيد") }
            },
            dismissButton = {
                TextButton(onClick = { showCreditDialog = false }) { Text("إلغاء") }
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
                        onValueChange = { if (it.matches(Regex("""^\d*\.?\d*$"""))) paidInput = it },
                        label = { Text("المبلغ المستلم فعلياً (DH)") },
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
        var startStr by rememberSaveable(itemToEdit.id) { mutableStateOf(dfDateTime.format(Date(itemToEdit.startTime))) }
        var endStr by rememberSaveable(itemToEdit.id) { mutableStateOf(itemToEdit.endTime?.let { dfDateTime.format(Date(it)) } ?: "") }
        var breakStr by rememberSaveable(itemToEdit.id) { mutableStateOf(itemToEdit.breakMinutes.toString()) }
        var amountStr by rememberSaveable(itemToEdit.id) { mutableStateOf(kotlin.math.abs(itemToEdit.calculatedAmount).toString()) }
        var noteStr by rememberSaveable(itemToEdit.id) { mutableStateOf(itemToEdit.note) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تعديل السجل #${ElamalUtils.localizeDigits("${itemToEdit.id}")}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("وقت البداية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (itemToEdit.type == EntryType.HOURLY) {
                        OutlinedTextField(
                            value = endStr,
                            onValueChange = { endStr = it },
                            label = { Text("وقت النهاية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = breakStr,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) breakStr = it },
                            label = { Text("استراحة (دقائق)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.matches(Regex("""^\d*\.?\d*$"""))) amountStr = it },
                            label = { Text("المبلغ (DH)") },
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
                        val sMillis = runCatching { dfDateTime.parse(startStr.trim())?.time }.getOrNull() ?: itemToEdit.startTime
                        val eMillis = if (itemToEdit.type == EntryType.HOURLY) runCatching { dfDateTime.parse(endStr.trim())?.time }.getOrNull() ?: itemToEdit.endTime else sMillis
                        val bMin = breakStr.toIntOrNull() ?: 0
                        val amt = amountStr.toDoubleOrNull() ?: kotlin.math.abs(itemToEdit.calculatedAmount)
                        saveEditedEntry(itemToEdit, sMillis, eMillis ?: sMillis, bMin, amt, noteStr, false)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ التعديلات") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) { Text("إلغاء") }
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
        var startInput by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 14 * 86400_000L))) }
        var endInput by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

        AlertDialog(
            onDismissRequest = { showCustomPeriodPdfDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("اختر فترة التقرير المخصصة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it },
                        label = { Text("من تاريخ (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it },
                        label = { Text("إلى تاريخ (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sMillis = ElamalUtils.parseStrictDate(startInput)
                        val eMillis = ElamalUtils.parseStrictDate(endInput)?.plus(86399_000L)
                        if (sMillis != null && eMillis != null && eMillis >= sMillis) {
                            customReportStartMillis = sMillis
                            customReportEndMillis = eMillis
                            selectedReportScope = 5
                            showCustomPeriodPdfDialog = false
                        } else {
                            showNotice("⚠️ صيغة التاريخ غير صالحة؛ استخدم yyyy-MM-dd")
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

    // 14. Custom Color Picker Dialog
    if (showCustomColorPickerDialog) {
        var hexInput by rememberSaveable { mutableStateOf(appColorHex) }
        AlertDialog(
            onDismissRequest = { showCustomColorPickerDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("اختيار لون مخصص (Hex)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("كود اللون (مثال: #059669)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                        val ok = runCatching { android.graphics.Color.parseColor(clean) }.isSuccess
                        if (ok) {
                            updateAppColor(clean)
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
        var newPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("إدارة رمز قفل التطبيق", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (savedPin.isBlank()) "أدخل رمز قفل جديد (من 4 إلى 8 أرقام):" else "أدخل رمزاً جديداً أو اتركه فارغاً لإلغاء القفل:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text("رمز PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.isBlank()) {
                            savedPin = ""
                            prefs.edit().remove("security_pin").remove("security_pin_length").apply()
                            showPinSetupDialog = false
                            showNotice("تم إلغاء قفل التطبيق ✓")
                        } else if (newPin.length in 4..8) {
                            viewModelScope.launch {
                                val hashed = withContext(Dispatchers.Default) { ElamalSecurity.hashPinPbkdf2(newPin) }
                                savedPin = hashed
                                prefs.edit()
                                    .putString("security_pin", hashed)
                                    .putInt("security_pin_length", newPin.length)
                                    .apply()
                                showPinSetupDialog = false
                                showNotice("تم حفظ رمز القفل بنجاح ✓")
                            }
                        } else {
                            showNotice("⚠️ يجب أن يكون الرمز بين 4 و 8 أرقام")
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
}
