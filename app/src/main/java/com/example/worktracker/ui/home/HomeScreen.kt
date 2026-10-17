package com.example.worktracker.ui.home

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeDays by viewModel.activeDays.collectAsState()

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

    Scaffold(
        containerColor = Color(0xFF121212),
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFF1A1A1A),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { currentSheetType = "credit"; showSheet = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                    ) { Text("— كريدي") }
                    
                    OutlinedButton(
                        onClick = { currentSheetType = "remote"; showSheet = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("عن بعد") }
                    
                    Button(
                        onClick = { currentSheetType = "work"; showSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B22), contentColor = Color(0xFFA5D6A7))
                    ) { Text("+ يوم عمل") }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("المجموع الكامل", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalBalance ?: 0.0)} DH",
                        color = Color(0xFFD4AF37),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("السجلات الأخيرة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activeDays) { day -> DayItemCard(day) }
            }
        }
    }
}

@Composable
fun DayItemCard(day: WorkDay) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
    val dateString = dateFormat.format(Date(day.date))
    val isNegative = day.amount < 0
    val amountColor = if (isNegative) Color(0xFFFF6B6B) else Color(0xFFA5D6A7)
    
    val title = when {
        day.isCredit -> "كريدي"
        day.isRemote -> "عمل عن بعد"
        else -> "يوم عمل"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (day.note.isNotEmpty()) {
                    Text(text = day.note, color = Color.Gray, fontSize = 12.sp)
                }
                Text(text = dateString, color = Color.Gray, fontSize = 14.sp)
            }
            Text(
                text = "${day.amount} DH",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
