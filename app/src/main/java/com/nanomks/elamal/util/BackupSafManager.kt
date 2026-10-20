package com.nanomks.elamal.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.nanomks.elamal.data.*
import org.json.JSONArray
import org.json.JSONObject

object BackupSafManager {

    suspend fun performSmartBackup(
        context: Context,
        dao: ElamalDao,
        isManual: Boolean = false
    ): String {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("backup_uri", "") ?: ""
        if (uriStr.isBlank()) return "اختر مجلد النسخ الاحتياطي من الإعدادات لحفظ نسخك تلقائياً"

        val treeUri = Uri.parse(uriStr)
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return "تعذر الوصول إلى مجلد النسخ"

        val entries = dao.getAllEntriesSync()
        val settlements = dao.getSettlementsAsc()
        val activeClockIn = prefs.getLong("active_clock_in", 0L)

        val prevEntriesCount = prefs.getInt("last_backup_entries_count", 0)
        val prevSettlementsCount = prefs.getInt("last_backup_settlements_count", 0)
        val prevHadClockIn = prefs.getBoolean("last_backup_had_clockin", false)

        if (entries.isEmpty() && settlements.isEmpty() && activeClockIn == 0L && (prevEntriesCount > 0 || prevSettlementsCount > 0)) {
            return "حماية تلقائية: تم منع الكتابة بنسخة فارغة فوق نسخة تحتوي على بيانات!"
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
                writeToDocumentFile(context, dir, safetyName, prevContent)
            }
        }

        val root = JSONObject()
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())
        root.put("activeClockIn", activeClockIn)
        root.put("hourlyRate", prefs.getFloat("hourly_rate", 10f).toDouble())

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

        val rawJson = root.toString(2)
        val encEnabled = prefs.getBoolean("backup_encrypt_enabled", false)
        val encPass = prefs.getString("backup_password", "") ?: ""
        val finalPayload = if (encEnabled && encPass.isNotBlank()) {
            ElamalUtils.encryptBackup(rawJson, encPass)
        } else {
            rawJson
        }

        val targetFileName = if (isManual) {
            "elamal_manual_${System.currentTimeMillis()}.json"
        } else {
            "elamal_auto_backup.json"
        }

        val writtenUri = writeToDocumentFile(context, dir, targetFileName, finalPayload)
            ?: return "فشل إنشاء ملف النسخة الاحتياطية"

        val verifyRead = readUriText(context, writtenUri)
        if (verifyRead != finalPayload) {
            return "تحذير: فشل التحقق من سلامة الملف بعد الكتابة!"
        }

        prefs.edit()
            .putInt("last_backup_entries_count", entries.size)
            .putInt("last_backup_settlements_count", settlements.size)
            .putBoolean("last_backup_had_clockin", activeClockIn > 0L)
            .putLong("last_backup_time", System.currentTimeMillis())
            .apply()

        return if (dataDecreased && !isManual) {
            "تم الحفظ وتدوير نسخة أمان (بسبب رصد نقص بيانات) ✓"
        } else {
            "تم النسخ الاحتياطي والتحقق من سلامته بنجاح ✓"
        }
    }

    suspend fun restoreFromBackup(context: Context, dao: ElamalDao, fileName: String = "elamal_auto_backup.json"): String {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("backup_uri", "") ?: ""
        if (uriStr.isBlank()) return "اختر مجلد النسخ أولاً"
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return "المجلد غير متاح"
        val target = dir.findFile(fileName) ?: return "الملف $fileName غير موجود"

        performSmartBackup(context, dao, isManual = true)

        val content = readUriText(context, target.uri)
        val pass = prefs.getString("backup_password", "") ?: ""
        val jsonStr = try {
            ElamalUtils.decryptBackup(content, pass)
        } catch (e: Exception) {
            return "فشل فك التشفير: تأكد من كلمة مرور النسخ الاحتياطي"
        }

        return try {
            val root = JSONObject(jsonStr)
            dao.clearEntries()
            dao.clearSettlements()

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
            "تمت الاستعادة بنجاح (مع حفظ نسخة أمان قبل الاستعادة) ✓"
        } catch (e: Exception) {
            "خطأ أثناء قراءة بيانات النسخة: ${e.message}"
        }
    }

    fun listAvailableBackups(context: Context): List<String> {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("backup_uri", "") ?: return emptyList()
        if (uriStr.isBlank()) return emptyList()
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return emptyList()
        return dir.listFiles().mapNotNull { it.name }.filter { it.startsWith("elamal_") }.sortedDescending()
    }

    private fun writeToDocumentFile(context: Context, dir: DocumentFile, name: String, content: String): Uri? {
        dir.findFile(name)?.delete()
        val doc = dir.createFile("application/json", name) ?: return null
        context.contentResolver.openOutputStream(doc.uri)?.use {
            it.write(content.toByteArray(Charsets.UTF_8))
        }
        return doc.uri
    }

    private fun readUriText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
    }
}
