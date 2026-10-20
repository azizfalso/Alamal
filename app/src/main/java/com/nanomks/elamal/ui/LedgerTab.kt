package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.util.ElamalUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.LedgerTab() {
    val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0
    val activeSum = entries.sumOf { it.calculatedAmount }
    val totalNet = activeSum + lastCarryOver
    val totalMinutesActive = entries.filter { it.type == EntryType.HOURLY && it.endTime != null }
        .sumOf { (((it.endTime!! - it.startTime) / 60000L) - it.breakMinutes).coerceAtLeast(0) }

    val elapsedActiveMin = if (activeClockIn > 0L) ((nowMillis - activeClockIn) / 60000L).coerceAtLeast(0L) else 0L
    val liveEarnedNow = if (activeClockIn > 0L) ElamalUtils.calculateHourlyPay(activeClockIn, nowMillis, 0, hourlyRate) else 0.0

    val activeHourlySum = entries.filter { it.type == EntryType.HOURLY }.sumOf { it.calculatedAmount }
    val activeRemoteSum = entries.filter { it.type == EntryType.REMOTE }.sumOf { it.calculatedAmount }
    val activeCreditSum = kotlin.math.abs(entries.filter { it.type == EntryType.CREDIT }.sumOf { it.calculatedAmount })

    // Hero Financial Dashboard Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                            listOf(primaryCustom, Color(0xFF0F172A))
                        }
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
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
                            text = if (activeClockIn > 0L) "جلسة عمل جارية الآن..." else "الصافي المستحق حالياً",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.5f.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "⏱ ${ElamalUtils.formatDurationArabic(totalMinutesActive + elapsedActiveMin)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = ElamalUtils.formatMoney(totalNet),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Rich Mini Breakdown inside Hero Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ساعات: ${ElamalUtils.formatMoney(activeHourlySum)}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (activeRemoteSum > 0.0) {
                        Text(
                            text = "عن بعد: +${ElamalUtils.formatMoney(activeRemoteSum)}",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (activeCreditSum > 0.0) {
                        Text(
                            text = "كريدي: -${ElamalUtils.formatMoney(activeCreditSum)}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (lastCarryOver != 0.0) {
                        Text(
                            text = "مرحّل: ${ElamalUtils.formatMoney(lastCarryOver)}",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Main Action Buttons (Fixed text clipping issue in Image 1!)
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
                .weight(1.45f)
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activeClockIn == 0L) primaryCustom else Color(0xFFDC2626)
            )
        ) {
            Icon(
                if (activeClockIn == 0L) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (activeClockIn == 0L) "تسجيل دخول" else "خروج (${ElamalUtils.formatDurationArabic(elapsedActiveMin)})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5f.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalIconButton(
            onClick = { showManualHourlyDialog = true },
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.MoreTime, contentDescription = "إضافة ساعات يدوية")
        }

        FilledTonalButton(
            onClick = { showRemoteDialog = true },
            modifier = Modifier
                .weight(0.95f)
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "عن بعد",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = { showCreditDialog = true },
            modifier = Modifier
                .weight(0.95f)
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFFFEE2E2),
                contentColor = Color(0xFF991B1B)
            )
        ) {
            Text(
                "كريدي (-)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // Wi-Fi Sessions Pending Review Card
    if (wifiSessions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
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
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
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

                wifiSessions.take(3).forEach { ws ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(ws.ssid, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val durMin = ((ws.endTime - ws.startTime) / 60000L).coerceAtLeast(1L)
                            Text("المدة المرصودة: ${ElamalUtils.formatDurationArabic(durMin)}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        }
                        if (ws.status == "ADDED") {
                            Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(8.dp)) {
                                Text("أُضيفت ✓", color = Color(0xFF065F46), fontSize = 11.5f.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث في الملاحظات...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        val cntHourly = entries.count { it.type == EntryType.HOURLY }
        val cntRemote = entries.count { it.type == EntryType.REMOTE }
        val cntCredit = entries.count { it.type == EntryType.CREDIT }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("الكل (${entries.size})", fontSize = 12.sp) })
            FilterChip(selected = filterType == EntryType.HOURLY, onClick = { filterType = EntryType.HOURLY }, label = { Text("ساعات ($cntHourly)", fontSize = 12.sp) })
            FilterChip(selected = filterType == EntryType.REMOTE, onClick = { filterType = EntryType.REMOTE }, label = { Text("عن بعد ($cntRemote)", fontSize = 12.sp) })
            FilterChip(selected = filterType == EntryType.CREDIT, onClick = { filterType = EntryType.CREDIT }, label = { Text("كريدي ($cntCredit)", fontSize = 12.sp) })
        }
    } else {
        Spacer(modifier = Modifier.height(10.dp))
    }

    val filtered = entries.filter {
        (!searchFilterEnabled || filterType == null || it.type == filterType) &&
                (!searchFilterEnabled || searchQuery.isBlank() || it.note.contains(searchQuery, ignoreCase = true))
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(56.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("لا توجد سجلات عمل مطابقة في الدفتر حالياً", fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Text("اضغط على «تسجيل دخول» أو أيقونة الساعات اليدوية للبدء", fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                EntryRowCard(
                    item = item,
                    fontScale = fontScale,
                    onEdit = { editingEntry = item },
                    onDelete = { softDeleteEntry(item) }
                )
            }
        }
    }
}
