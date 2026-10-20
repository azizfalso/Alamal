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
fun AppState.AppDialogs() {
        // Custom Date Range PDF Dialog
        if (showCustomPeriodPdfDialog) {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            var fromDateStr by remember { mutableStateOf(df.format(Date(System.currentTimeMillis() - 14 * 86400_000L))) }
            var toDateStr by remember { mutableStateOf(df.format(Date())) }

            AlertDialog(
                onDismissRequest = { showCustomPeriodPdfDialog = false },
                title = { Text("تقرير PDF لفترة مخصصة") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fromDateStr, onValueChange = { fromDateStr = it }, label = { Text("من تاريخ (yyyy-MM-dd)") })
                        OutlinedTextField(value = toDateStr, onValueChange = { toDateStr = it }, label = { Text("إلى تاريخ (yyyy-MM-dd)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val startT = runCatching { df.parse(fromDateStr)?.time }.getOrNull() ?: 0L
                        val endT = (runCatching { df.parse(toDateStr)?.time }.getOrNull() ?: System.currentTimeMillis()) + 86399_000L
                        val rangeEntries = allEntries.filter { it.startTime in startT..endT }
                        PdfReportGenerator.generateAndSharePdf(context, "فترة $fromDateStr إلى $toDateStr", rangeEntries, settlements, isDark)
                        showCustomPeriodPdfDialog = false
                    }) { Text("تصدير ومشاركة PDF") }
                }
            )
        }

        // Edit Existing Entry Dialog
        if (editingEntry != null) {
            val target = editingEntry!!
            var noteStr by remember { mutableStateOf(target.note) }
            var breakStr by remember { mutableStateOf(target.breakMinutes.toString()) }
            var amountStr by remember { mutableStateOf(kotlin.math.abs(target.calculatedAmount).toString()) }

            AlertDialog(
                onDismissRequest = { editingEntry = null },
                title = { Text("تعديل السجل") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (target.type == EntryType.HOURLY) {
                            OutlinedTextField(
                                value = breakStr,
                                onValueChange = { breakStr = it },
                                label = { Text("الاستراحة (دقائق)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        } else {
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("المبلغ (DH)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        OutlinedTextField(value = noteStr, onValueChange = { noteStr = it }, label = { Text("الملاحظة") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            val updated = if (target.type == EntryType.HOURLY && target.endTime != null) {
                                val brk = breakStr.toIntOrNull() ?: target.breakMinutes
                                val recalc = ElamalUtils.calculateHourlyPay(target.startTime, target.endTime, brk, target.hourlyRateAtEntry)
                                target.copy(breakMinutes = brk, calculatedAmount = recalc, note = noteStr)
                            } else {
                                val rawAmt = kotlin.math.abs(amountStr.toDoubleOrNull() ?: target.calculatedAmount)
                                val signedAmt = if (target.type == EntryType.CREDIT) -rawAmt else rawAmt
                                target.copy(calculatedAmount = signedAmt, note = noteStr)
                            }
                            dao.updateEntry(updated)
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            editingEntry = null
                        }
                    }) { Text("حفظ التعديل") }
                }
            )
        }

        if (confirmIgnoreAllWifi) {
            AlertDialog(
                onDismissRequest = { confirmIgnoreAllWifi = false },
                title = { Text("تأكيد تجاهل الكل") },
                text = { Text("هل أنت متأكد من تجاهل جميع فترات الواي فاي المقترحة؟") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            dao.ignoreAllPendingWifi()
                            confirmIgnoreAllWifi = false
                        }
                    }) { Text("نعم، تجاهل الكل") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmIgnoreAllWifi = false }) { Text("إلغاء") }
                }
            )
        }

        if (editingSettlement != null) {
            val target = editingSettlement!!
            var paidInput by remember { mutableStateOf(target.amountPaid.toString()) }
            var noteInput by remember { mutableStateOf(target.note) }
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            var dateInput by remember { mutableStateOf(df.format(Date(target.timestamp))) }

            AlertDialog(
                onDismissRequest = { editingSettlement = null },
                title = { Text("تعديل دفعة قديمة (مع تحديث المرحّل لاحقاً)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("التاريخ (yyyy-MM-dd)") })
                        OutlinedTextField(
                            value = paidInput,
                            onValueChange = { paidInput = it },
                            label = { Text("المبلغ المستلم (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(value = noteInput, onValueChange = { noteInput = it }, label = { Text("الملاحظة") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val newPaid = paidInput.toDoubleOrNull() ?: target.amountPaid
                        val parsedTime = runCatching { df.parse(dateInput)?.time }.getOrNull() ?: target.timestamp
                        scope.launch {
                            dao.updateSettlement(target.copy(timestamp = parsedTime, amountPaid = newPaid, note = noteInput))
                            dao.recalculateSettlementsCascade()
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            editingSettlement = null
                        }
                    }) { Text("حفظ وإعادة حساب المرحّل") }
                }
            )
        }

        if (showClockOutDialog) {
            var breakMinStr by remember { mutableStateOf("0") }
            var noteStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showClockOutDialog = false },
                title = { Text("تسجيل الخروج") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = breakMinStr,
                            onValueChange = { breakMinStr = it },
                            label = { Text("مدة الاستراحة (بالدقائق)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(value = noteStr, onValueChange = { noteStr = it }, label = { Text("ملاحظة (اختياري)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val end = System.currentTimeMillis()
                        val brk = breakMinStr.toIntOrNull() ?: 0
                        val pay = ElamalUtils.calculateHourlyPay(activeClockIn, end, brk, hourlyRate)
                        scope.launch {
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
                            ElamalWidgetReceiver.refreshAllWidgets(context)
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            showClockOutDialog = false
                        }
                    }) { Text("حفظ الخروج") }
                }
            )
        }

        if (showRemoteDialog) {
            var amountStr by remember { mutableStateOf("") }
            var noteStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showRemoteDialog = false },
                title = { Text("إضافة عمل عن بعد (أجرة ثابتة)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("المبلغ الثابت (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(value = noteStr, onValueChange = { noteStr = it }, label = { Text("ملاحظة") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            val now = System.currentTimeMillis()
                            scope.launch {
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
                    }) { Text("إضافة") }
                }
            )
        }

        if (showCreditDialog) {
            var creditStr by remember { mutableStateOf("") }
            var noteStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreditDialog = false },
                title = { Text("إضافة كريدي (يُسجل بالسالب)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditStr,
                            onValueChange = { creditStr = it },
                            label = { Text("قيمة الكريدي (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(value = noteStr, onValueChange = { noteStr = it }, label = { Text("ملاحظة") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val amt = kotlin.math.abs(creditStr.toDoubleOrNull() ?: 0.0)
                        if (amt > 0) {
                            val now = System.currentTimeMillis()
                            scope.launch {
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
                    }) { Text("حفظ بالسالب") }
                }
            )
        }

        if (showSettleDialog) {
            val prevCarry = settlements.firstOrNull()?.nextCarryOver ?: 0.0
            val entriesSum = entries.sumOf { it.calculatedAmount }
            val netDue = entriesSum + prevCarry
            var paidStr by remember { mutableStateOf(netDue.toString()) }
            var noteStr by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showSettleDialog = false },
                title = { Text("استلام دفعة وأرشفة السجلات") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المجموع الصافي المستحق: ${ElamalUtils.formatMoney(netDue)}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = paidStr,
                            onValueChange = { paidStr = it },
                            label = { Text("المبلغ المستلم فعلياً (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(value = noteStr, onValueChange = { noteStr = it }, label = { Text("ملاحظة الدفعة") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val paid = paidStr.toDoubleOrNull() ?: 0.0
                        val nextCarry = netDue - paid
                        scope.launch {
                            val sId = dao.insertSettlement(
                                Settlement(
                                    timestamp = System.currentTimeMillis(),
                                    entriesTotal = entriesSum,
                                    previousCarryOver = prevCarry,
                                    amountPaid = paid,
                                    nextCarryOver = nextCarry,
                                    note = noteStr
                                )
                            )
                            dao.attachEntriesToSettlement(sId)
                            dao.recalculateSettlementsCascade()
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            showSettleDialog = false
                        }
                    }) { Text("تأكيد وترحيل الفرق") }
                }
            )
        }
}
