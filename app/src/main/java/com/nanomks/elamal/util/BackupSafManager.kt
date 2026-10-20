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

object BackupSafManager {

    private val backupMutex = Mutex()

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

            val entries = dao.getAllEntriesSync()
            val settlements = dao.getSettlementsAsc()
            val wifiSessions = dao.getAllWifiSessionsSync()
            val activeClockIn = prefs.getLong("active_clock_in", 0L)

            val prevEntriesCount = prefs.getInt("last_backup_entries_count", 0)
            val prevSettlementsCount = prefs.getInt("last_backup_settlements_count", 0)
            val prevHadClockIn = prefs.getBoolean("last_backup_had_clockin", false)

            if (entries.isEmpty() && settlements.isEmpty() && activeClockIn == 0L && (prevEntriesCount > 0 || prevSettlementsCount > 0)) {
                return@withContext "حماية تلقائية: تم منع الكتابة بنسخة فارغة فوق نسخة تحتوي على بيانات!"
            }

            val dataDecreased = (entries.size < prevEntriesCount) ||
                    (settlements.size < prevSettlementsCount) ||
                    (prevHadClockIn && activeClockIn == 0L && entries.size <= prevEntriesCount)

            val mainFile = dir.findFile("elamal_auto_backup.json")
            if (!isManual && dataDecreased && mainFile != null) {
                val prevContent = readUriText(context, mainFile.uri)
                if (prevContent.isNotBlank()) {
                    val slot = (prefs.getInt("safety_slot_index", 0) % 5) + 1
                    prefs.edit().putInt("safety_slot_index", slot).apply()
                    val safetyName = "elamal_safety_slot_${slot}.json"
                    writeToDocumentFileSafely(context, dir, safetyName, prevContent)
                }
            }

            val root = JSONObject()
            root.put("version", 5)
            root.put("timestamp", System.currentTimeMillis())
            root.put("activeClockIn", activeClockIn)
            root.put("hourlyRate", prefs.getFloat("hourly_rate", 10f).toDouble())
            root.put("workSsids", prefs.getString("work_ssids", "") ?: "")

