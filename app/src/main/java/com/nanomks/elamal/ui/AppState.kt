package com.nanomks.elamal.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.data.*
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.util.PdfReportGenerator
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class AppState(private val app: Application) : AndroidViewModel(app) {

    val prefs: SharedPreferences = app.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
    val dao: ElamalDao = ElamalDatabase.getInstance(app).dao()
    val snackbarHostState = SnackbarHostState()

    var sysDark by mutableStateOf(false)
    var themeMode by mutableIntStateOf(prefs.getInt("theme_mode", 0))
    val isDark: Boolean
        get() = when (themeMode) {
            1 -> false
            2 -> true
            else -> sysDark
        }

    var pdfDarkMode by mutableStateOf(prefs.getBoolean("pdf_dark_mode", false))
    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic_color", false))
    var appColorHex by mutableStateOf(prefs.getString("app_color_hex", "#059669") ?: "#059669")
    var syncIconWithColor by mutableStateOf(prefs.getBoolean("sync_icon_with_color", true))
    var activeIconAlias by mutableStateOf(
        prefs.getString("active_icon_alias", "com.nanomks.elamal.LauncherDefault")
            ?: "com.nanomks.elamal.LauncherDefault"
    )
    var fontScale by mutableFloatStateOf(prefs.getFloat("font_scale", 1.0f))
    var useArabicDigits by mutableStateOf(prefs.getBoolean("use_arabic_digits", false))
    var currencySymbol by mutableStateOf(prefs.getString("currency_symbol", "DH") ?: "DH")

    var hourlyRate by mutableDoubleStateOf(prefs.getFloat("hourly_rate", 10.0f).toDouble())
    var prefsRefreshCounter by mutableIntStateOf(0)
    var activeClockIn by mutableLongStateOf(prefs.getLong("active_clock_in", 0L))
    var widgetEnabled by mutableStateOf(prefs.getBoolean("widget_enabled", true))
    var rootWifiEnabled by mutableStateOf(prefs.getBoolean("root_wifi_enabled", true))
    var wifiUseRootSu by mutableStateOf(prefs.getBoolean("wifi_use_root_su", true))
    var wifiWakeAlarm by mutableStateOf(prefs.getBoolean("wifi_wake_alarm", true))
    var searchFilterEnabled by mutableStateOf(prefs.getBoolean("search_filter_enabled", true))
    var forgotAlarmHours by mutableIntStateOf(prefs.getInt("forgot_alarm_hours", 12))
    var workSsids by mutableStateOf(prefs.getString("work_ssids", "") ?: "")
    var backupUriStr by mutableStateOf(prefs.getString("backup_uri", "") ?: "")
    var firstRunDone by mutableStateOf(prefs.getBoolean("first_run_done", false))

    var savedPin by mutableStateOf(prefs.getString("security_pin", "") ?: "")
    var isUnlocked by mutableStateOf(savedPin.isBlank())
    var backgroundElapsedRealtime by mutableLongStateOf(0L)
    var failedPinAttempts by mutableIntStateOf(prefs.getInt("failed_pin_attempts", 0))
    var lockoutUntil by mutableLongStateOf(prefs.getLong("lockout_until", 0L))
    var flagSecureEnabled by mutableStateOf(prefs.getBoolean("flag_secure_enabled", savedPin.isNotBlank()))

    var primaryCustom: Color = Color(0xFF059669)
    var colorScheme: ColorScheme = lightColorScheme()
    lateinit var folderPicker: ManagedActivityResultLauncher<Uri?, Uri?>

    var entries by mutableStateOf<List<WorkEntry>>(emptyList())
        private set
    var allEntries by mutableStateOf<List<WorkEntry>>(emptyList())
        private set
    var deletedEntries by mutableStateOf<List<WorkEntry>>(emptyList())
        private set
    var settlements by mutableStateOf<List<Settlement>>(emptyList())
        private set
    var wifiSessions by mutableStateOf<List<WifiSession>>(emptyList())
        private set

    var lastSoftDeleteMillis by mutableLongStateOf(0L)

    val pendingWifiList: List<WifiSession>
        get() = wifiSessions.filter { it.status == "PENDING" }

    var currentTab by mutableIntStateOf(0)
    var searchQuery by mutableStateOf("")
    var filterType by mutableStateOf<EntryType?>(null)
    var statusBanner by mutableStateOf("")

    var selectedReportScope by mutableIntStateOf(0)
    var customReportStartMillis by mutableLongStateOf(System.currentTimeMillis() - 14 * 86400_000L)
    var customReportEndMillis by mutableLongStateOf(System.currentTimeMillis())

    var showClockOutDialog by mutableStateOf(false)
    var showEditActiveClockInDialog by mutableStateOf(false)
    var showManualHourlyDialog by mutableStateOf(false)
    var showRemoteDialog by mutableStateOf(false)
    var showCreditDialog by mutableStateOf(false)
    var showSettleDialog by mutableStateOf(false)
    var showCustomPeriodPdfDialog by mutableStateOf(false)
    var showCustomColorPickerDialog by mutableStateOf(false)
    var showPinSetupDialog by mutableStateOf(false)
    var showTrashBinDialog by mutableStateOf(false)

    var editingEntry by mutableStateOf<WorkEntry?>(null)
    var pendingDeleteEntry by mutableStateOf<WorkEntry?>(null)
    var editingSettlement by mutableStateOf<Settlement?>(null)
    var confirmUndoLatestSettlement by mutableStateOf(false)
    var confirmIgnoreAllWifi by mutableStateOf(false)
    var confirmRestoreFileName by mutableStateOf<String?>(null)

    var overlapWarningMessage by mutableStateOf<String?>(null)
    var pendingForcedAction by mutableStateOf<(() -> Unit)?>(null)

    private val initCal = Calendar.getInstance()
    var calendarYear by mutableIntStateOf(initCal.get(Calendar.YEAR))
    var calendarMonth by mutableIntStateOf(initCal.get(Calendar.MONTH))
    var selectedCalendarDay by mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH))

    var nowMillis by mutableLongStateOf(System.currentTimeMillis())
    var detectedRootSsid by mutableStateOf<String?>(null)

    private var autoBackupJob: Job? = null

    init {
        ElamalUtils.useArabicDigits = useArabicDigits
        ElamalUtils.currencySymbol = currencySymbol
        viewModelScope.launch {
            dao.getActiveEntries().collectLatest { entries = it }
        }
        viewModelScope.launch {
            dao.getAllNonDeletedEntriesFlow().collectLatest { allEntries = it }
        }
        viewModelScope.launch {
            dao.getDeletedEntriesFlow().collectLatest {
                deletedEntries = it
                val newestDelTime = it.firstOrNull()?.deletedAt ?: 0L
                if (newestDelTime > lastSoftDeleteMillis) {
                    lastSoftDeleteMillis = newestDelTime
                }
            }
        }
        viewModelScope.launch {
            dao.getAllSettlements().collectLatest { settlements = it }
        }
        viewModelScope.launch {
            dao.getVisibleWifiSessions().collectLatest { wifiSessions = it }
        }
    }

    fun onAppBackgrounded() {
        if (savedPin.isNotBlank() && isUnlocked) {
            backgroundElapsedRealtime = SystemClock.elapsedRealtime()
        }
    }

    fun onAppForegrounded() {
        if (savedPin.isNotBlank() && isUnlocked && backgroundElapsedRealtime > 0L) {
            val awayMs = SystemClock.elapsedRealtime() - backgroundElapsedRealtime
            if (awayMs >= 60_000L) {
                isUnlocked = false
            }
            backgroundElapsedRealtime = 0L
        }
    }

    fun updateCurrencySymbol(newSymbol: String) {
        val clean = newSymbol.trim().take(6).ifEmpty { "DH" }
        currencySymbol = clean
        ElamalUtils.currencySymbol = clean
        prefs.edit().putString("currency_symbol", clean).apply()
        prefsRefreshCounter += 1
    }

    fun toggleArabicDigits(enabled: Boolean) {
        useArabicDigits = enabled
        ElamalUtils.useArabicDigits = enabled
        prefs.edit().putBoolean("use_arabic_digits", enabled).apply()
        prefsRefreshCounter += 1
    }

    fun showNotice(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        viewModelScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun scheduleSmartBackupDebounced() {
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            delay(3_500L)
            val msg = BackupSafManager.performSmartBackup(app, dao, isManual = false)
            statusBanner = msg
            if (msg.startsWith("⚠️") || msg.startsWith("❌")) {
                showNotice(msg)
            }
        }
    }

    fun updateAppColor(newHex: String) {
        val cleanHex = if (newHex.startsWith("#")) newHex.uppercase() else "#${newHex.uppercase()}"
        appColorHex = cleanHex
        prefs.edit().putString("app_color_hex", cleanHex).apply()
        if (syncIconWithColor) {
            val matchedAlias = ElamalUtils.findClosestIconAlias(cleanHex)
            activeIconAlias = matchedAlias
            prefs.edit().putString("active_icon_alias", matchedAlias).apply()
            ElamalUtils.applyLauncherAlias(app, matchedAlias)
        }
    }

    fun toggleSyncIconWithColor(enabled: Boolean) {
        syncIconWithColor = enabled
        prefs.edit().putBoolean("sync_icon_with_color", enabled).apply()
        if (enabled) {
            val matchedAlias = ElamalUtils.findClosestIconAlias(appColorHex)
            activeIconAlias = matchedAlias
            prefs.edit().putString("active_icon_alias", matchedAlias).apply()
            ElamalUtils.applyLauncherAlias(app, matchedAlias)
        }
    }

    fun refreshPrefsState() {
        hourlyRate = prefs.getFloat("hourly_rate", 10.0f).toDouble()
        currencySymbol = prefs.getString("currency_symbol", "DH") ?: "DH"
        ElamalUtils.currencySymbol = currencySymbol
        activeClockIn = prefs.getLong("active_clock_in", 0L)
        workSsids = prefs.getString("work_ssids", "") ?: ""
        prefsRefreshCounter += 1
        ElamalUtils.syncPeriodicWifiPollSchedule(app)
    }

    fun navigateCalendarMonth(delta: Int) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        calendarYear = c.get(Calendar.YEAR)
        calendarMonth = c.get(Calendar.MONTH)
        selectedCalendarDay = -1
    }

    fun startClockIn() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("active_clock_in", now).apply()
        activeClockIn = now
        nowMillis = now
        ElamalUtils.scheduleForgotClockOutAlarm(app, now)
        ElamalUtils.showOrUpdateOngoingWorkNotification(app, now, hourlyRate)
        ElamalWidgetReceiver.refreshAllWidgets(app)
        showNotice("بدأ حساب ساعات العمل ✓")
    }

    fun updateActiveClockInStart(newStartMillis: Long) {
        prefs.edit().putLong("active_clock_in", newStartMillis).apply()
        activeClockIn = newStartMillis
        nowMillis = System.currentTimeMillis()
        ElamalUtils.scheduleForgotClockOutAlarm(app, newStartMillis)
        ElamalUtils.showOrUpdateOngoingWorkNotification(app, newStartMillis, hourlyRate)
        ElamalWidgetReceiver.refreshAllWidgets(app)
        showNotice("تم تحديث وقت بداية الجلسة الجارية ✓")
    }

    fun completeClockOut(breakMin: Int, note: String) {
        val start = activeClockIn
        val end = System.currentTimeMillis()
        if (start <= 0L) return
        val pay = ElamalUtils.calculateHourlyPay(start, end, breakMin, hourlyRate)
        viewModelScope.launch {
            dao.insertEntry(
                WorkEntry(
                    type = EntryType.HOURLY,
                    startTime = start,
                    endTime = end,
                    breakMinutes = breakMin,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = pay,
                    note = note
                )
            )
            prefs.edit().putLong("active_clock_in", 0L).apply()
            activeClockIn = 0L
            ElamalUtils.cancelForgotClockOutAlarm(app)
            ElamalUtils.cancelOngoingWorkNotification(app)
            ElamalWidgetReceiver.refreshAllWidgets(app)
            showClockOutDialog = false
            showNotice("تم حفظ جلسة العمل بنجاح ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun saveManualHourlyEntry(startMillis: Long, endMillis: Long, breakMin: Int, note: String, forceIgnoreOverlap: Boolean) {
        viewModelScope.launch {
            if (!forceIgnoreOverlap) {
                val overlaps = dao.findOverlappingHourlyEntries(startMillis, endMillis)
                if (overlaps.isNotEmpty()) {
                    overlapWarningMessage = "هذه الفترة تتقاطع زمنياً مع سجل ساعات موجود مسبقاً (${ElamalUtils.formatDateTime(overlaps.first().startTime)}). هل تريد إضافتها رغم التداخل؟"
                    pendingForcedAction = { saveManualHourlyEntry(startMillis, endMillis, breakMin, note, true) }
                    return@launch
                }
            }
            val pay = ElamalUtils.calculateHourlyPay(startMillis, endMillis, breakMin, hourlyRate)
            dao.insertEntry(
                WorkEntry(
                    type = EntryType.HOURLY,
                    startTime = startMillis,
                    endTime = endMillis,
                    breakMinutes = breakMin,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = pay,
                    note = note
                )
            )
            showManualHourlyDialog = false
            showNotice("تمت إضافة سجل الساعات ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun approveWifiSession(ws: WifiSession, forceIgnoreOverlap: Boolean = false) {
        viewModelScope.launch {
            if (!forceIgnoreOverlap) {
                val overlaps = dao.findOverlappingHourlyEntries(ws.startTime, ws.endTime)
                if (overlaps.isNotEmpty()) {
                    overlapWarningMessage = "فترة الواي فاي (${ws.ssid}) تتقاطع مع سجل ساعات مسجل يدوياً في نفس الوقت! هل تريد اعتمادها رغم ذلك؟"
                    pendingForcedAction = { approveWifiSession(ws, true) }
                    return@launch
                }
            }
            val pay = ElamalUtils.calculateHourlyPay(ws.startTime, ws.endTime, 0, hourlyRate)
            dao.insertEntry(
                WorkEntry(
                    type = EntryType.HOURLY,
                    startTime = ws.startTime,
                    endTime = ws.endTime,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = pay,
                    note = "حضور واي فاي (${ws.ssid})"
                )
            )
            dao.updateWifiSession(ws.copy(status = "ADDED"))
            showNotice("تم اعتماد فترة الواي فاي ✓")
            scheduleSmartBackupDebounced()
        }
    }

    /**
     * Smart same-day Wi-Fi sessions merge:
     * Combines multiple pending Wi-Fi sessions from the same day into a single work shift
     * and automatically calculates the disconnection gaps as breakMinutes!
     */
    fun mergeAndApproveDayWifiSessions(daySessions: List<WifiSession>, forceIgnoreOverlap: Boolean = false) {
        if (daySessions.size < 2) return
        val sorted = daySessions.sortedBy { it.startTime }
        val firstStart = sorted.first().startTime
        val lastEnd = sorted.maxOf { it.endTime }
        var totalGapMillis = 0L
        for (i in 1 until sorted.size) {
            val gap = (sorted[i].startTime - sorted[i - 1].endTime).coerceAtLeast(0L)
            totalGapMillis += gap
        }
        val autoBreakMins = (totalGapMillis / 60_000L).toInt().coerceAtLeast(0)
        val combinedSsids = sorted.map { it.ssid }.distinct().joinToString("+")

        viewModelScope.launch {
            if (!forceIgnoreOverlap) {
                val overlaps = dao.findOverlappingHourlyEntries(firstStart, lastEnd)
                if (overlaps.isNotEmpty()) {
                    overlapWarningMessage = "الفترة المدمجة تتقاطع مع سجل ساعات موجود مسبقاً! هل تريد اعتمادها رغم التداخل؟"
                    pendingForcedAction = { mergeAndApproveDayWifiSessions(daySessions, true) }
                    return@launch
                }
            }
            val pay = ElamalUtils.calculateHourlyPay(firstStart, lastEnd, autoBreakMins, hourlyRate)
            dao.insertEntry(
                WorkEntry(
                    type = EntryType.HOURLY,
                    startTime = firstStart,
                    endTime = lastEnd,
                    breakMinutes = autoBreakMins,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = pay,
                    note = "حضور واي فاي مدمج ($combinedSsids)"
                )
            )
            for (s in sorted) {
                dao.updateWifiSession(s.copy(status = "ADDED"))
            }
            showNotice("تم دمج ${sorted.size} فترات (استراحة ${autoBreakMins}د) واعتمادها ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun ignoreWifiSession(ws: WifiSession) {
        viewModelScope.launch {
            dao.updateWifiSession(ws.copy(status = "IGNORED"))
        }
    }

    fun ignoreAllPendingWifiSessions() {
        viewModelScope.launch {
            dao.ignoreAllPendingWifi()
            confirmIgnoreAllWifi = false
            showNotice("تم تجاهل الفترات المقترحة")
        }
    }

    fun saveFixedEntry(type: EntryType, signedAmount: Double, note: String, customTimestamp: Long = System.currentTimeMillis()) {
        val roundedAmount = ElamalUtils.roundMoney(signedAmount)
        viewModelScope.launch {
            dao.insertEntry(
                WorkEntry(
                    type = type,
                    startTime = customTimestamp,
                    endTime = customTimestamp,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = roundedAmount,
                    note = note
                )
            )
            showRemoteDialog = false
            showCreditDialog = false
            showNotice("تمت إضافة السجل للدفتر ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun saveEditedEntry(
        target: WorkEntry,
        newStart: Long,
        newEnd: Long,
        newBreak: Int,
        rawAmount: Double,
        newNote: String,
        forceIgnoreOverlap: Boolean
    ) {
        viewModelScope.launch {
            if (target.type == EntryType.HOURLY && !forceIgnoreOverlap) {
                val overlaps = dao.findOverlappingHourlyEntries(newStart, newEnd, excludeId = target.id)
                if (overlaps.isNotEmpty()) {
                    overlapWarningMessage = "الوقت المعدّل يتقاطع مع سجل ساعات آخر في نفس الفترة. هل تريد الحفظ على أي حال؟"
                    pendingForcedAction = { saveEditedEntry(target, newStart, newEnd, newBreak, rawAmount, newNote, true) }
                    return@launch
                }
            }
            val updated = if (target.type == EntryType.HOURLY) {
                val recalc = ElamalUtils.calculateHourlyPay(newStart, newEnd, newBreak, target.hourlyRateAtEntry)
                target.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    breakMinutes = newBreak,
                    calculatedAmount = recalc,
                    note = newNote
                )
            } else {
                val roundedRaw = ElamalUtils.roundMoney(rawAmount)
                val signedAmt = if (target.type == EntryType.CREDIT) -roundedRaw else roundedRaw
                target.copy(
                    startTime = newStart,
                    endTime = newStart,
                    calculatedAmount = signedAmt,
                    note = newNote
                )
            }
            dao.updateEntry(updated)
            if (target.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            editingEntry = null
            showNotice("تم حفظ التعديلات ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun requestDeleteEntry(item: WorkEntry) {
        pendingDeleteEntry = item
    }

    fun confirmSoftDeleteEntry() {
        val item = pendingDeleteEntry ?: return
        pendingDeleteEntry = null
        val delTime = System.currentTimeMillis()
        lastSoftDeleteMillis = delTime
        viewModelScope.launch {
            dao.updateEntry(item.copy(isDeleted = true, deletedAt = delTime))
            if (item.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            scheduleSmartBackupDebounced()
            showNotice(
                message = "تم نقل السجل إلى سلة المحذوفات",
                actionLabel = "تراجع",
                onAction = { restoreDeletedEntry(item) }
            )
        }
    }

    fun undoLastDelete() {
        val toRestore = deletedEntries.firstOrNull() ?: return
        restoreDeletedEntry(toRestore)
    }

    fun restoreDeletedEntry(item: WorkEntry) {
        viewModelScope.launch {
            dao.updateEntry(item.copy(isDeleted = false, deletedAt = null))
            if (item.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            scheduleSmartBackupDebounced()
            showNotice("تم استرجاع السجل بنجاح ✓")
        }
    }

    fun deleteEntryForever(item: WorkEntry) {
        viewModelScope.launch {
            dao.deleteEntryPermanently(item)
            if (item.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            showNotice("تم حذف السجل نهائياً")
        }
    }

    fun emptyTrashBinPermanently() {
        viewModelScope.launch {
            dao.purgeSoftDeletedEntries()
            lastSoftDeleteMillis = 0L
            showTrashBinDialog = false
            showNotice("تم إفراغ سلة المحذوفات نهائياً ✓")
        }
    }

    fun createNewSettlement(entriesSum: Double, prevCarry: Double, paid: Double, note: String) {
        val roundedEntriesSum = ElamalUtils.roundMoney(entriesSum)
        val roundedPrevCarry = ElamalUtils.roundMoney(prevCarry)
        val roundedPaid = ElamalUtils.roundMoney(paid)
        val nextCarry = ElamalUtils.roundMoney((roundedEntriesSum + roundedPrevCarry) - roundedPaid)

        viewModelScope.launch {
            val sId = dao.insertSettlement(
                Settlement(
                    timestamp = System.currentTimeMillis(),
                    entriesTotal = roundedEntriesSum,
                    previousCarryOver = roundedPrevCarry,
                    amountPaid = roundedPaid,
                    nextCarryOver = nextCarry,
                    note = note
                )
            )
            dao.attachEntriesToSettlement(sId)
            dao.recalculateSettlementsCascade()
            showSettleDialog = false
            showNotice("تم استلام الدفعة وأرشفة السجلات ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun saveEditedSettlement(target: Settlement, newTimestamp: Long, newPaid: Double, newNote: String) {
        val roundedPaid = ElamalUtils.roundMoney(newPaid)
        viewModelScope.launch {
            dao.updateSettlement(target.copy(timestamp = newTimestamp, amountPaid = roundedPaid, note = newNote))
            dao.recalculateSettlementsCascade()
            editingSettlement = null
            showNotice("تم تحديث الدفعة وإعادة حساب المرحّل ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun performConfirmedUndoLatestSettlement() {
        val latest = settlements.firstOrNull() ?: return
        confirmUndoLatestSettlement = false
        viewModelScope.launch {
            dao.detachEntriesFromSettlement(latest.id)
            dao.deleteSettlement(latest)
            dao.recalculateSettlementsCascade()
            showNotice("تم التراجع عن آخر دفعة وإعادة سجلاتها للدفتر ✓")
            scheduleSmartBackupDebounced()
        }
    }

    fun performConfirmedRestore(fileName: String) {
        viewModelScope.launch {
            val msg = BackupSafManager.restoreFromBackup(app, dao, fileName)
            refreshPrefsState()
            statusBanner = msg
            confirmRestoreFileName = null
            showNotice(msg)
        }
    }

    fun exportPdfReportAsync(
        title: String,
        reportEntries: List<WorkEntry>,
        reportSettlements: List<Settlement>,
        carryOver: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            PdfReportGenerator.generateAndSharePdf(app, title, reportEntries, reportSettlements, pdfDarkMode, carryOver)
        }
    }

    fun exportCsvAsync(reportEntries: List<WorkEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            PdfReportGenerator.exportAndShareCsv(app, reportEntries)
        }
    }
}
