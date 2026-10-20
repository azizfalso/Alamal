package com.nanomks.elamal.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nanomks.elamal.data.ElamalDao
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WifiSession
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ElamalUtils {

    fun calculateHourlyPay(startMillis: Long, endMillis: Long, breakMinutes: Int, hourlyRate: Double): Double {
        val totalMinutes = ((endMillis - startMillis) / 60000L) - breakMinutes
        if (totalMinutes <= 0) return 0.0
        val raw = (totalMinutes.toDouble() / 60.0) * hourlyRate
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun formatDurationArabic(minutesTotal: Long): String {
        val safeMin = minutesTotal.coerceAtLeast(0)
        val h = safeMin / 60
        val m = safeMin % 60
        return "${h} س ${m} د"
    }

    fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%.2f DH", amount)
    }

    /**
     * Multi-fallback Root Wi-Fi SSID reader:
     * Opens interactive `su` shell (triggers Magisk/KernelSU grant popup if needed)
     * and checks `cmd wifi status`, `dumpsys wifi`, `wpa_cli`, and `iw`.
     */
    fun getConnectedSsidViaRoot(): String? {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("cmd wifi status\n")
            os.writeBytes("dumpsys wifi | grep -E 'mWifiInfo|SSID'\n")
            os.writeBytes("iw dev wlan0 link | grep SSID\n")
            os.writeBytes("exit\n")
            os.flush()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()

            // Pattern 1: SSID: "MyNetwork"
            val p1 = Regex("""SSID:\s*"([^"]+)"""")
            p1.find(output)?.groupValues?.getOrNull(1)?.let {
                if (it.isNotBlank() && it != "<unknown ssid>" && it != "0x") return it.trim()
            }

            // Pattern 2: SSID: MyNetwork, BSSID: ...
            val p2 = Regex("""SSID:\s*([^,\n"]+)""")
            p2.find(output)?.groupValues?.getOrNull(1)?.let {
                val cleaned = it.trim()
                if (cleaned.isNotBlank() && cleaned != "<unknown ssid>" && cleaned != "null" && cleaned != "0x") {
                    return cleaned
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pollAndMergeRootWifi(context: Context, dao: ElamalDao): String? {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val configuredList = (prefs.getString("work_ssids", "") ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(6)

        val currentSsid = getConnectedSsidViaRoot()
        val now = System.currentTimeMillis()
        val isWorkWifi = currentSsid != null && configuredList.any { it.equals(currentSsid, ignoreCase = true) }

        val latest = dao.getLatestWifiSessionSync()

        if (isWorkWifi && currentSsid != null) {
            cancelWifiDisconnectTimeout(context)

            if (latest != null && latest.status == "PENDING") {
                val gapMillis = now - latest.endTime
                if (latest.isOpen || gapMillis <= 10 * 60_000L) {
                    val mergedSsid = if (latest.ssid.split("+").any { it.equals(currentSsid, ignoreCase = true) }) {
                        latest.ssid
                    } else {
                        "${latest.ssid}+$currentSsid"
                    }
                    // Ensure at least 1 minute duration so user sees progress immediately
                    val safeEnd = maxOf(now, latest.startTime + 60_000L)
                    dao.updateWifiSession(latest.copy(ssid = mergedSsid, endTime = safeEnd, isOpen = true))
                    return currentSsid
                }
            }
            dao.insertWifiSession(
                WifiSession(
                    ssid = currentSsid,
                    startTime = now,
                    endTime = now + 60_000L,
                    status = "PENDING",
                    isOpen = true
                )
            )
        } else {
            if (latest != null && latest.isOpen) {
                if (now - latest.endTime >= 10 * 60_000L) {
                    dao.updateWifiSession(latest.copy(isOpen = false))
                } else {
                    scheduleWifiDisconnectTimeout(context, latest.endTime + 10 * 60_000L)
                }
            }
        }
        return currentSsid
    }

    private fun scheduleWifiDisconnectTimeout(context: Context, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_WIFI_DISCONNECT_TIMEOUT"
        }
        val pi = PendingIntent.getBroadcast(
            context, 303, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.set(AlarmManager.RTC, triggerAtMillis, pi)
    }

    private fun cancelWifiDisconnectTimeout(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_WIFI_DISCONNECT_TIMEOUT"
        }
        val pi = PendingIntent.getBroadcast(
            context, 303, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    suspend fun processPendingWidgetEventsOnAppOpen(context: Context, dao: ElamalDao) {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val queueJson = prefs.getString("widget_event_queue", "[]") ?: "[]"
        val arr = runCatching { JSONArray(queueJson) }.getOrNull() ?: return
        if (arr.length() == 0) return

        val savedUuids = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uuid = o.getString("uuid")
            val start = o.getLong("startTime")
            val end = o.getLong("endTime")
            val rate = o.getDouble("rate")
            val pay = calculateHourlyPay(start, end, 0, rate)

            if (dao.getEntryByUuid(uuid) == null) {
                dao.insertEntry(
                    WorkEntry(
                        type = EntryType.HOURLY,
                        startTime = start,
                        endTime = end,
                        hourlyRateAtEntry = rate,
                        calculatedAmount = pay,
                        note = "تسجيل تلقائي من الودجت",
                        eventUuid = uuid
                    )
                )
            }
            savedUuids.add(uuid)
        }

        delay(8000L)

        val currentJson = prefs.getString("widget_event_queue", "[]") ?: "[]"
        val currentArr = runCatching { JSONArray(currentJson) }.getOrNull() ?: JSONArray()
        val remainingArr = JSONArray()
        for (i in 0 until currentArr.length()) {
            val o = currentArr.getJSONObject(i)
            if (o.getString("uuid") !in savedUuids) {
                remainingArr.put(o)
            }
        }
        prefs.edit().putString("widget_event_queue", remainingArr.toString()).apply()
    }

    fun scheduleForgotClockOutAlarm(context: Context, startMillis: Long) {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val limitHours = prefs.getInt("forgot_alarm_hours", 12).coerceIn(0, 48)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_FORGOT_CLOCK_OUT"
        }
        val pi = PendingIntent.getBroadcast(
            context, 202, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (limitHours == 0) {
            alarmManager.cancel(pi)
            return
        }
        val triggerAt = startMillis + (limitHours * 3600_000L)
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancelForgotClockOutAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_FORGOT_CLOCK_OUT"
        }
        val pi = PendingIntent.getBroadcast(
            context, 202, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun encryptBackup(plainJson: String, password: String): String {
        if (password.isBlank()) return plainJson
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val keySpec = SecretKeySpec(secret, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val enc = Base64.getEncoder()
        return "ELAMAL_ENC_V1:" + enc.encodeToString(salt) + ":" + enc.encodeToString(iv) + ":" + enc.encodeToString(encrypted)
    }

    fun decryptBackup(payload: String, password: String): String {
        if (!payload.startsWith("ELAMAL_ENC_V1:")) return payload
        val parts = payload.split(":")
        require(parts.size == 4) { "ملف مشفر غير صالح" }
        val dec = Base64.getDecoder()
        val salt = dec.decode(parts[1])
        val iv = dec.decode(parts[2])
        val cipherBytes = dec.decode(parts[3])
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val keySpec = SecretKeySpec(secret, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    fun buildWhatsappFormattedText(entries: List<WorkEntry>, totalNet: Double, carryOver: Double): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.append("*📊 ملخص تطبيق «العمل»*\n")
        sb.append("━━━━━━━━━━━━━━\n")
        var totalMins = 0L
        if (entries.isEmpty()) {
            sb.append("لا توجد سجلات نشطة حالياً.\n")
        } else {
            for (e in entries) {
                val dateStr = df.format(Date(e.startTime))
                val typeStr = when (e.type) {
                    EntryType.HOURLY -> {
                        val m = if (e.endTime != null) ((e.endTime - e.startTime) / 60000L) - e.breakMinutes else 0L
                        totalMins += m.coerceAtLeast(0)
                        "ساعات (${formatDurationArabic(m)})"
                    }
                    EntryType.REMOTE -> "عمل عن بعد"
                    EntryType.CREDIT -> "كريدي (-)"
                }
                val notePart = if (e.note.isNotBlank()) " - ${e.note}" else ""
                sb.append("• $dateStr | $typeStr: *${formatMoney(e.calculatedAmount)}*$notePart\n")
            }
        }
        sb.append("━━━━━━━━━━━━━━\n")
        sb.append("⏱ *مجموع الساعات:* ${formatDurationArabic(totalMins)}\n")
        if (carryOver != 0.0) {
            sb.append("🔄 *رصيد مرحّل:* ${formatMoney(carryOver)}\n")
        }
        sb.append("💰 *الصافي المستحق:* *${formatMoney(totalNet)}*")
        return sb.toString()
    }
}
