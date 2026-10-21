package com.nanomks.elamal.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.nanomks.elamal.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupSafManager {

    private val backupMutex = Mutex()
    private const val MAX_MANUAL_BACKUPS_TO_KEEP = 10

    fun formatBackupFileLabel(fileName: String): String {
        return when {
            fileName == "elamal_auto_backup.json" -> "🔄 النسخة التلقائية الأساسية"
            fileName.startsWith("elamal_snap_") -> {
                val idx = fileName.removePrefix("elamal_snap_").removeSuffix(".json")
                "📅 نسخة يومية سابقة ($idx)"
            }
            fileName.startsWith("elamal_safety_slot_") -> {
                val idx = fileName.removePrefix("elamal_safety_slot_").removeSuffix(".json")
                "🛡️ نسخة أمان قبل نقص بيانات ($idx)"
            }
            fileName.startsWith("elamal_manual_") -> {
                val ts = fileName.removePrefix("elamal_manual_").removeSuffix(".json").toLongOrNull()
                val dateStr = if (ts != null) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ts)) else ""
                "💾 نسخة يدوية ($dateStr)"
            }
            else -> fileName
        }
    }

    suspend fun performSmartBackup(
        context: Context,
        dao: ElamalDao,
        isManual: Boolean = false
    ): String = backupMutex.withLock {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
            val uriStr = prefs.getString("backup_uri", "") ?: ""
            if (uriStr.isBlank()) return@withContext "اختر مجلد النسخ الاحتياطي من الإعدادات لحفظ نسخك تلقائياً"

            val treeUri = Uri.parse(uriStr)
            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext "تعذر الوصول إلى مجلد النسخ"

            val encEnabled = prefs.getBoolean("backup_encrypt_enabled", false)
            val encPass = ElamalSecurity.getBackupPassword(prefs)
            if (encEnabled) {
                if (ElamalSecurity.hasKeystoreDecryptionError(prefs)) {
                    return@withContext "⚠️ خطأ أمان: تعذر قراءة كلمة المرور من Keystore! أعد إدخال كلمة مرور النسخ في الإعدادات (توقف النسخ لحماية بياناتك)"
                }
                if (encPass.isBlank()) {
                    return@withContext "⚠️ تنبيه: التشفير مفعّل لكن كلمة المرور فارغة! أدخل كلمة مرور التشفير في الإعدادات أولاً"
                }
            }

            val allEntries = dao.getAllEntriesIncludingDeletedSync()
            val activeNonDeletedCount = allEntries.count { !it.isDeleted }
            val settlements = dao.getSettlementsAsc()
            val wifiSessions = dao.getAllWifiSessionsSync()
            val activeClockIn = prefs.getLong("active_clock_in", 0L)

            val prevEntriesCount = prefs.getInt("last_backup_entries_count", 0)
            val prevSettlementsCount = prefs.getInt("last_backup_settlements_count", 0)
            val prevHadClockIn = prefs.getBoolean("last_backup_had_clockin", false)

            if (activeNonDeletedCount == 0 && settlements.isEmpty() && activeClockIn == 0L && (prevEntriesCount > 0 || prevSettlementsCount > 0)) {
                return@withContext "حماية تلقائية: تم منع الكتابة بنسخة فارغة فوق نسخة تحتوي على بيانات!"
            }

            val dataDecreased = (activeNonDeletedCount < prevEntriesCount) ||
                    (settlements.size < prevSettlementsCount) ||
                    (prevHadClockIn && activeClockIn == 0L && activeNonDeletedCount <= prevEntriesCount)

            val mainFile = dir.findFile("elamal_auto_backup.json")
            if (!isManual && mainFile != null) {
                val prevContent = readUriText(context, mainFile.uri)
                if (prevContent.isNotBlank()) {
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    if (prefs.getString("last_snap_day", "") != todayKey) {
                        rotateDailySnapshots(context, dir, prevContent)
                        prefs.edit().putString("last_snap_day", todayKey).apply()
                    }
                    if (dataDecreased) {
                        val slot = (prefs.getInt("safety_slot_index", 0) % 5) + 1
                        prefs.edit().putInt("safety_slot_index", slot).apply()
                        val safetyName = "elamal_safety_slot_${slot}.json"
                        writeToDocumentFileFast(context, dir, safetyName, prevContent)
                    }
                }
            }

            val root = JSONObject()
            root.put("version", 6)
            root.put("timestamp", System.currentTimeMillis())
            root.put("activeClockIn", activeClockIn)
            root.put("hourlyRate", prefs.getFloat("hourly_rate", 10f).toDouble())
            root.put("currencySymbol", prefs.getString("currency_symbol", "DH") ?: "DH")
            root.put("workSsids", prefs.getString("work_ssids", "") ?: "")

            val entriesArr = JSONArray()
            for (e in allEntries) {
                val obj = JSONObject().apply {
                    put("id", e.id)
                    put("type", e.type.name)
                    put("startTime", e.startTime)
                    put("endTime", e.endTime ?: -1L)
                    put("breakMinutes", e.breakMinutes)
                    put("hourlyRateAtEntry", e.hourlyRateAtEntry)
                    put("calculatedAmount", e.calculatedAmount)
                    put("note", e.note)
                    put("settlementId", e.settlementId ?: -1L)
                    put("isDeleted", e.isDeleted)
                    put("deletedAt", e.deletedAt ?: -1L)
                    if (!e.eventUuid.isNullOrBlank()) {
                        put("eventUuid", e.eventUuid)
                    }
                }
                entriesArr.put(obj)
            }
            root.put("entries", entriesArr)

            val settlementsArr = JSONArray()
            for (s in settlements) {
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("timestamp", s.timestamp)
                    put("entriesTotal", s.entriesTotal)
                    put("previousCarryOver", s.previousCarryOver)
                    put("amountPaid", s.amountPaid)
                    put("nextCarryOver", s.nextCarryOver)
                    put("note", s.note)
                }
                settlementsArr.put(obj)
            }
            root.put("settlements", settlementsArr)

            val wifiArr = JSONArray()
            for (w in wifiSessions) {
                val obj = JSONObject().apply {
                    put("id", w.id)
                    put("ssid", w.ssid)
                    put("startTime", w.startTime)
                    put("endTime", w.endTime)
                    put("status", w.status)
                    put("isOpen", w.isOpen)
                }
                wifiArr.put(obj)
            }
            root.put("wifiSessions", wifiArr)

            val rawJson = root.toString()
            val finalPayload = if (encEnabled) {
                ElamalSecurity.encryptBackup(rawJson, encPass)
            } else {
                rawJson
            }

            val targetFileName = if (isManual) {
                "elamal_manual_${System.currentTimeMillis()}.json"
            } else {
                "elamal_auto_backup.json"
            }

            val writtenUri = writeToDocumentFileFast(context, dir, targetFileName, finalPayload)
                ?: return@withContext "❌ فشل إنشاء أو تحديث ملف النسخة الاحتياطية"

            if (isManual) {
                val verifyRead = readUriText(context, writtenUri)
                if (verifyRead != finalPayload) {
                    return@withContext "⚠️ تحذير: فشل التحقق من سلامة الملف بعد الكتابة!"
                }
                pruneOldManualBackups(dir)
            }

            prefs.edit()
                .putInt("last_backup_entries_count", activeNonDeletedCount)
                .putInt("last_backup_settlements_count", settlements.size)
                .putBoolean("last_backup_had_clockin", activeClockIn > 0L)
                .putLong("last_backup_time", System.currentTimeMillis())
                .apply()

            if (dataDecreased && !isManual) {
                "تم الحفظ وتدوير نسخة أمان تلقائية ✓"
            } else {
                "تم الحفظ الاحتياطي بنجاح ✓"
            }
        }
    }

    private fun rotateDailySnapshots(context: Context, dir: DocumentFile, currentAutoContent: String) {
        runCatching {
            val s1 = dir.findFile("elamal_snap_1.json")?.let { readUriText(context, it.uri) }
            val s2 = dir.findFile("elamal_snap_2.json")?.let { readUriText(context, it.uri) }
            if (!s2.isNullOrBlank()) writeToDocumentFileFast(context, dir, "elamal_snap_3.json", s2)
            if (!s1.isNullOrBlank()) writeToDocumentFileFast(context, dir, "elamal_snap_2.json", s1)
            writeToDocumentFileFast(context, dir, "elamal_snap_1.json", currentAutoContent)
        }
    }

    private fun pruneOldManualBackups(dir: DocumentFile) {
        runCatching {
            val manualFiles = dir.listFiles()
                .filter { f ->
                    val name = f.name ?: ""
                    name.startsWith("elamal_manual_") && name.endsWith(".json")
                }
                .sortedByDescending { it.lastModified() }

            if (manualFiles.size > MAX_MANUAL_BACKUPS_TO_KEEP) {
                manualFiles.drop(MAX_MANUAL_BACKUPS_TO_KEEP).forEach { oldFile ->
                    runCatching { oldFile.delete() }
                }
            }
        }
    }

    suspend fun restoreFromBackup(
        context: Context,
        dao: ElamalDao,
        fileName: String = "elamal_auto_backup.json"
    ): String {
        val currentEntriesCount = withContext(Dispatchers.IO) { dao.getAllEntriesSync().size }
        if (currentEntriesCount > 0) {
            val preBackupMsg = performSmartBackup(context, dao, isManual = true)
            if (preBackupMsg.startsWith("❌") || preBackupMsg.startsWith("⚠️")) {
                return "توقفت الاستعادة لحماية بياناتك الحالية: فشل حفظ نسخة الأمان الاستباقية ($preBackupMsg)"
            }
        }

        return backupMutex.withLock {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
                val uriStr = prefs.getString("backup_uri", "") ?: ""
                if (uriStr.isBlank()) return@withContext "اختر مجلد النسخ أولاً"
                val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return@withContext "المجلد غير متاح"
                val target = dir.findFile(fileName) ?: return@withContext "الملف $fileName غير موجود"

                val content = readUriText(context, target.uri)
                if (content.isBlank()) return@withContext "الملف المختار فارغ أو تالف"

                val pass = ElamalSecurity.getBackupPassword(prefs)
                val jsonStr = try {
                    ElamalSecurity.decryptBackup(content, pass)
                } catch (e: Exception) {
                    return@withContext "❌ فشل فك التشفير: ${e.message}"
                }

                try {
                    val root = JSONObject(jsonStr)
                    val parsedEntries = mutableListOf<WorkEntry>()
                    val parsedSettlements = mutableListOf<Settlement>()
                    val parsedWifi = if (root.has("wifiSessions")) mutableListOf<WifiSession>() else null

                    val entriesArr = root.getJSONArray("entries")
                    for (i in 0 until entriesArr.length()) {
                        val o = entriesArr.getJSONObject(i)
                        val endT = o.optLong("endTime", -1L).let { if (it == -1L) null else it }
                        val setId = o.optLong("settlementId", -1L).let { if (it == -1L) null else it }
                        val isDel = o.optBoolean("isDeleted", false)
                        val delAt = o.optLong("deletedAt", -1L).let { if (it == -1L) null else it }
                        val uuid = o.optString("eventUuid", "").takeIf { it.isNotBlank() }
                        parsedEntries.add(
                            WorkEntry(
                                id = o.getLong("id"),
                                type = EntryType.valueOf(o.getString("type")),
                                startTime = o.getLong("startTime"),
                                endTime = endT,
                                breakMinutes = o.optInt("breakMinutes", 0),
                                hourlyRateAtEntry = o.optDouble("hourlyRateAtEntry", 10.0),
                                calculatedAmount = o.getDouble("calculatedAmount"),
                                note = o.optString("note", ""),
                                settlementId = setId,
                                isDeleted = isDel,
                                eventUuid = uuid,
                                deletedAt = delAt
                            )
                        )
                    }

                    val settlementsArr = root.getJSONArray("settlements")
                    for (i in 0 until settlementsArr.length()) {
                        val o = settlementsArr.getJSONObject(i)
                        parsedSettlements.add(
                            Settlement(
                                id = o.getLong("id"),
                                timestamp = o.getLong("timestamp"),
                                entriesTotal = o.getDouble("entriesTotal"),
                                previousCarryOver = o.getDouble("previousCarryOver"),
                                amountPaid = o.getDouble("amountPaid"),
                                nextCarryOver = o.getDouble("nextCarryOver"),
                                note = o.optString("note", "")
                            )
                        )
                    }

                    if (parsedWifi != null) {
                        val wifiArr = root.getJSONArray("wifiSessions")
                        for (i in 0 until wifiArr.length()) {
                            val o = wifiArr.getJSONObject(i)
                            parsedWifi.add(
                                WifiSession(
                                    id = o.getLong("id"),
                                    ssid = o.getString("ssid"),
                                    startTime = o.getLong("startTime"),
                                    endTime = o.getLong("endTime"),
                                    status = o.optString("status", "PENDING"),
                                    isOpen = o.optBoolean("isOpen", false)
                                )
                            )
                        }
                    }

                    dao.restoreFullDatabaseAtomic(parsedEntries, parsedSettlements, parsedWifi)

                    val editor = prefs.edit()
                    if (root.has("hourlyRate")) {
                        editor.putFloat("hourly_rate", root.getDouble("hourlyRate").toFloat())
                    }
                    if (root.has("currencySymbol")) {
                        val cur = root.optString("currencySymbol", "DH").trim().ifEmpty { "DH" }
                        editor.putString("currency_symbol", cur)
                        ElamalUtils.currencySymbol = cur
                    }
                    if (root.has("workSsids")) {
                        val restoredSsids = root.getString("workSsids")
                        if (restoredSsids.isNotBlank()) {
                            editor.putString("work_ssids", restoredSsids)
                        }
                    }

                    val now = System.currentTimeMillis()
                    val rawClockIn = if (root.has("activeClockIn")) root.getLong("activeClockIn") else 0L
                    val validClockIn = if (rawClockIn in (now - 20 * 3600_000L)..now) rawClockIn else 0L
                    editor.putLong("active_clock_in", validClockIn)
                    editor.apply()

                    if (validClockIn == 0L) {
                        ElamalUtils.cancelOngoingWorkNotification(context)
                        ElamalUtils.cancelForgotClockOutAlarm(context)
                    }

                    val staleNotice = if (rawClockIn > 0L && validClockIn == 0L) " (تم تجاهل جلسة مفتوحة قديمة)" else ""
                    "تمت الاستعادة الذرية بنجاح ✓$staleNotice"
                } catch (e: Exception) {
                    "❌ خطأ في بنية ملف النسخة (لم يتم مسح بياناتك الحالية): ${e.message}"
                }
            }
        }
    }

    fun listAvailableBackups(context: Context): List<String> {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("backup_uri", "") ?: return emptyList()
        if (uriStr.isBlank()) return emptyList()
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return emptyList()
        return dir.listFiles()
            .filter { f ->
                val n = f.name ?: ""
                n.startsWith("elamal_") && n.endsWith(".json")
            }
            .sortedWith(
                compareByDescending<DocumentFile> { it.name == "elamal_auto_backup.json" }
                    .thenByDescending { it.lastModified() }
            )
            .mapNotNull { it.name }
    }

    fun deleteBackupFile(context: Context, fileName: String): Boolean {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("backup_uri", "") ?: return false
        if (uriStr.isBlank()) return false
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return false
        return dir.findFile(fileName)?.delete() == true
    }

    private fun writeToDocumentFileFast(context: Context, dir: DocumentFile, name: String, content: String): Uri? {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val targetDoc = dir.findFile(name) ?: dir.createFile("application/json", name) ?: return null
        val writeOk = runCatching {
            val stream = context.contentResolver.openOutputStream(targetDoc.uri, "wt")
                ?: context.contentResolver.openOutputStream(targetDoc.uri)
            stream?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        return if (writeOk) targetDoc.uri else null
    }

    private fun readUriText(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        }.getOrDefault("")
    }
}
