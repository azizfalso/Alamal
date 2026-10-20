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

/** Holds all screen state. Tabs/dialogs are extension functions on this class. */
class AppState(
    val activity: FragmentActivity,
    val context: Context,
    val prefs: SharedPreferences,
    val dao: ElamalDao,
    val scope: CoroutineScope
) {
    var sysDark: Boolean = false
    var themeMode by mutableIntStateOf(prefs.getInt("theme_mode", 0))
    val isDark: Boolean
        get() = when (themeMode) {
            1 -> false
            2 -> true
            else -> sysDark
        }
    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic_color", false))
    var appColorHex by mutableStateOf(prefs.getString("app_color_hex", "#059669") ?: "#059669")
    var fontScale by mutableFloatStateOf(prefs.getFloat("font_scale", 1.0f))

    var hourlyRate by mutableDoubleStateOf(prefs.getFloat("hourly_rate", 10.0f).toDouble())
    var activeClockIn by mutableLongStateOf(prefs.getLong("active_clock_in", 0L))
    var widgetEnabled by mutableStateOf(prefs.getBoolean("widget_enabled", true))
    var searchFilterEnabled by mutableStateOf(prefs.getBoolean("search_filter_enabled", true))
    var forgotAlarmHours by mutableIntStateOf(prefs.getInt("forgot_alarm_hours", 12))
    var workSsids by mutableStateOf(prefs.getString("work_ssids", "") ?: "")
    var backupUriStr by mutableStateOf(prefs.getString("backup_uri", "") ?: "")
    var firstRunDone by mutableStateOf(prefs.getBoolean("first_run_done", false))

    var savedPin by mutableStateOf(prefs.getString("security_pin", "") ?: "")
    var isUnlocked by mutableStateOf(savedPin.isBlank())
    var failedPinAttempts by mutableIntStateOf(prefs.getInt("failed_pin_attempts", 0))
    var lockoutUntil by mutableLongStateOf(prefs.getLong("lockout_until", 0L))

    // Set on every composition by RootContent
    var primaryCustom: Color = Color(0xFF059669)
    var colorScheme: ColorScheme = lightColorScheme()
    lateinit var folderPicker: ManagedActivityResultLauncher<Uri?, Uri?>
    var entries: List<WorkEntry> = emptyList()
    var allEntries: List<WorkEntry> = emptyList()
    var settlements: List<Settlement> = emptyList()
    var wifiSessions: List<WifiSession> = emptyList()
    val pendingWifiList: List<WifiSession>
        get() = wifiSessions.filter { it.status == "PENDING" }

    var currentTab by mutableIntStateOf(0)
    var searchQuery by mutableStateOf("")
    var filterType by mutableStateOf<EntryType?>(null)
    var lastDeletedEntry by mutableStateOf<WorkEntry?>(null)
    var statusBanner by mutableStateOf("")

    var showClockOutDialog by mutableStateOf(false)
    var showRemoteDialog by mutableStateOf(false)
    var showCreditDialog by mutableStateOf(false)
    var showSettleDialog by mutableStateOf(false)
    var showCustomPeriodPdfDialog by mutableStateOf(false)
    var editingEntry by mutableStateOf<WorkEntry?>(null)
    var editingSettlement by mutableStateOf<Settlement?>(null)
    var confirmIgnoreAllWifi by mutableStateOf(false)

    var nowMillis by mutableLongStateOf(System.currentTimeMillis())
    var detectedRootSsid by mutableStateOf<String?>(null)
}
