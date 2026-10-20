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

@Composable
fun ElamalRootScreen(activity: FragmentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember {
        AppState(
            activity,
            context,
            context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE),
            ElamalDatabase.getInstance(context).dao(),
            scope
        )
    }
    state.RootContent()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.RootContent() {
    sysDark = isSystemInDarkTheme()
    primaryCustom = runCatching { Color(android.graphics.Color.parseColor(appColorHex)) }.getOrDefault(Color(0xFF059669))

    // Rich, clean, high-contrast Light & Dark Color Schemes (Eliminates muddy olive/beige defaults!)
    colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = primaryCustom,
            onPrimary = Color.White,
            secondary = Color(0xFF3B82F6),
            background = Color(0xFF0B1120),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF1E293B),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
        else -> lightColorScheme(
            primary = primaryCustom,
            onPrimary = Color.White,
            secondary = Color(0xFF2563EB),
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFFFFFFF),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    LaunchedEffect(isDark, primaryCustom) {
        activity.window.statusBarColor = if (isDark) Color(0xFF0B1120).toArgb() else primaryCustom.toArgb()
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
        // Security Lock Screen
        if (!isUnlocked) {
            LockScreen()
            return@MaterialTheme
        }

        // First Run Onboarding Dialog
        if (!firstRunDone) {
            OnboardingDialog()
        }

        entries = dao.getActiveEntries().collectAsState(initial = emptyList()).value
        allEntries = dao.getAllNonDeletedEntriesFlow().collectAsState(initial = emptyList()).value
        settlements = dao.getAllSettlements().collectAsState(initial = emptyList()).value
        wifiSessions = dao.getVisibleWifiSessions().collectAsState(initial = emptyList()).value


        // Minute Ticker + Auto Widget Queue Sync on App Open
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                ElamalUtils.processPendingWidgetEventsOnAppOpen(context, dao)
            }
            while (true) {
                nowMillis = System.currentTimeMillis()
                activeClockIn = prefs.getLong("active_clock_in", 0L)
                withContext(Dispatchers.IO) {
                    detectedRootSsid = ElamalUtils.pollAndMergeRootWifi(context, dao)
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryCustom),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("العمل", fontSize = (19 * fontScale).sp, fontWeight = FontWeight.ExtraBold)
                                if (statusBanner.isNotBlank()) {
                                    Text(statusBanner, fontSize = 11.sp, color = primaryCustom)
                                }
                            }
                        }
                    },
                    actions = {
                        if (lastDeletedEntry != null) {
                            FilledTonalButton(
                                onClick = {
                                    val toRestore = lastDeletedEntry!!
                                    scope.launch {
                                        dao.updateEntry(toRestore.copy(isDeleted = false))
                                        lastDeletedEntry = null
                                        Toast.makeText(context, "تم التراجع عن الحذف ✓", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تراجع عن الحذف")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        label = { Text("الدفتر", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("الدفعات", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        label = { Text("التقارير", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("الإعدادات", fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) }
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
                when (currentTab) {
                    0 -> LedgerTab()
                    1 -> SettlementsTab()
                    2 -> ReportsTab()
                    3 -> SettingsTab()

                }
            }
        }

        AppDialogs()
    }
}
