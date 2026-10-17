package com.example.worktracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Scaffold(
        containerColor = Color(0xFF121212), // خلفية داكنة تناسب التطبيق
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { viewModel.addTestRecord(-50.0, true) },
                    containerColor = Color(0xFF3A1C1C), // أحمر غامق للسلف
                    contentColor = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("كريدي -", modifier = Modifier.padding(horizontal = 16.dp))
                }
                FloatingActionButton(
                    onClick = { viewModel.addTestRecord(120.5, false) },
                    containerColor = Color(0xFF2E3B22), // أخضر غامق للأجور
                    contentColor = Color(0xFFA5D6A7)
                ) {
                    Text("يوم عمل +", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            
            // بطاقة المجموع الكامل
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
                        text = "${totalBalance ?: 0.0} DH",
                        color = Color(0xFFD4AF37), // اللون الذهبي
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("السجلات الأخيرة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // قائمة الأيام
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activeDays) { day ->
                    DayItemCard(day)
                }
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
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = if (day.isCredit) "كريدي" else "يوم عمل",
                    color = Color.White, 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
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
