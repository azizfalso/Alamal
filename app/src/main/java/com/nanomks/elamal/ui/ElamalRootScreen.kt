package com.nanomks.elamal.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ElamalRootScreen(activity: FragmentActivity) {
    val state: AppState = viewModel()
    state.RootContent(activity = activity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.RootContent(activity: FragmentActivity) {
    val context = LocalContext.current
    sysDark = isSystemInDarkTheme()
    primaryCustom = runCatching { Color(android.graphics.Color.parseColor(appColorHex)) }.getOrDefault(Color(0xFF059669))

    colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = primaryCustom,
            onPrimary = Color.White,
            secondary = Color(0xFF3B82F6),
            background = Color(0xFF090E1A),
            surface = Color(0xFF151F32),
            surfaceVariant = Color(0xFF1E293B),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8)
        )
        else -> lightColorScheme(
            primary = primaryCustom,
            onPrimary = Color.White,
            secondary = Color(0xFF2563EB),
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    LaunchedEffect(isDark, primaryCustom) {
        activity.window.statusBarColor = if (isDark) Color(0xFF090E1A).toArgb() else primaryCustom.toArgb()
    }

    folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.edit().putString("backup_uri", uri.toString()).putBoolean("first_run_done", true).apply()
            backupUriStr = uri.toString()
            firstRunDone = true
            Toast.makeText(context, "تم ربط مجلد النسخ الاحتياطي بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        if (!isUnlocked) {
            LockScreen(activity = activity)
            return@MaterialTheme
        }

        if (!firstRunDone) {
            OnboardingDialog()
        }

        entries = dao.getActiveEntries().collectAsState(initial = emptyList()).value
        allEntries = dao.getAllNonDeletedEntriesFlow().collectAsState(initial = emptyList()).value
        settlements = dao.getAllSettlements().collectAsState(initial = emptyList()).value
        wifiSessions = dao.getVisibleWifiSessions().collectAsState(initial = emptyList()).value

        // Minute Ticker + Auto Widget Queue Sync
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                ElamalUtils.processPendingWidgetEventsOnAppOpen(context, dao)
                dao.purgeSoftDeletedEntries()
            }
            while (true) {
                nowMillis = System.currentTimeMillis()
                activeClockIn = prefs.getLong("active_clock_in", 0L)
                if (activeClockIn > 0L) {
                    ElamalUtils.showOrUpdateOngoingWorkNotification(context, activeClockIn, hourlyRate)
                } else {
                    ElamalUtils.cancelOngoingWorkNotification(context)
                }
                if (rootWifiEnabled && workSsids.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        detectedRootSsid = ElamalUtils.pollAndMergeRootWifi(context, dao)
                    }
                }
                delay(60_000L)
            }
        }

        Scaffold(
            containerColor = colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface,
                        titleContentColor = colorScheme.onSurface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(primaryCustom, primaryCustom.copy(alpha = 0.75f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccessTimeFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("العمل", fontSize = (19 * fontScale).sp, fontWeight = FontWeight.ExtraBold)
                                    Surface(
                                        color = primaryCustom.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "v4.2",
                                            fontSize = 10.sp,
                                            color = primaryCustom,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (statusBanner.isNotBlank()) {
                                    Text(statusBanner, fontSize = 11.sp, color = primaryCustom, maxLines = 1)
                                }
                            }
                        }
                    },
                    actions = {
                        if (lastDeletedEntry != null) {
                            FilledTonalButton(
                                onClick = { undoLastDelete() },
                                modifier = Modifier.padding(end = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تراجع عن الحذف", fontSize = 12.sp)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = colorScheme.surface,
                    tonalElevation = 10.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingWifiList.isNotEmpty()) {
                                        Badge { Text("${pendingWifiList.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                            }
                        },
                        label = { Text("الدفتر", fontWeight = if (currentTab == 0) FontWeight.ExtraBold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("الدفعات", fontWeight = if (currentTab == 1) FontWeight.ExtraBold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        label = { Text("التقارير", fontWeight = if (currentTab == 2) FontWeight.ExtraBold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("الإعدادات", fontWeight = if (currentTab == 3) FontWeight.ExtraBold else FontWeight.Normal) }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabTransition"
                ) { tabIndex ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (tabIndex) {
                            0 -> LedgerTab()
                            1 -> SettlementsTab()
                            2 -> ReportsTab()
                            3 -> SettingsTab()
                        }
                    }
                }
            }
        }

        AppDialogs()
    }
}