            val entriesArr = JSONArray()
            for (e in entries) {
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

            val rawJson = root.toString(2)
            val encEnabled = prefs.getBoolean("backup_encrypt_enabled", false)
            val encPass = ElamalSecurity.getBackupPassword(prefs)
            val finalPayload = if (encEnabled && encPass.isNotBlank()) {
                ElamalSecurity.encryptBackup(rawJson, encPass)
            } else {
                rawJson
            }

            val targetFileName = if (isManual) {
                "elamal_manual_${System.currentTimeMillis()}.json"
            } else {
                "elamal_auto_backup.json"
            }

            val writtenUri = writeToDocumentFileSafely(context, dir, targetFileName, finalPayload)
                ?: return@withContext "فشل إنشاء أو تحديث ملف النسخة الاحتياطية"

            val verifyRead = readUriText(context, writtenUri)
            if (verifyRead != finalPayload) {
                return@withContext "تحذير: فشل التحقق من سلامة الملف بعد الكتابة!"
            }

            prefs.edit()
                .putInt("last_backup_entries_count", entries.size)
                .putInt("last_backup_settlements_count", settlements.size)
                .putBoolean("last_backup_had_clockin", activeClockIn > 0L)
                .putLong("last_backup_time", System.currentTimeMillis())
                .apply()

            if (dataDecreased && !isManual) {
                "تم الحفظ وتدوير نسخة أمان (بسبب رصد نقص بيانات) ✓"
            } else {
                "تم النسخ الاحتياطي والتحقق من سلامته بنجاح ✓"
            }
        }
    }

    suspend fun restoreFromBackup(
        context: Context,
        dao: ElamalDao,
        fileName: String = "elamal_auto_backup.json"
    ): String {
        // Save a manual safety snapshot before restoring
        performSmartBackup(context, dao, isManual = true)

        return backupMutex.withLock {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
                val uriStr = prefs.getString("backup_uri", "") ?: ""
                if (uriStr.isBlank()) return@withContext "اختر مجلد النسخ أولاً"
                val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return@withContext "المجلد غير متاح"
                val target = dir.findFile(fileName) ?: return@withContext "الملف $fileName غير موجود"

                val content = readUriText(context, target.uri)
                val pass = ElamalSecurity.getBackupPassword(prefs)
                val jsonStr = try {
                    ElamalSecurity.decryptBackup(content, pass)
                } catch (e: Exception) {
                    return@withContext "فشل فك التشفير: تأكد من كلمة مرور النسخ الاحتياطي"
                }

                try {
                    val root = JSONObject(jsonStr)
                    dao.clearEntries()
                    dao.clearSettlements()

                    if (root.has("hourlyRate")) {
                        prefs.edit().putFloat("hourly_rate", root.getDouble("hourlyRate").toFloat()).apply()
                    }
                    if (root.has("activeClockIn")) {
                        prefs.edit().putLong("active_clock_in", root.getLong("activeClockIn")).apply()
                    }
                    if (root.has("workSsids")) {
                        val restoredSsids = root.getString("workSsids")
                        if (restoredSsids.isNotBlank()) {
                            prefs.edit().putString("work_ssids", restoredSsids).apply()
                        }
                    }

                    val entriesArr = root.getJSONArray("entries")
                    for (i in 0 until entriesArr.length()) {
                        val o = entriesArr.getJSONObject(i)
                        val endT = o.getLong("endTime").let { if (it == -1L) null else it }
                        val setId = o.getLong("settlementId").let { if (it == -1L) null else it }
                        dao.insertEntry(
                            WorkEntry(
                                id = o.getLong("id"),
                                type = EntryType.valueOf(o.getString("type")),
                                startTime = o.getLong("startTime"),
                                endTime = endT,
                                breakMinutes = o.getInt("breakMinutes"),
                                hourlyRateAtEntry = o.getDouble("hourlyRateAtEntry"),
                                calculatedAmount = o.getDouble("calculatedAmount"),
                                note = o.getString("note"),
                                settlementId = setId
                            )
                        )
                    }

                    val settlementsArr = root.getJSONArray("settlements")
                    for (i in 0 until settlementsArr.length()) {
                        val o = settlementsArr.getJSONObject(i)
                        dao.insertSettlement(
                            Settlement(
                                id = o.getLong("id"),
                                timestamp = o.getLong("timestamp"),
                                entriesTotal = o.getDouble("entriesTotal"),
                                previousCarryOver = o.getDouble("previousCarryOver"),
                                amountPaid = o.getDouble("amountPaid"),
                                nextCarryOver = o.getDouble("nextCarryOver"),
                                note = o.getString("note")
                            )
                        )
                    }

                    if (root.has("wifiSessions")) {
                        dao.clearWifiSessions()
                        val wifiArr = root.getJSONArray("wifiSessions")
                        for (i in 0 until wifiArr.length()) {
                            val o = wifiArr.getJSONObject(i)
                            dao.insertWifiSession(
                                WifiSession(
                                    id = o.getLong("id"),
                                    ssid = o.getString("ssid"),
                                    startTime = o.getLong("startTime"),
                                    endTime = o.getLong("endTime"),
                                    status = o.getString("status"),
                                    isOpen = o.optBoolean("isOpen", false)
                                )
                            )
                        }
                    }

                    dao.recalculateSettlementsCascade()
                    "تمت الاستعادة الشاملة بنجاح (مع حفظ نسخة أمان قبل الاستعادة) ✓"
                } catch (e: Exception) {
                    "خطأ أثناء قراءة بيانات النسخة: ${e.message}"
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
            .mapNotNull { it.name }
            .filter { it.startsWith("elamal_") && it.endsWith(".json") }
            .sortedDescending()
    }

    /**
     * Safe atomic-style write:
     * 1. Writes to a temporary staging file first and verifies it.
     * 2. Overwrites the target file in-place ("wt") without deleting it first, preserving the previous file if anything fails.
     */
    private fun writeToDocumentFileSafely(context: Context, dir: DocumentFile, name: String, content: String): Uri? {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val tempName = "$name.tmp"
        dir.findFile(tempName)?.delete()
        val tempDoc = dir.createFile("application/octet-stream", tempName)
        if (tempDoc != null) {
            val tempOk = runCatching {
                context.contentResolver.openOutputStream(tempDoc.uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                readUriText(context, tempDoc.uri) == content
            }.getOrDefault(false)
            if (!tempOk) {
                tempDoc.delete()
                return null
            }
        }

        val targetDoc = dir.findFile(name) ?: dir.createFile("application/json", name) ?: return null
        val writeOk = runCatching {
            val stream = context.contentResolver.openOutputStream(targetDoc.uri, "wt")
                ?: context.contentResolver.openOutputStream(targetDoc.uri)
            stream?.use { out ->
                out.write(bytes)
                out.flush()
            }
            true
        }.getOrDefault(false)

        tempDoc?.delete()
        return if (writeOk) targetDoc.uri else null
    }

    private fun readUriText(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        }.getOrDefault("")
    }
}



