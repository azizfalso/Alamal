package com.nanomks.elamal.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.ElamalUtils
import java.util.*

private data class ScopedReportMetrics(
    val netSum: Double,
    val effectiveNet: Double,
    val hourlyMinutes: Long,
    val hourlyMoney: Double,
    val remoteMoney: Double,
    val creditMoney: Double,
    val remoteCount: Int,
    val creditCount: Int,
    val byDayMap: Map<String, List<WorkEntry>>,
    val dailyAverageMoney: Double,
    val bestDayKey: String?
)

@Composable
fun AppState.ReportsTab() {
    val context = LocalContext.current
    val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0

    val scopeTitle = when (selectedReportScope) {
        0 -> "الملخص النشط (الدفتر الحالي)"
        1 -> "تقرير آخر 7 أيام (أسبوع)"
        2 -> "تقرير آخر 30 يوماً (شهري)"
        3 -> "تقرير آخر دفعة مستلمة"
        4 -> "أرشيف كل الدفعات السابقة"
        5 -> "فترة (${ElamalUtils.formatDate(customReportStartMillis)} ← ${ElamalUtils.formatDate(customReportEndMillis)})"
        else -> "الملخص النشط"
    }

    val scopedEntries = remember(selectedReportScope, entries, allEntries, settlements, customReportStartMillis, customReportEndMillis) {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 86400_000L
        val monthAgo = now - 30 * 86400_000L
        when (selectedReportScope) {
            0 -> entries
            1 -> allEntries.filter { it.startTime >= weekAgo }
            2 -> allEntries.filter { it.startTime >= monthAgo }
            3 -> {
                val latestS = settlements.firstOrNull()
                if (latestS != null) allEntries.filter { it.settlementId == latestS.id } else emptyList()
            }
            4 -> allEntries.filter { it.settlementId != null }
            5 -> allEntries.filter { it.startTime in customReportStartMillis..customReportEndMillis }
            else -> entries
        }
    }

    // Memoized report calculations (#27 & #39)
    val metrics = remember(scopedEntries, selectedReportScope, lastCarryOver) {
        val netSum = ElamalUtils.roundMoney(scopedEntries.sumOf { it.calculatedAmount })
        val effectiveNet = if (selectedReportScope == 0) ElamalUtils.roundMoney(netSum + lastCarryOver) else netSum
        val hourlyList = scopedEntries.filter { it.type == EntryType.HOURLY }
        val remoteList = scopedEntries.filter { it.type == EntryType.REMOTE }
        val creditList = scopedEntries.filter { it.type == EntryType.CREDIT }

        val hourlyMins = hourlyList.filter { it.endTime != null }.sumOf {
            (((it.endTime!! - it.startTime) / 60000L) - it.breakMinutes).coerceAtLeast(0)
        }
        val hMoney = ElamalUtils.roundMoney(hourlyList.sumOf { it.calculatedAmount })
        val rMoney = ElamalUtils.roundMoney(remoteList.sumOf { it.calculatedAmount })
        val cMoney = ElamalUtils.roundMoney(kotlin.math.abs(creditList.sumOf { it.calculatedAmount }))

        val byDay = scopedEntries.groupBy { ElamalUtils.formatDate(it.startTime) }
        val activeDays = byDay.size.coerceAtLeast(1)
        val dailyAvg = if (scopedEntries.isNotEmpty()) ElamalUtils.roundMoney((hMoney + rMoney) / activeDays) else 0.0
        val bestDay = byDay.maxByOrNull { (_, list) -> list.sumOf { it.calculatedAmount } }?.key

        ScopedReportMetrics(
            netSum = netSum,
            effectiveNet = effectiveNet,
            hourlyMinutes = hourlyMins,
            hourlyMoney = hMoney,
            remoteMoney = rMoney,
            creditMoney = cMoney,
            remoteCount = remoteList.size,
            creditCount = creditList.size,
            byDayMap = byDay,
            dailyAverageMoney = dailyAvg,
            bestDayKey = bestDay
        )
    }

    // Pre-group monthly calendar entries once per month/entries change (#27)
    val monthEntriesByDay = remember(allEntries, calendarYear, calendarMonth) {
        val map = mutableMapOf<Int, MutableList<WorkEntry>>()
        val c = Calendar.getInstance()
        for (e in allEntries) {
            c.timeInMillis = e.startTime
            if (c.get(Calendar.YEAR) == calendarYear && c.get(Calendar.MONTH) == calendarMonth) {
                val d = c.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(d) { mutableListOf() }.add(e)
            }
        }
        map
    }

    var showScopedRecordsList by rememberSaveable { mutableStateOf(true) }
    var visibleRecordsLimit by rememberSaveable(selectedReportScope) { mutableIntStateOf(20) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, primaryCustom.copy(alpha = 0.22f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Analytics, contentDescription = "مركز التقارير", tint = primaryCustom)
                            Text(
                                "مركز الملخصات والتقارير",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (pdfDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable {
                                pdfDarkMode = !pdfDarkMode
                                prefs.edit().putBoolean("pdf_dark_mode", pdfDarkMode).apply()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (pdfDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "تبديل مظهر PDF",
                                    tint = if (pdfDarkMode) Color(0xFFFDE68A) else Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (pdfDarkMode) "PDF: داكن" else "PDF: فاتح",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pdfDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Text(
                        "اختر النطاق لعرض إحصائياته وسجلاته بالأسفل أو تصديره:",
                        fontSize = 11.5f.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScopeOptionButton(
                            title = "الملخص",
                            isSelected = selectedReportScope == 0,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedReportScope = 0 }
                        )
                        ScopeOptionButton(
                            title = "أسبوع",
                            isSelected = selectedReportScope == 1,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedReportScope = 1 }
                        )
                        ScopeOptionButton(
                            title = "شهري",
                            isSelected = selectedReportScope == 2,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedReportScope = 2 }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScopeOptionButton(
                            title = "آخر دفعة",
                            isSelected = selectedReportScope == 3,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedReportScope = 3 }
                        )
                        ScopeOptionButton(
                            title = "الأرشيف",
                            isSelected = selectedReportScope == 4,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedReportScope = 4 }
                        )
                        ScopeOptionButton(
                            title = "فترة مخصصة",
                            isSelected = selectedReportScope == 5,
                            primaryColor = primaryCustom,
                            modifier = Modifier.weight(1f),
                            onClick = { showCustomPeriodPdfDialog = true }
                        )
                    }

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportPdfReportAsync(scopeTitle, scopedEntries, settlements) },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("تصدير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val carry = if (selectedReportScope == 0) lastCarryOver else 0.0
                                val txt = ElamalUtils.buildWhatsappFormattedText(scopedEntries, metrics.effectiveNet, carry)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, txt)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة لواتساب").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة واتساب", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { exportCsvAsync(scopedEntries) },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = "تصدير CSV", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scopeTitle,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5f.sp,
                                color = primaryCustom
                            )
                            Text(
                                text = "عدد السجلات: ${scopedEntries.size} سجل • أيام العمل النشطة: ${metrics.byDayMap.size} يوم",
                                fontSize = 11.5f.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = primaryCustom.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = ElamalUtils.formatMoney(metrics.effectiveNet),
                                    color = primaryCustom,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5f.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RichMetricTile(
                            label = "مجموع الساعات",
                            value = ElamalUtils.formatDurationArabic(metrics.hourlyMinutes),
                            subValue = ElamalUtils.formatMoney(metrics.hourlyMoney),
                            accentColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        RichMetricTile(
                            label = "العمل عن بعد",
                            value = ElamalUtils.formatMoney(metrics.remoteMoney),
                            subValue = "${metrics.remoteCount} عملية",
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RichMetricTile(
                            label = "إجمالي الكريدي",
                            value = "-${ElamalUtils.formatMoney(metrics.creditMoney)}",
                            subValue = "${metrics.creditCount} سلفة",
                            accentColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        RichMetricTile(
                            label = "متوسط الدخل اليومي",
                            value = ElamalUtils.formatMoney(metrics.dailyAverageMoney),
                            subValue = if (metrics.bestDayKey != null) "أعلى يوم: ${metrics.bestDayKey.takeLast(5)}" else "لا يوجد بعد",
                            accentColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val maxVal = maxOf(metrics.hourlyMoney, metrics.remoteMoney, metrics.creditMoney, 10.0)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("التوزيع المالي للنطاق المختار:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Canvas(modifier = Modifier.fillMaxWidth().height(85.dp)) {
                            val w = size.width
                            val h = size.height
                            val barW = w / 5.5f
                            val h1 = maxOf(((metrics.hourlyMoney / maxVal) * (h - 12f)).toFloat(), 10f)
                            val h2 = maxOf(((metrics.remoteMoney / maxVal) * (h - 12f)).toFloat(), 10f)
                            val h3 = maxOf(((metrics.creditMoney / maxVal) * (h - 12f)).toFloat(), 10f)

                            drawRoundRect(Color(0xFF059669), topLeft = Offset(barW * 0.6f, h - h1), size = Size(barW, h1), cornerRadius = CornerRadius(10f, 10f))
                            drawRoundRect(Color(0xFF2563EB), topLeft = Offset(barW * 2.25f, h - h2), size = Size(barW, h2), cornerRadius = CornerRadius(10f, 10f))
                            drawRoundRect(Color(0xFFDC2626), topLeft = Offset(barW * 3.9f, h - h3), size = Size(barW, h3), cornerRadius = CornerRadius(10f, 10f))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showScopedRecordsList = !showScopedRecordsList }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showScopedRecordsList) "إخفاء سجلات هذا النطاق (${scopedEntries.size})" else "استعراض سجلات هذا النطاق (${scopedEntries.size})",
                            fontSize = 12.5f.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryCustom
                        )
                        Icon(
                            if (showScopedRecordsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "تبديل عرض السجلات",
                            tint = primaryCustom
                        )
                    }
                }
            }
        }

        if (showScopedRecordsList && scopedEntries.isNotEmpty()) {
            val pagedList = scopedEntries.take(visibleRecordsLimit)
            items(pagedList, key = { "scope_${it.id}" }) { entryItem ->
                EntryRowCard(
                    item = entryItem,
                    fontScale = fontScale,
                    onEdit = { editingEntry = entryItem },
                    onDelete = { requestDeleteEntry(entryItem) }
                )
            }
            if (scopedEntries.size > visibleRecordsLimit) {
                item {
                    OutlinedButton(
                        onClick = { visibleRecordsLimit += 25 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("عرض المزيد (${scopedEntries.size - visibleRecordsLimit} سجل متبقٍ)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            val firstDayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendarYear)
                set(Calendar.MONTH, calendarMonth)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val todayCal = Calendar.getInstance()
            val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startOffset = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
            val totalCells = startOffset + daysInMonth
            val rowsCount = (totalCells + 6) / 7
            val weekDaysHeader = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
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
                            Icon(Icons.Default.CalendarMonth, contentDescription = "التقويم التفاعلي", tint = primaryCustom)
                            Text(
                                "التقويم التفاعلي (${calendarYear}/${calendarMonth + 1})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalIconButton(
                                onClick = { navigateCalendarMonth(-1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "الشهر السابق", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = { navigateCalendarMonth(1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر التالي", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                    val dayEntries = monthEntriesByDay[dayNum] ?: emptyList()
                                    val daySum = dayEntries.sumOf { it.calculatedAmount }
                                    val isSelected = selectedCalendarDay == dayNum
                                    val isToday = todayCal.get(Calendar.YEAR) == calendarYear &&
                                            todayCal.get(Calendar.MONTH) == calendarMonth &&
                                            todayCal.get(Calendar.DAY_OF_MONTH) == dayNum

                                    val boxBg = when {
                                        daySum > 0 -> Color(0xFF059669)
                                        daySum < 0 -> Color(0xFFDC2626)
                                        dayEntries.isNotEmpty() -> Color(0xFF2563EB)
                                        isDark -> Color(0xFF1E293B)
                                        else -> Color(0xFFE2E8F0)
                                    }
                                    val txtCol = if (daySum != 0.0 || dayEntries.isNotEmpty() || isDark) Color.White else Color(0xFF1E293B)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(boxBg)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.dp, if (dayEntries.isNotEmpty()) Color.White else primaryCustom, RoundedCornerShape(12.dp))
                                                } else if (isToday) {
                                                    Modifier.border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                                                } else Modifier
                                            )
                                            .clickable {
                                                selectedCalendarDay = if (selectedCalendarDay == dayNum) -1 else dayNum
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 12.sp,
                                                lineHeight = 13.sp,
                                                color = txtCol,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                            if (dayEntries.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(1.dp))
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        text = if (daySum > 0) "+${daySum.toInt()}" else "${daySum.toInt()}",
                                                        fontSize = 9.sp,
                                                        lineHeight = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White.copy(alpha = 0.95f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Clip
                                                    )
                                                }
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
                        val selectedList = monthEntriesByDay[selectedCalendarDay] ?: emptyList()
                        val dayTotalNet = ElamalUtils.roundMoney(selectedList.sumOf { it.calculatedAmount })
                        val dayTotalMins = selectedList.filter { it.type == EntryType.HOURLY && it.endTime != null }
                            .sumOf { (((it.endTime!! - it.startTime) / 60000L) - it.breakMinutes).coerceAtLeast(0) }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = primaryCustom.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📅 سجلات يوم $selectedCalendarDay/${calendarMonth + 1}/$calendarYear",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5f.sp,
                                        color = primaryCustom
                                    )
                                    Text(
                                        text = "عدد العمليات: ${selectedList.size} • الساعات: ${ElamalUtils.formatDurationArabic(dayTotalMins)}",
                                        fontSize = 11.5f.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = ElamalUtils.formatMoney(dayTotalNet),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (dayTotalNet < 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedList.isEmpty()) {
                            Surface(
                                color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد سجلات مسجلة في هذا اليوم.",
                                    fontSize = 12.5f.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedList.forEach { dayItem ->
                                    EntryRowCard(
                                        item = dayItem,
                                        fontScale = fontScale,
                                        onEdit = { editingEntry = dayItem },
                                        onDelete = { requestDeleteEntry(dayItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeOptionButton(
    title: String,
    isSelected: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RichMetricTile(
    label: String,
    value: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(value, fontSize = 14.5f.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, maxLines = 1)
            }
            Text(subValue, fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), maxLines = 1)
        }
    }
}
