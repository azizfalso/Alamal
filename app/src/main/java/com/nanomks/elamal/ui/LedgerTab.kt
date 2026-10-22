package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.R
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalUtils

@Composable
private fun HeroMiniStatPill(
    label: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = valueText,
                    color = valueColor,
                    fontSize = 11.5f.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.LedgerTab() {
    val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0

    val (activeSum, totalMinutesActive, activeHourlySum, activeRemoteSum, activeCreditSum) = remember(entries, prefsRefreshCounter) {
        val sum = ElamalUtils.roundMoney(entries.sumOf { it.calculatedAmount })
        val mins = entries.filter { it.type == EntryType.HOURLY && it.endTime != null }
            .sumOf { (((it.endTime!! - it.startTime) / 60000L) - it.breakMinutes).coerceAtLeast(0) }
        val hSum = ElamalUtils.roundMoney(entries.filter { it.type == EntryType.HOURLY }.sumOf { it.calculatedAmount })
        val rSum = ElamalUtils.roundMoney(entries.filter { it.type == EntryType.REMOTE }.sumOf { it.calculatedAmount })
        val cSum = ElamalUtils.roundMoney(kotlin.math.abs(entries.filter { it.type == EntryType.CREDIT }.sumOf { it.calculatedAmount }))
        quintuple(sum, mins, hSum, rSum, cSum)
    }

    val totalNet = ElamalUtils.roundMoney(activeSum + lastCarryOver)
    val elapsedActiveMin = if (activeClockIn > 0L) ((nowMillis - activeClockIn) / 60000L).coerceAtLeast(0L) else 0L
    val liveEarnedNow = if (activeClockIn > 0L) ElamalUtils.calculateHourlyPay(activeClockIn, nowMillis, 0, hourlyRate) else 0.0

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = if (activeClockIn > 0L) {
                            listOf(Color(0xFF047857), Color(0xFF064E3B))
                        } else {
                            listOf(primaryCustom, if (isDark) Color(0xFF1E293B) else Color(0xFF0F172A))
                        }
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeClockIn > 0L) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                        }
                        Text(
                            text = if (activeClockIn > 0L) "جلسة جارية (بدأت ${ElamalUtils.formatTime(activeClockIn)})" else stringResource(R.string.net_due_title),
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeClockIn > 0L) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.clickable { showEditActiveClockInDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل وقت البداية", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("تعديل البداية", color = Color.White, fontSize = 10.5f.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "⏱ ${ElamalUtils.formatDurationArabic(totalMinutesActive + elapsedActiveMin)}",
                                color = Color.White,
                                fontSize = 11.5f.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    openShareDialog(
                                        title = "الفترة الحالية (غير مدفوعة)",
                                        targetEntries = entries,
                                        netTotal = totalNet,
                                        carryOver = lastCarryOver
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "مشاركة الفترة الحالية",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = ElamalUtils.formatMoney(totalNet),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    if (activeClockIn > 0L) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "+${ElamalUtils.formatMoney(liveEarnedNow)} جارية",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.5f.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Inline warning banner if active session exceeds 16 hours (OPEN_WARN)
                if (activeClockIn > 0L && elapsedActiveMin > 16 * 60L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.24f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = "تنبيه", tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                            Text(
                                text = "مضى وقت طويل على الدخول. تأكد من مراجعة وقت الخروج عند الإنهاء.",
                                color = Color(0xFFFDE68A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniStatPill(
                            label = "ساعات",
                            valueText = ElamalUtils.formatMoney(activeHourlySum),
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniStatPill(
                            label = "عن بعد",
                            valueText = "+${ElamalUtils.formatMoney(activeRemoteSum)}",
                            valueColor = Color(0xFF93C5FD),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniStatPill(
                            label = "كريدي",
                            valueText = "-${ElamalUtils.formatMoney(activeCreditSum)}",
                            valueColor = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniStatPill(
                            label = "مرحّل",
                            valueText = ElamalUtils.formatMoney(lastCarryOver),
                            valueColor = Color(0xFFFDE68A),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                if (activeClockIn == 0L) startClockIn() else showClockOutDialog = true
            },
            modifier = Modifier
                .weight(1.35f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activeClockIn == 0L) primaryCustom else Color(0xFFDC2626)
            )
        ) {
            Icon(
                if (activeClockIn == 0L) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = stringResource(R.string.btn_clock_in),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (activeClockIn == 0L) stringResource(R.string.btn_clock_in) else "خروج (${ElamalUtils.formatDurationArabic(elapsedActiveMin)})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5f.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalIconButton(
            onClick = {
                prefillDialogDateMillis = null
                showManualHourlyDialog = true
            },
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.MoreTime, contentDescription = "إضافة ساعات يدوية")
        }

        FilledTonalButton(
            onClick = {
                prefillDialogDateMillis = null
                showRemoteDialog = true
            },
            modifier = Modifier
                .weight(0.95f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFDBEAFE),
                contentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
            )
        ) {
            Icon(Icons.Default.Computer, contentDescription = stringResource(R.string.btn_remote), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                stringResource(R.string.btn_remote),
                fontSize = 11.5f.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = {
                prefillDialogDateMillis = null
                showCreditDialog = true
            },
            modifier = Modifier
                .weight(0.95f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEE2E2),
                contentColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
            )
        ) {
            Text(
                stringResource(R.string.btn_credit),
                fontWeight = FontWeight.Bold,
                fontSize = 11.5f.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (wifiSessions.isNotEmpty()) {
        val mergeableDayGroups = remember(pendingWifiList) {
            pendingWifiList.filter { !it.isOpen }
                .groupBy { ElamalUtils.formatDate(it.startTime) }
                .filter { it.value.size >= 2 }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.25f)),
            colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = "واي فاي العمل", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        Text(
                            "فترات واي فاي المقترحة (${pendingWifiList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5f.sp
                        )
                    }
                    if (pendingWifiList.isNotEmpty()) {
                        TextButton(onClick = { confirmIgnoreAllWifi = true }) {
                            Text("تجاهل الكل", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }

                mergeableDayGroups.forEach { (dayStr, dayList) ->
                    val sorted = dayList.sortedBy { it.startTime }
                    var gapMs = 0L
                    for (i in 1 until sorted.size) {
                        gapMs += (sorted[i].startTime - sorted[i - 1].endTime).coerceAtLeast(0L)
                    }
                    val brkMins = (gapMs / 60_000L).toInt().coerceAtLeast(0)
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = { mergeAndApproveDayWifiSessions(dayList) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = "دمج الفترات", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "دمج ${dayList.size} فترات ليوم ${ElamalUtils.ltrIsolate(dayStr)} (استراحة ${ElamalUtils.localizeDigits("$brkMins")}د)",
                            fontSize = 11.5f.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                wifiSessions.take(3).forEach { ws ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(ws.ssid, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            val durMin = ((ws.endTime - ws.startTime) / 60000L).coerceAtLeast(1L)
                            Text("المدة المرصودة: ${ElamalUtils.formatDurationArabic(durMin)}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        }
                        if (ws.status == "ADDED") {
                            Surface(
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "أُضيفت ✓",
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                    fontSize = 11.5f.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { approveWifiSession(ws) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("اعتماد", fontSize = 12.sp) }

                                OutlinedButton(
                                    onClick = { ignoreWifiSession(ws) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("تجاهل", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (searchFilterEnabled) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث في الملاحظات (يدعم البحث الذكي)...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        val (cntHourly, cntRemote, cntCredit) = remember(entries) {
            Triple(
                entries.count { it.type == EntryType.HOURLY },
                entries.count { it.type == EntryType.REMOTE },
                entries.count { it.type == EntryType.CREDIT }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("الكل (${entries.size})", fontSize = 11.5f.sp, maxLines = 1) })
            FilterChip(selected = filterType == EntryType.HOURLY, onClick = { filterType = EntryType.HOURLY }, label = { Text("ساعات ($cntHourly)", fontSize = 11.5f.sp, maxLines = 1) })
            FilterChip(selected = filterType == EntryType.REMOTE, onClick = { filterType = EntryType.REMOTE }, label = { Text("عن بعد ($cntRemote)", fontSize = 11.5f.sp, maxLines = 1) })
            FilterChip(selected = filterType == EntryType.CREDIT, onClick = { filterType = EntryType.CREDIT }, label = { Text("كريدي ($cntCredit)", fontSize = 11.5f.sp, maxLines = 1) })
        }
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }

    val normalizedQuery = remember(searchQuery) { ElamalUtils.normalizeArabicSearch(searchQuery) }
    val filtered = remember(entries, searchFilterEnabled, filterType, normalizedQuery) {
        entries.filter {
            (!searchFilterEnabled || filterType == null || it.type == filterType) &&
                    (!searchFilterEnabled || normalizedQuery.isEmpty() || ElamalUtils.normalizeArabicSearch(it.note).contains(normalizedQuery))
        }
    }

    val groupedByWeek = remember(filtered) {
        filtered.groupBy { ElamalUtils.getWeekMondayStartMillis(it.startTime) }
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventNote, contentDescription = stringResource(R.string.empty_ledger_title), modifier = Modifier.size(56.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.empty_ledger_title), fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.empty_ledger_subtitle), fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            groupedByWeek.forEach { (weekMondayMillis, weekEntries) ->
                item(key = "wk_hdr_$weekMondayMillis") {
                    val weekNet = ElamalUtils.roundMoney(weekEntries.sumOf { it.calculatedAmount })
                    val weekLabel = ElamalUtils.formatWeekRangeLabel(weekMondayMillis)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weekLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = ElamalUtils.formatMoney(weekNet),
                                    fontSize = 12.5f.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (weekNet < 0) Color(0xFFDC2626) else primaryCustom
                                )
                            }
                            IconButton(
                                onClick = {
                                    openShareDialog(
                                        title = "ملخص $weekLabel",
                                        targetEntries = weekEntries,
                                        netTotal = weekNet,
                                        carryOver = 0.0
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "مشاركة الأسبوع",
                                    tint = primaryCustom,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                items(weekEntries, key = { it.id }) { item ->
                    EntryRowCard(
                        item = item,
                        fontScale = fontScale,
                        onEdit = { editingEntry = item },
                        onDelete = { requestDeleteEntry(item) }
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
private fun <A, B, C, D, E> quintuple(a: A, b: B, c: C, d: D, e: E) = Quintuple(a, b, c, d, e)
