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
fun AppState.ReportsTab() {
                        // TAB 2: REPORTS, CHARTS & CALENDAR
                        val lastCarryOver = settlements.firstOrNull()?.nextCarryOver ?: 0.0
                        val activeSum = entries.sumOf { it.calculatedAmount }
                        val totalNet = activeSum + lastCarryOver

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = primaryCustom)
                                            Text("تصدير ومشاركة تقارير PDF (${if (isDark) "وضع داكن" else "وضع فاتح"})", fontWeight = FontWeight.Bold)
                                        }
                                        val now = System.currentTimeMillis()
                                        val weekAgo = now - 7 * 86400_000L
                                        val monthAgo = now - 30 * 86400_000L

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    PdfReportGenerator.generateAndSharePdf(context, "تقرير أسبوع", allEntries.filter { it.startTime >= weekAgo }, settlements, isDark)
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("أسبوع") }

                                            Button(
                                                onClick = {
                                                    PdfReportGenerator.generateAndSharePdf(context, "تقرير شهري", allEntries.filter { it.startTime >= monthAgo }, settlements, isDark)
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("شهري") }

                                            Button(
                                                onClick = {
                                                    PdfReportGenerator.generateAndSharePdf(context, "الملخص النشط", entries, settlements, isDark)
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
                                                    PdfReportGenerator.generateAndSharePdf(context, "تقرير آخر دفعة", sEntries, settlements, isDark)
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("دفعة") }

                                            OutlinedButton(
                                                onClick = {
                                                    PdfReportGenerator.generateAndSharePdf(context, "كل الدفعات", allEntries.filter { it.settlementId != null }, settlements, isDark)
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("كل الدفعات") }

                                            OutlinedButton(
                                                onClick = { showCustomPeriodPdfDialog = true },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("فترة") }
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
                                                Text("نص لواتساب")
                                            }

                                            OutlinedButton(
                                                onClick = { PdfReportGenerator.exportAndShareCsv(context, allEntries) },
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

                            item {
                                val hourlySum = allEntries.filter { it.type == EntryType.HOURLY }.sumOf { it.calculatedAmount }
                                val remoteSum = allEntries.filter { it.type == EntryType.REMOTE }.sumOf { it.calculatedAmount }
                                val creditSum = kotlin.math.abs(allEntries.filter { it.type == EntryType.CREDIT }.sumOf { it.calculatedAmount })
                                val maxVal = maxOf(hourlySum, remoteSum, creditSum, 10.0)

                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("الرسم البياني للأجور والكريدي", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            val barW = w / 5.5f
                                            // Minimum visible bar height 8dp even when 0 so chart never looks broken/empty!
                                            val h1 = maxOf(((hourlySum / maxVal) * (h - 16f)).toFloat(), 12f)
                                            val h2 = maxOf(((remoteSum / maxVal) * (h - 16f)).toFloat(), 12f)
                                            val h3 = maxOf(((creditSum / maxVal) * (h - 16f)).toFloat(), 12f)

                                            drawRoundRect(Color(0xFF059669), topLeft = Offset(barW * 0.6f, h - h1), size = Size(barW, h1), cornerRadius = CornerRadius(12f, 12f))
                                            drawRoundRect(Color(0xFF2563EB), topLeft = Offset(barW * 2.25f, h - h2), size = Size(barW, h2), cornerRadius = CornerRadius(12f, 12f))
                                            drawRoundRect(Color(0xFFDC2626), topLeft = Offset(barW * 3.9f, h - h3), size = Size(barW, h3), cornerRadius = CornerRadius(12f, 12f))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                            Text("ساعات: ${ElamalUtils.formatMoney(hourlySum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                            Text("عن بعد: ${ElamalUtils.formatMoney(remoteSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                            Text("كريدي: -${ElamalUtils.formatMoney(creditSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }

                            item {
                                val cal = Calendar.getInstance()
                                val currentMonth = cal.get(Calendar.MONTH)
                                val currentYear = cal.get(Calendar.YEAR)
                                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("التقويم الشهري (${currentYear}/${currentMonth + 1})", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        for (weekRow in 0 until ((daysInMonth + 6) / 7)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                for (col in 1..7) {
                                                    val dayNum = weekRow * 7 + col
                                                    if (dayNum <= daysInMonth) {
                                                        val daySum = allEntries.filter {
                                                            val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                                                            c.get(Calendar.YEAR) == currentYear &&
                                                                    c.get(Calendar.MONTH) == currentMonth &&
                                                                    c.get(Calendar.DAY_OF_MONTH) == dayNum
                                                        }.sumOf { it.calculatedAmount }

                                                        val boxBg = when {
                                                            daySum > 0 -> Color(0xFF059669)
                                                            daySum < 0 -> Color(0xFFDC2626)
                                                            isDark -> Color(0xFF334155)
                                                            else -> Color(0xFFE2E8F0)
                                                        }
                                                        val txtCol = if (daySum != 0.0 || isDark) Color.White else Color(0xFF1E293B)

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(42.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(boxBg),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("$dayNum", fontSize = 12.sp, color = txtCol, fontWeight = FontWeight.Bold)
                                                                if (daySum != 0.0) {
                                                                    Text("${daySum.toInt()}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f))
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
}
