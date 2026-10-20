package com.nanomks.elamal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppState.AppDialogs() {
    CustomPeriodPdfDialog()
    ManualHourlyDialog()
    EditEntryDialog()
    ConfirmIgnoreWifiDialog()
    EditSettlementDialog()
    ClockOutDialog()
    RemoteDialog()
    CreditDialog()
    SettleDialog()
}

@Composable
fun AppState.CustomPeriodPdfDialog() {
    val context = LocalContext.current
    val dfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // Custom Date Range PDF Dialog
    if (showCustomPeriodPdfDialog) {
        var fromDateStr by remember { mutableStateOf(dfDate.format(Date(System.currentTimeMillis() - 14 * 86400_000L))) }
        var toDateStr by remember { mutableStateOf(dfDate.format(Date())) }

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
                        val startT = runCatching { dfDate.parse(fromDateStr)?.time }.getOrNull() ?: 0L
                        val endT = (runCatching { dfDate.parse(toDateStr)?.time }.getOrNull() ?: System.currentTimeMillis()) + 86399_000L
                        val rangeEntries = allEntries.filter { it.startTime in startT..endT }
                        PdfReportGenerator.generateAndSharePdf(context, "فترة $fromDateStr إلى $toDateStr", rangeEntries, settlements, isDark)
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
}

@Composable
fun AppState.ConfirmIgnoreWifiDialog() {
    if (confirmIgnoreAllWifi) {
        AlertDialog(
            onDismissRequest = { confirmIgnoreAllWifi = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("تأكيد تجاهل الكل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من تجاهل جميع فترات الواي فاي المقترحة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModelScope.launch {
                            dao.ignoreAllPendingWifi()
                            confirmIgnoreAllWifi = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("نعم، تجاهل الكل") }
            },
            dismissButton = {
                TextButton(onClick = { confirmIgnoreAllWifi = false }) { Text("إلغاء") }
            }
        )
    }
}
