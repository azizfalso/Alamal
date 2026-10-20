package com.nanomks.elamal.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppState.ManualHourlyDialog() {
    val context = LocalContext.current
    val dfDateTime = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // Add Manual Past Hourly Session Dialog
    if (showManualHourlyDialog) {
        val now = System.currentTimeMillis()
        var startStr by remember { mutableStateOf(dfDateTime.format(Date(now - 4 * 3600_000L))) }
        var endStr by remember { mutableStateOf(dfDateTime.format(Date(now))) }
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
                        val sTime = runCatching { dfDateTime.parse(startStr)?.time }.getOrNull()
                        val eTime = runCatching { dfDateTime.parse(endStr)?.time }.getOrNull()
                        if (sTime == null || eTime == null || eTime <= sTime) {
                            Toast.makeText(context, "يرجى التأكد من صيغة التاريخ وأن وقت النهاية بعد البداية", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val brk = breakStr.toIntOrNull() ?: 0
                        val pay = ElamalUtils.calculateHourlyPay(sTime, eTime, brk, hourlyRate)
                        viewModelScope.launch {
                            dao.insertEntry(
                                WorkEntry(
                                    type = EntryType.HOURLY,
                                    startTime = sTime,
                                    endTime = eTime,
                                    breakMinutes = brk,
                                    hourlyRateAtEntry = hourlyRate,
                                    calculatedAmount = pay,
                                    note = noteStr
                                )
                            )
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            showManualHourlyDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة السجل") }
            },
            dismissButton = {
                TextButton(onClick = { showManualHourlyDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AppState.EditEntryDialog() {
    val context = LocalContext.current
    val dfDateTime = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // Edit Existing Entry Dialog (Now supports editing Start & End timestamps!)
    if (editingEntry != null) {
        val target = editingEntry!!
        var startStr by remember(target) { mutableStateOf(dfDateTime.format(Date(target.startTime))) }
        var endStr by remember(target) { mutableStateOf(dfDateTime.format(Date(target.endTime ?: target.startTime))) }
        var noteStr by remember(target) { mutableStateOf(target.note) }
        var breakStr by remember(target) { mutableStateOf(target.breakMinutes.toString()) }
        var amountStr by remember(target) { mutableStateOf(kotlin.math.abs(target.calculatedAmount).toString()) }

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
                        val newStart = runCatching { dfDateTime.parse(startStr)?.time }.getOrNull() ?: target.startTime
                        val newEnd = runCatching { dfDateTime.parse(endStr)?.time }.getOrNull() ?: (target.endTime ?: newStart)
                        if (target.type == EntryType.HOURLY && newEnd <= newStart) {
                            Toast.makeText(context, "يجب أن يكون وقت النهاية بعد وقت البداية", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModelScope.launch {
                            val updated = if (target.type == EntryType.HOURLY) {
                                val brk = breakStr.toIntOrNull() ?: target.breakMinutes
                                val recalc = ElamalUtils.calculateHourlyPay(newStart, newEnd, brk, target.hourlyRateAtEntry)
                                target.copy(
                                    startTime = newStart,
                                    endTime = newEnd,
                                    breakMinutes = brk,
                                    calculatedAmount = recalc,
                                    note = noteStr
                                )
                            } else {
                                val rawAmt = kotlin.math.abs(amountStr.toDoubleOrNull() ?: target.calculatedAmount)
                                val signedAmt = if (target.type == EntryType.CREDIT) -rawAmt else rawAmt
                                target.copy(
                                    startTime = newStart,
                                    endTime = newStart,
                                    calculatedAmount = signedAmt,
                                    note = noteStr
                                )
                            }
                            dao.updateEntry(updated)
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            editingEntry = null
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
}
