package com.example.worktracker.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worktracker.ui.home.HomeViewModel

@Composable
fun SummaryScreen(viewModel: HomeViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val days by viewModel.activeDays.collectAsState()

    val totalWages = days.filter { !it.isCredit }.sumOf { it.amount }
    val totalCredit = days.filter { it.isCredit }.sumOf { it.amount }

    // فرض الخلفية الداكنة هنا أيضاً
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        Text("نظرة عامة", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "مجموع الأجور", value = totalWages, color = Color(0xFFA5D6A7), modifier = Modifier.weight(1f))
            StatCard(title = "مجموع الكريدي", value = totalCredit, color = Color(0xFFFF6B6B), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("الأيام الأخيرة (رسم بياني)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val recentAmounts = days.take(7).map { it.amount.toFloat() }.reversed()
        if (recentAmounts.isNotEmpty()) {
            val maxAmount = recentAmounts.maxOfOrNull { Math.abs(it) } ?: 1f
            
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    val barWidth = size.width / (recentAmounts.size * 2)
                    val space = barWidth
                    
                    recentAmounts.forEachIndexed { index, amount ->
                        val isNegative = amount < 0
                        val barHeight = (Math.abs(amount) / maxAmount) * (size.height - 20f)
                        val x = index * (barWidth + space) + space / 2
                        val y = if (isNegative) size.height / 2 else (size.height / 2) - barHeight

                        drawRoundRect(
                            color = if (isNegative) Color(0xFFFF6B6B) else Color(0xFFA5D6A7),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight.coerceAtLeast(10f)),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                    drawLine(color = Color.Gray.copy(alpha = 0.5f), start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2f)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${String.format(java.util.Locale.US, "%.1f", value)}", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
