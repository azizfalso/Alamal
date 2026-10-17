package com.example.worktracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.worktracker.ui.home.HomeScreen
import com.example.worktracker.ui.home.HomeViewModel
import com.example.worktracker.ui.calendar.CalendarScreen
import com.example.worktracker.ui.summary.SummaryScreen

@Composable
fun MainAppScreen(viewModel: HomeViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("📝") },
                    label = { Text("السجلات", color = if (selectedTab == 0) Color(0xFFD4AF37) else Color.Gray) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF333333))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📅") },
                    label = { Text("التقويم", color = if (selectedTab == 1) Color(0xFFD4AF37) else Color.Gray) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF333333))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📊") },
                    label = { Text("الملخص", color = if (selectedTab == 2) Color(0xFFD4AF37) else Color.Gray) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF333333))
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel)
                1 -> CalendarScreen(viewModel)
                2 -> SummaryScreen(viewModel)
            }
        }
    }
}
