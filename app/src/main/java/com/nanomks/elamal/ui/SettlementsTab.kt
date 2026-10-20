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
fun AppState.SettlementsTab() {
                        // TAB 1: SETTLEMENTS & ARCHIVE
                        val prevCarry = settlements.firstOrNull()?.nextCarryOver ?: 0.0
                        val entriesSum = entries.sumOf { it.calculatedAmount }
                        val netDue = entriesSum + prevCarry

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("الرصيد الجاهز للاستلام والأرشفة", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(ElamalUtils.formatMoney(netDue), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = primaryCustom)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showSettleDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("استلام دفعة جديدة وترحيل الفرق", fontWeight = FontWeight.Bold)
                                }
                                if (settlements.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            val latest = settlements.first()
                                            scope.launch {
                                                dao.detachEntriesFromSettlement(latest.id)
                                                dao.deleteSettlement(latest)
                                                dao.recalculateSettlementsCascade()
                                                statusBanner = BackupSafManager.performSmartBackup(context, dao)
                                                Toast.makeText(context, "تم التراجع عن آخر دفعة وإعادة السجلات للدفتر ✓", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("التراجع عن آخر دفعة", color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("أرشيف الدفعات السابقة (${settlements.size})", fontWeight = FontWeight.Bold, fontSize = (15 * fontScale).sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (settlements.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(54.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("الأرشيف فارغ حالياً", fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                    Text("عند استلام دفعة، ستنتقل سجلاتها إلى هنا مع حفظ الرصيد المرحّل", fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(settlements, key = { it.id }) { s ->
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("دفعة ${df.format(Date(s.timestamp))}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Row {
                                                    IconButton(onClick = {
                                                        val sEntries = allEntries.filter { it.settlementId == s.id }
                                                        PdfReportGenerator.generateAndSharePdf(context, "دفعة ${df.format(Date(s.timestamp))}", sEntries, listOf(s), isDark)
                                                    }) {
                                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = primaryCustom)
                                                    }
                                                    IconButton(onClick = { editingSettlement = s }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                                    }
                                                }
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = colorScheme.onSurface.copy(alpha = 0.08f))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("مجموع السجلات: ${ElamalUtils.formatMoney(s.entriesTotal)}", fontSize = 13.sp)
                                                Text("مرحّل سابق: ${ElamalUtils.formatMoney(s.previousCarryOver)}", fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("المستلم: ${ElamalUtils.formatMoney(s.amountPaid)}", color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                                Text("المرحّل للقادم: ${ElamalUtils.formatMoney(s.nextCarryOver)}", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                            }
                                            if (s.note.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("📝 ${s.note}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
}
