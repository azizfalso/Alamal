package com.nanomks.elamal.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    val baseDensity = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    sysDark = isSystemInDarkTheme()
    primaryCustom = runCatching { Color(android.graphics.Color.parseColor(appColorHex)) }.getOrDefault(Color(0xFF059669))

    colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = primaryCustom,
            onPrimary = Color.White,
            secondary = primaryCustom.copy(alpha = 0.85f),
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
            secondary = primaryCustom.copy(alpha = 0.85f),
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE2E8F0),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    // Back navigation handler (#21): return to Ledger tab first before closing app
    BackHandler(enabled = isUnlocked && currentTab != 0) {
        currentTab = 0
    }

    LaunchedEffect(isDark, primaryCustom, colorScheme.surface) {
        val window = activity.window
        window.statusBarColor = if (isDark) Color(0xFF090E1A).toArgb() else primaryCustom.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = !isDark
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
            showNotice("تم ربط مجلد النسخ الاحتياطي بنجاح ✓")
        }
    }

    // One-time startup sync: only purge soft-deleted entries older than 7 days (#1)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ElamalUtils.processPendingWidgetEventsOnAppOpen(context, dao)
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 86400_000L
            dao.purgeOldSoftDeletedEntries(sevenDaysAgo)
            if (rootWifiEnabled && workSsids.isNotBlank() && ElamalUtils.isWifiConnectedFast(context)) {
                detectedRootSsid = ElamalUtils.pollAndMergeRootWifi(context, dao)
            }
        }
        val currentClockIn = prefs.getLong("active_clock_in", 0L)
        activeClockIn = currentClockIn
        if (currentClockIn > 0L) {
            ElamalUtils.showOrUpdateOngoingWorkNotification(context, currentClockIn, hourlyRate)
        }
    }

    // Battery-friendly UI minute ticker
    LaunchedEffect(lifecycleOwner, activeClockIn) {
        if (activeClockIn <= 0L) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (activeClockIn > 0L) {
                nowMillis = System.currentTimeMillis()
                delay(30_000L)
            }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = fontScale.coerceIn(0.85f, 1.35f)
        )
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            if (!isUnlocked) {
                LockScreen(activity = activity)
                return@MaterialTheme
            }

            if (!firstRunDone) {
                OnboardingDialog()
            }

            Scaffold(
                containerColor = colorScheme.background,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFF0F172A),
                            contentColor = Color.White,
                            actionColor = Color(0xFF34D399),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(primaryCustom, primaryCustom.copy(alpha = 0.65f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Smartphone,
                                        contentDescription = "شعار العمل",
                                        tint = Color.White.copy(alpha = 0.28f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "DH",
                                        color = Color.White,
                                        fontSize = 11.5f.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("العمل", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                        Surface(
                                            color = primaryCustom.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "v4.5",
                                                fontSize = 10.sp,
                                                color = primaryCustom,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (statusBanner.isNotBlank()) {
                                        Text(
                                            statusBanner,
                                            fontSize = 11.sp,
                                            color = if (statusBanner.startsWith("⚠️") || statusBanner.startsWith("❌")) Color(0xFFEF4444) else primaryCustom,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (deletedEntries.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = { undoLastDelete() },
                                    modifier = Modifier.padding(end = 8.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = "تراجع عن الحذف", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تراجع (${deletedEntries.size})", fontSize = 12.sp)
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
                                    Icon(Icons.Default.AccessTime, contentDescription = "تبويب الدفتر")
                                }
                            },
                            label = {
                                Text(
                                    "الدفتر",
                                    fontWeight = if (currentTab == 0) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "تبويب الدفعات") },
                            label = {
                                Text(
                                    "الدفعات",
                                    fontWeight = if (currentTab == 1) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "تبويب التقارير") },
                            label = {
                                Text(
                                    "التقارير",
                                    fontWeight = if (currentTab == 2) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "تبويب الإعدادات") },
                            label = {
                                Text(
                                    "الإعدادات",
                                    fontWeight = if (currentTab == 3) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
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
                                3 -> SettingsTab(activity = activity)
                            }
                        }
                    }
                }
            }

            AppDialogs()
        }
    }
}
