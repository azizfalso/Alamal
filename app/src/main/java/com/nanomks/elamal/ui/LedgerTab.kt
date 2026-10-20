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
fun AppState.LedgerTab() {
                        // TAB 0: HOME & LEDGER
                        val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0
                        val activeSum = entries.sumOf { it.calculatedAmount }
                        val totalNet = activeSum + lastCarryOver
                        val totalMinutesActive = entries.filter { it.type == EntryType.HOURLY && it.endTime != null }
                            .sumOf { (((it.endTime!! - it.startTime) / 60000L) - it.breakMinutes).coerceAtLeast(0) }

                        // Hero Financial Dashboard Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(primaryCustom, Color(0xFF047857))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الصافي المستحق حالياً", color = Color.White.copy(alpha = 0.85f), fontSize = (14 * fontScale).sp)
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "⏱ ${ElamalUtils.formatDurationArabic(totalMinutesActive)}",
                                            color = Color.White,
                                            fontSize = (12 * fontScale).sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = ElamalUtils.formatMoney(totalNet),
                                        fontSize = (32 * fontScale).sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                if (lastCarryOver != 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "يشمل رصيداً مرحّلاً من الدفعة السابقة: ${ElamalUtils.formatMoney(lastCarryOver)}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = (12 * fontScale).sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Main Action Buttons (Clock-In/Out + Remote + Credit)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (activeClockIn == 0L) {
                                        val now = System.currentTimeMillis()
                                        prefs.edit().putLong("active_clock_in", now).apply()
                                        activeClockIn = now
                                        ElamalUtils.scheduleForgotClockOutAlarm(context, now)
                                        ElamalWidgetReceiver.refreshAllWidgets(context)
                                        Toast.makeText(context, "بدأ حساب ساعات العمل ✓", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showClockOutDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeClockIn == 0L) primaryCustom else Color(0xFFDC2626)
                                )
                            ) {
                                Icon(
                                    if (activeClockIn == 0L) Icons.Default.PlayArrow else Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val elapsedMin = if (activeClockIn > 0L) (nowMillis - activeClockIn) / 60000L else 0L
                                Text(
                                    text = if (activeClockIn == 0L) "تسجيل دخول" else "خروج (${ElamalUtils.formatDurationArabic(elapsedMin)})",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = { showRemoteDialog = true },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عن بعد")
                            }

                            FilledTonalButton(
                                onClick = { showCreditDialog = true },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFEE2E2),
                                    contentColor = Color(0xFF991B1B)
                                )
                            ) {
                                Text("كريدي (-)", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Wi-Fi Sessions Pending Review Card right on the Home Screen!
                        if (wifiSessions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF2563EB))
                                            Text(
                                                "فترات واي فاي بانتظار المراجعة (${pendingWifiList.size})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (14 * fontScale).sp
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
                                                Text("المدة المقترحة: ${ElamalUtils.formatDurationArabic(durMin)}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                            }
                                            if (ws.status == "ADDED") {
                                                Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(8.dp)) {
                                                    Text("أُضيفت ✓", color = Color(0xFF065F46), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            } else {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                val pay = ElamalUtils.calculateHourlyPay(ws.startTime, ws.endTime, 0, hourlyRate)
                                                                dao.insertEntry(
                                                                    WorkEntry(
                                                                        type = EntryType.HOURLY,
                                                                        startTime = ws.startTime,
                                                                        endTime = ws.endTime,
                                                                        hourlyRateAtEntry = hourlyRate,
                                                                        calculatedAmount = pay,
                                                                        note = "حضور واي فاي (${ws.ssid})"
                                                                    )
                                                                )
                                                                dao.updateWifiSession(ws.copy(status = "ADDED"))
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) { Text("أضف", fontSize = 12.sp) }

                                                    OutlinedButton(
                                                        onClick = { scope.launch { dao.updateWifiSession(ws.copy(status = "IGNORED")) } },
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
                                placeholder = { Text("ابحث في الملاحظات...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("الكل (${entries.size})") })
                                FilterChip(selected = filterType == EntryType.HOURLY, onClick = { filterType = EntryType.HOURLY }, label = { Text("ساعات") })
                                FilterChip(selected = filterType == EntryType.REMOTE, onClick = { filterType = EntryType.REMOTE }, label = { Text("عن بعد") })
                                FilterChip(selected = filterType == EntryType.CREDIT, onClick = { filterType = EntryType.CREDIT }, label = { Text("كريدي") })
                            }
                        }

                        val filtered = entries.filter {
                            (!searchFilterEnabled || filterType == null || it.type == filterType) &&
                                    (!searchFilterEnabled || searchQuery.isBlank() || it.note.contains(searchQuery, ignoreCase = true))
                        }

                        if (filtered.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(54.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد سجلات عمل في الدفتر حالياً", fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                    Text("اضغط على «تسجيل دخول» أو «عن بعد» أو «كريدي» للبدء", fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered, key = { it.id }) { item ->
                                    EntryRowCard(
                                        item = item,
                                        fontScale = fontScale,
                                        onEdit = { editingEntry = item },
                                        onDelete = {
                                            lastDeletedEntry = item
                                            scope.launch {
                                                dao.updateEntry(item.copy(isDeleted = true))
                                                statusBanner = BackupSafManager.performSmartBackup(context, dao)
                                            }
                                        }
                                    )
                                }
                            }
                        }
}
