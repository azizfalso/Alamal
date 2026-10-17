package com.example.worktracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worktracker.data.local.WorkDay
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeDays by viewModel.activeDays.collectAsState()
    val activeSessionStart by viewModel.activeSessionStart.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var currentSheetType by remember { mutableStateOf("work") }

    if (showSheet) {
        AddRecordSheet(
            sheetType = currentSheetType,
            onDismiss = { showSheet = false },
            onSave = { amount, note, isCredit, isRemote ->
                viewModel.addRealRecord(amount, note, isCredit, isRemote)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(bottom = 70.dp)) { // مساحة للأزرار
            Card(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("المجموع الكامل", color = Color.Gray, fontSize = 16.sp)
                    Text(text = "${String.format(Locale.US, "%.2f", totalBalance ?: 0.0)} DH", color = Color(0xFFD4AF37), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val isClockedIn = activeSessionStart != null
            val cardColor = if (isClockedIn) Color(0xFF3A1C1C) else Color(0xFF2E3B22)
            val textColor = if (isClockedIn) Color(0xFFFF6B6B) else Color(0xFFA5D6A7)
            var elapsedTime by remember { mutableStateOf("00:00:00") }

            LaunchedEffect(isClockedIn, activeSessionStart) {
                if (isClockedIn && activeSessionStart != null) {
                    while (true) {
                        val durationMillis = System.currentTimeMillis() - activeSessionStart!!
                        val hours = (durationMillis / (1000 * 60 * 60)).toInt()
                        val minutes = ((durationMillis / (1000 * 60)) % 60).toInt()
                        val seconds = ((durationMillis / 1000) % 60).toInt()
                        elapsedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        delay(1000)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp).clickable { viewModel.toggleClockIn() },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (isClockedIn) "تسجيل خروج" else "تسجيل دخول العمل", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (isClockedIn) "وقت العمل المستغرق:" else "اضغط عند وصولك", color = textColor.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    if (isClockedIn) {
                        Text(elapsedTime, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("السجلات الأخيرة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activeDays) { day -> DayItemCard(day) }
            }
        }

        // الأزرار السفلية (عائمة فوق القائمة)
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).background(Color(0xFF1A1A1A).copy(alpha = 0.9f), RoundedCornerShape(16.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { currentSheetType = "credit"; showSheet = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))) { Text("— كريدي") }
            OutlinedButton(onClick = { currentSheetType = "remote"; showSheet = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("عن بعد") }
            Button(onClick = { currentSheetType = "work"; showSheet = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B22), contentColor = Color(0xFFA5D6A7))) { Text("+ إضافة") }
        }
    }
}

@Composable
fun DayItemCard(day: WorkDay) {
    val dateFormat = SimpleDateFormat("dd MMMM", Locale("ar"))
    val dateString = dateFormat.format(Date(day.date))
    val isNegative = day.amount < 0
    val amountColor = if (isNegative) Color(0xFFFF6B6B) else Color(0xFFA5D6A7)
    val title = when { day.isCredit -> "كريدي"; day.isRemote -> "عن بعد"; else -> "يوم عمل" }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = dateString, color = Color.Gray, fontSize = 13.sp)
            }
            Text(text = "${String.format(Locale.US, "%.2f", day.amount)} DH", color = amountColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
