package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.ElamalUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EntryRowCard(item: WorkEntry, fontScale: Float, onEdit: () -> Unit, onDelete: () -> Unit) {
    val badgeColor = when (item.type) {
        EntryType.HOURLY -> Color(0xFF059669)
        EntryType.REMOTE -> Color(0xFF2563EB)
        EntryType.CREDIT -> Color(0xFFDC2626)
    }
    val badgeLabel = when (item.type) {
        EntryType.HOURLY -> "ساعات"
        EntryType.REMOTE -> "عن بعد"
        EntryType.CREDIT -> "كريدي"
    }
    val badgeIcon = when (item.type) {
        EntryType.HOURLY -> Icons.Default.AccessTime
        EntryType.REMOTE -> Icons.Default.Computer
        EntryType.CREDIT -> Icons.Default.RemoveCircleOutline
    }
    val dfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dfTime = remember { SimpleDateFormat("HH:mm", Locale.US) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                color = Color.White,
                                fontSize = (10 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (item.type == EntryType.HOURLY && item.endTime != null) {
                            val mins = ((item.endTime - item.startTime) / 60000L) - item.breakMinutes
                            Text(
                                text = ElamalUtils.formatDurationArabic(mins),
                                fontSize = (13 * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val timeInfo = if (item.type == EntryType.HOURLY && item.endTime != null) {
                        val brkTxt = if (item.breakMinutes > 0) " • استراحة ${item.breakMinutes}د" else ""
                        "${dfDate.format(Date(item.startTime))} (${dfTime.format(Date(item.startTime))} ← ${dfTime.format(Date(item.endTime))})$brkTxt"
                    } else {
                        "${dfDate.format(Date(item.startTime))} • ${dfTime.format(Date(item.startTime))}"
                    }

                    Text(
                        text = timeInfo,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            fontSize = (12.5f * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = ElamalUtils.formatMoney(item.calculatedAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (16 * fontScale).sp,
                        color = if (item.calculatedAmount < 0) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}



