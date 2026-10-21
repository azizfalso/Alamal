package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.util.ElamalUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppState.SettlementsTab() {
    val prevCarry = settlements.firstOrNull()?.nextCarryOver ?: 0.0
    val entriesSum = entries.sumOf { it.calculatedAmount }
    val netDue = entriesSum + prevCarry
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    var expandedSettlementId by remember { mutableStateOf<Long?>(null) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, primaryCustom.copy(alpha = 0.25f)),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("الرصيد الجاهز للاستلام والأرشفة", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(ElamalUtils.formatMoney(netDue), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = primaryCustom)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showSettleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استلام دفعة جديدة وترحيل الفرق", fontWeight = FontWeight.Bold)
            }
            if (settlements.isNotEmpty()) {
                TextButton(
                    onClick = { undoLatestSettlement() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("التراجع عن آخر دفعة", color = Color(0xFFEF4444), fontSize = 12.5f.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("أرشيف الدفعات السابقة (${settlements.size}) - اضغط لعرض السجلات", fontWeight = FontWeight.Bold, fontSize = 14.5f.sp)
    Spacer(modifier = Modifier.height(8.dp))

    if (settlements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(54.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("الأرشيف فارغ حالياً", fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Text("عند استلام دفعة، ستنتقل سجلاتها إلى هنا مع حفظ الرصيد المرحّل", fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            items(settlements, key = { it.id }) { s ->
                val sEntries = allEntries.filter { it.settlementId == s.id }
                val isExpanded = expandedSettlementId == s.id

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            expandedSettlementId = if (isExpanded) null else s.id
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isExpanded) primaryCustom.copy(alpha = 0.45f) else colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("دفعة ${df.format(Date(s.timestamp))}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("عدد السجلات المؤرشفة: ${sEntries.size}", fontSize = 11.5f.sp, color = colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    exportPdfReportAsync("دفعة ${df.format(Date(s.timestamp))}", sEntries, listOf(s))
                                }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = primaryCustom)
                                }
                                IconButton(onClick = { editingSettlement = s }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant
                                )
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

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = primaryCustom.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "السجلات المؤرشفة في هذه الدفعة (${sEntries.size}):",
                                fontSize = 12.5f.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryCustom
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (sEntries.isEmpty()) {
                                Text("لا توجد سجلات مرتبطة بهذه الدفعة.", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sEntries.forEach { item ->
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
                    }
                }
            }
        }
    }
}
