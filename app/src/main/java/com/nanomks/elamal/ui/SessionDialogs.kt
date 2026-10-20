package com.nanomks.elamal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import kotlinx.coroutines.launch

@Composable
fun AppState.ClockOutDialog() {
    val context = LocalContext.current

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
                        val end = System.currentTimeMillis()
                        val brk = breakMinStr.toIntOrNull() ?: 0
                        val pay = ElamalUtils.calculateHourlyPay(activeClockIn, end, brk, hourlyRate)
                        viewModelScope.launch {
                            dao.insertEntry(
                                WorkEntry(
                                    type = EntryType.HOURLY,
                                    startTime = activeClockIn,
                                    endTime = end,
                                    breakMinutes = brk,
                                    hourlyRateAtEntry = hourlyRate,
                                    calculatedAmount = pay,
                                    note = noteStr
                                )
                            )
                            prefs.edit().putLong("active_clock_in", 0L).apply()
                            activeClockIn = 0L
                            ElamalUtils.cancelForgotClockOutAlarm(context)
                            ElamalUtils.cancelOngoingWorkNotification(context)
                            ElamalWidgetReceiver.refreshAllWidgets(context)
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            showClockOutDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ الخروج") }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AppState.RemoteDialog() {
    val context = LocalContext.current

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
                        if (amt > 0) {
                            val now = System.currentTimeMillis()
                            viewModelScope.launch {
                                dao.insertEntry(
                                    WorkEntry(
                                        type = EntryType.REMOTE,
                                        startTime = now,
                                        endTime = now,
                                        hourlyRateAtEntry = hourlyRate,
                                        calculatedAmount = amt,
                                        note = noteStr
                                    )
                                )
                                statusBanner = BackupSafManager.performSmartBackup(context, dao)
                                showRemoteDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AppState.CreditDialog() {
    val context = LocalContext.current

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
                        if (amt > 0) {
                            val now = System.currentTimeMillis()
                            viewModelScope.launch {
                                dao.insertEntry(
                                    WorkEntry(
                                        type = EntryType.CREDIT,
                                        startTime = now,
                                        endTime = now,
                                        hourlyRateAtEntry = hourlyRate,
                                        calculatedAmount = -amt,
                                        note = noteStr
                                    )
                                )
                                statusBanner = BackupSafManager.performSmartBackup(context, dao)
                                showCreditDialog = false
                            }
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
}
