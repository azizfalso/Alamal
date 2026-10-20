package com.nanomks.elamal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.Settlement
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppState.EditSettlementDialog() {
    val context = LocalContext.current
    val dfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    if (editingSettlement != null) {
        val target = editingSettlement!!
        var paidInput by remember(target) { mutableStateOf(target.amountPaid.toString()) }
        var noteInput by remember(target) { mutableStateOf(target.note) }
        var dateInput by remember(target) { mutableStateOf(dfDate.format(Date(target.timestamp))) }

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
                        val newPaid = paidInput.toDoubleOrNull() ?: target.amountPaid
                        val parsedTime = runCatching { dfDate.parse(dateInput)?.time }.getOrNull() ?: target.timestamp
                        viewModelScope.launch {
                            dao.updateSettlement(target.copy(timestamp = parsedTime, amountPaid = newPaid, note = noteInput))
                            dao.recalculateSettlementsCascade()
                            statusBanner = BackupSafManager.performSmartBackup(context, dao)
                            editingSettlement = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("حفظ وإعادة حساب المرحّل") }
            },
            dismissButton = {
                TextButton(onClick = { editingSettlement = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AppState.SettleDialog() {
    val context = LocalContext.current

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
                        val nextCarry = netDue - paid
                        viewModelScope.launch {
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
