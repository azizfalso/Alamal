package com.nanomks.elamal.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material3.ColorScheme
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Clean ViewModel managing all UI states and executing all database/backup operations off the UI code.
 */
class AppState(private val app: Application) : AndroidViewModel(app) {

    val prefs: SharedPreferences = app.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
    val dao: ElamalDao = ElamalDatabase.getInstance(app).dao()

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
    var prefsRefreshCounter by mutableIntStateOf(0)
    var activeClockIn by mutableLongStateOf(prefs.getLong("active_clock_in", 0L))
    var widgetEnabled by mutableStateOf(prefs.getBoolean("widget_enabled", true))
    var rootWifiEnabled by mutableStateOf(prefs.getBoolean("root_wifi_enabled", true))
    var searchFilterEnabled by mutableStateOf(prefs.getBoolean("search_filter_enabled", true))
    var forgotAlarmHours by mutableIntStateOf(prefs.getInt("forgot_alarm_hours", 12))
    var workSsids by mutableStateOf(prefs.getString("work_ssids", "") ?: "")
    var backupUriStr by mutableStateOf(prefs.getString("backup_uri", "") ?: "")
    var firstRunDone by mutableStateOf(prefs.getBoolean("first_run_done", false))

    var savedPin by mutableStateOf(prefs.getString("security_pin", "") ?: "")
    var isUnlocked by mutableStateOf(savedPin.isBlank())
    var failedPinAttempts by mutableIntStateOf(prefs.getInt("failed_pin_attempts", 0))
    var lockoutUntil by mutableLongStateOf(prefs.getLong("lockout_until", 0L))

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
    var showManualHourlyDialog by mutableStateOf(false)
    var showRemoteDialog by mutableStateOf(false)
    var showCreditDialog by mutableStateOf(false)
    var showSettleDialog by mutableStateOf(false)
    var showCustomPeriodPdfDialog by mutableStateOf(false)
    var editingEntry by mutableStateOf<WorkEntry?>(null)
    var editingSettlement by mutableStateOf<Settlement?>(null)
    var confirmIgnoreAllWifi by mutableStateOf(false)
    var confirmRestoreFileName by mutableStateOf<String?>(null)

    var overlapWarningMessage by mutableStateOf<String?>(null)
    var pendingForcedAction by mutableStateOf<(() -> Unit)?>(null)

    private val initCal = Calendar.getInstance()
    var calendarYear by mutableIntStateOf(initCal.get(Calendar.YEAR))
    var calendarMonth by mutableIntStateOf(initCal.get(Calendar.MONTH))
    var selectedCalendarDay by mutableIntStateOf(-1)

    var nowMillis by mutableLongStateOf(System.currentTimeMillis())
    var detectedRootSsid by mutableStateOf<String?>(null)

    fun refreshPrefsState() {
        hourlyRate = prefs.getFloat("hourly_rate", 10.0f).toDouble()
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
        ElamalUtils.scheduleForgotClockOutAlarm(app, now)
        ElamalUtils.showOrUpdateOngoingWorkNotification(app, now, hourlyRate)
        ElamalWidgetReceiver.refreshAllWidgets(app)
        Toast.makeText(app, "بدأ حساب ساعات العمل ✓", Toast.LENGTH_SHORT).show()
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
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            showClockOutDialog = false
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
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            showManualHourlyDialog = false
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
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
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
        }
    }

    fun saveFixedEntry(type: EntryType, signedAmount: Double, note: String) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.insertEntry(
                WorkEntry(
                    type = type,
                    startTime = now,
                    endTime = now,
                    hourlyRateAtEntry = hourlyRate,
                    calculatedAmount = signedAmount,
                    note = note
                )
            )
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            showRemoteDialog = false
            showCreditDialog = false
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
                val signedAmt = if (target.type == EntryType.CREDIT) -rawAmount else rawAmount
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
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            editingEntry = null
        }
    }

    fun softDeleteEntry(item: WorkEntry) {
        lastDeletedEntry = item
        viewModelScope.launch {
            dao.updateEntry(item.copy(isDeleted = true))
            if (item.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
        }
    }

    fun undoLastDelete() {
        val toRestore = lastDeletedEntry ?: return
        viewModelScope.launch {
            dao.updateEntry(toRestore.copy(isDeleted = false))
            if (toRestore.settlementId != null) {
                dao.recalculateSettlementsCascade()
            }
            lastDeletedEntry = null
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            Toast.makeText(app, "تم التراجع عن الحذف ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun createNewSettlement(entriesSum: Double, prevCarry: Double, paid: Double, note: String) {
        val nextCarry = (entriesSum + prevCarry) - paid
        viewModelScope.launch {
            val sId = dao.insertSettlement(
                Settlement(
                    timestamp = System.currentTimeMillis(),
                    entriesTotal = entriesSum,
                    previousCarryOver = prevCarry,
                    amountPaid = paid,
                    nextCarryOver = nextCarry,
                    note = note
                )
            )
            dao.attachEntriesToSettlement(sId)
            dao.recalculateSettlementsCascade()
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            showSettleDialog = false
        }
    }

    fun saveEditedSettlement(target: Settlement, newTimestamp: Long, newPaid: Double, newNote: String) {
        viewModelScope.launch {
            dao.updateSettlement(target.copy(timestamp = newTimestamp, amountPaid = newPaid, note = newNote))
            dao.recalculateSettlementsCascade()
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            editingSettlement = null
        }
    }

    fun undoLatestSettlement() {
        val latest = settlements.firstOrNull() ?: return
        viewModelScope.launch {
            dao.detachEntriesFromSettlement(latest.id)
            dao.deleteSettlement(latest)
            dao.recalculateSettlementsCascade()
            statusBanner = BackupSafManager.performSmartBackup(app, dao)
            Toast.makeText(app, "تم التراجع عن آخر دفعة وإعادة السجلات للدفتر ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun performConfirmedRestore(fileName: String) {
        viewModelScope.launch {
            val msg = BackupSafManager.restoreFromBackup(app, dao, fileName)
            refreshPrefsState()
            statusBanner = msg
            confirmRestoreFileName = null
            Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun exportPdfReportAsync(title: String, reportEntries: List<WorkEntry>, reportSettlements: List<Settlement>) {
        viewModelScope.launch(Dispatchers.IO) {
            PdfReportGenerator.generateAndSharePdf(app, title, reportEntries, reportSettlements, isDark)
        }
    }

    fun exportCsvAsync(reportEntries: List<WorkEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            PdfReportGenerator.exportAndShareCsv(app, reportEntries)
        }
    }
}



