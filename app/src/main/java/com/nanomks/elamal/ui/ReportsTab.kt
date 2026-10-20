package com.nanomks.elamal.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalUtils
import java.util.*

@Composable
fun AppState.ReportsTab() {
    val context = LocalContext.current
    val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0
    val activeSum = entries.sumOf { it.calculatedAmount }
    val totalNet = activeSum + lastCarryOver

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = primaryCustom)
                        Text(
                            "تصدير ومشاركة التقارير (${if (isDark) "نسق داكن" else "نسق فاتح"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    val now = System.currentTimeMillis()
                    val weekAgo = now - 7 * 86400_000L
                    val monthAgo = now - 30 * 86400_000L

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportPdfReportAsync("تقرير أسبوع", allEntries.filter { it.startTime >= weekAgo }, settlements)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("أسبوع") }

                        Button(
                            onClick = {
                                exportPdfReportAsync("تقرير شهري", allEntries.filter { it.startTime >= monthAgo }, settlements)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("شهري") }

                        Button(
                            onClick = {
                                exportPdfReportAsync("الملخص النشط", entries, settlements)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("الملخص") }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val latestS = settlements.firstOrNull()
                                val sEntries = if (latestS != null) allEntries.filter { it.settlementId == latestS.id } else entries
                                exportPdfReportAsync("تقرير آخر دفعة", sEntries, settlements)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("آخر دفعة") }

                        OutlinedButton(
                            onClick = {
                                exportPdfReportAsync("كل الدفعات", allEntries.filter { it.settlementId != null }, settlements)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("الأرشيف") }

                        OutlinedButton(
                            onClick = { showCustomPeriodPdfDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("فترة مخصصة") }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val txt = ElamalUtils.buildWhatsappFormattedText(entries, totalNet, lastCarryOver)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, txt)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة لواتساب").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ملخص واتساب")
                        }

                        OutlinedButton(
                            onClick = { exportCsvAsync(allEntries) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير CSV")
                        }
                    }
                }
            }
        }

        // Visual Breakdown Bar Chart
        item {
            val hourlySum = allEntries.filter { it.type == EntryType.HOURLY }.sumOf { it.calculatedAmount }
            val remoteSum = allEntries.filter { it.type == EntryType.REMOTE }.sumOf { it.calculatedAmount }
            val creditSum = kotlin.math.abs(allEntries.filter { it.type == EntryType.CREDIT }.sumOf { it.calculatedAmount })
            val maxVal = maxOf(hourlySum, remoteSum, creditSum, 10.0)

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("التوزيع المالي الإجمالي (ساعات / عن بعد / كريدي)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                        val w = size.width
                        val h = size.height
                        val barW = w / 5.5f
                        val h1 = maxOf(((hourlySum / maxVal) * (h - 16f)).toFloat(), 12f)
                        val h2 = maxOf(((remoteSum / maxVal) * (h - 16f)).toFloat(), 12f)
                        val h3 = maxOf(((creditSum / maxVal) * (h - 16f)).toFloat(), 12f)

                        drawRoundRect(Color(0xFF059669), topLeft = Offset(barW * 0.6f, h - h1), size = Size(barW, h1), cornerRadius = CornerRadius(12f, 12f))
                        drawRoundRect(Color(0xFF2563EB), topLeft = Offset(barW * 2.25f, h - h2), size = Size(barW, h2), cornerRadius = CornerRadius(12f, 12f))
                        drawRoundRect(Color(0xFFDC2626), topLeft = Offset(barW * 3.9f, h - h3), size = Size(barW, h3), cornerRadius = CornerRadius(12f, 12f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Text("ساعات: ${ElamalUtils.formatMoney(hourlySum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        Text("عن بعد: ${ElamalUtils.formatMoney(remoteSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Text("كريدي: -${ElamalUtils.formatMoney(creditSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        // Interactive Monthly Calendar with Month Navigation & True Weekday Alignment
        item {
            val firstDayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendarYear)
                set(Calendar.MONTH, calendarMonth)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startOffset = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
            val totalCells = startOffset + daysInMonth
            val rowsCount = (totalCells + 6) / 7
            val weekDaysHeader = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = primaryCustom)
                            Text("التقويم التفاعلي (${calendarYear}/${calendarMonth + 1})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = { navigateCalendarMonth(-1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "الشهر السابق", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = { navigateCalendarMonth(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر التالي", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekday Names Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        weekDaysHeader.forEach { dName ->
                            Text(
                                text = dName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    for (weekRow in 0 until rowsCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = weekRow * 7 + col
                                val dayNum = cellIndex - startOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val dayEntries = allEntries.filter {
                                        val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                                        c.get(Calendar.YEAR) == calendarYear &&
                                                c.get(Calendar.MONTH) == calendarMonth &&
                                                c.get(Calendar.DAY_OF_MONTH) == dayNum
                                    }
                                    val daySum = dayEntries.sumOf { it.calculatedAmount }
                                    val isSelected = selectedCalendarDay == dayNum

                                    val boxBg = when {
                                        daySum > 0 -> Color(0xFF059669)
                                        daySum < 0 -> Color(0xFFDC2626)
                                        isDark -> Color(0xFF1E293B)
                                        else -> Color(0xFFE2E8F0)
                                    }
                                    val txtCol = if (daySum != 0.0 || isDark) Color.White else Color(0xFF1E293B)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) primaryCustom.copy(alpha = 0.4f) else boxBg)
                                            .clickable {
                                                selectedCalendarDay = if (selectedCalendarDay == dayNum) -1 else dayNum
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$dayNum", fontSize = 12.sp, color = txtCol, fontWeight = FontWeight.Bold)
                                            if (daySum != 0.0) {
                                                Text("${daySum.toInt()}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.92f))
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (selectedCalendarDay in 1..daysInMonth) {
                        val selectedList = allEntries.filter {
                            val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                            c.get(Calendar.YEAR) == calendarYear &&
                                    c.get(Calendar.MONTH) == calendarMonth &&
                                    c.get(Calendar.DAY_OF_MONTH) == selectedCalendarDay
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "سجلات يوم $selectedCalendarDay/${calendarMonth + 1}/${calendarYear} (${selectedList.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryCustom
                        )
                        if (selectedList.isEmpty()) {
                            Text("لا توجد سجلات في هذا اليوم.", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        } else {
                            selectedList.forEach { item ->
                                Text(
                                    text = "• ${ElamalUtils.formatMoney(item.calculatedAmount)} ${if (item.note.isNotBlank()) "- ${item.note}" else ""}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



