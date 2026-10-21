package com.nanomks.elamal.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.NotificationCompat
import com.nanomks.elamal.MainActivity
import com.nanomks.elamal.data.ElamalDao
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WifiSession
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object ElamalUtils {

    private val LAUNCHER_ALIASES = listOf(
        "com.nanomks.elamal.LauncherDefault" to "#059669",
        "com.nanomks.elamal.LauncherAlt" to "#2563EB",
        "com.nanomks.elamal.LauncherPurple" to "#7C3AED",
        "com.nanomks.elamal.LauncherTeal" to "#0D9488",
        "com.nanomks.elamal.LauncherCyan" to "#0284C7",
        "com.nanomks.elamal.LauncherIndigo" to "#4F46E5",
        "com.nanomks.elamal.LauncherSlate" to "#475569"
    )

    fun findClosestIconAlias(hexColor: String): String {
        val targetColor = runCatching { Color.parseColor(hexColor) }.getOrDefault(Color.parseColor("#059669"))
        val r1 = Color.red(targetColor)
        val g1 = Color.green(targetColor)
        val b1 = Color.blue(targetColor)

        return LAUNCHER_ALIASES.minByOrNull { (_, aliasHex) ->
            val c2 = Color.parseColor(aliasHex)
            val r2 = Color.red(c2)
            val g2 = Color.green(c2)
            val b2 = Color.blue(c2)
            (r1 - r2).toDouble().pow(2.0) + (g1 - g2).toDouble().pow(2.0) + (b1 - b2).toDouble().pow(2.0)
        }?.first ?: "com.nanomks.elamal.LauncherDefault"
    }

    fun applyLauncherAlias(context: Context, targetAliasName: String) {
        val pm = context.packageManager
        val targetComp = ComponentName(context, targetAliasName)
        if (pm.getComponentEnabledSetting(targetComp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            val anyOtherEnabled = LAUNCHER_ALIASES.any { (name, _) ->
                name != targetAliasName && pm.getComponentEnabledSetting(ComponentName(context, name)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            if (!anyOtherEnabled) return
        }

        runCatching {
            pm.setComponentEnabledSetting(
                targetComp,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            LAUNCHER_ALIASES.forEach { (aliasName, _) ->
                if (aliasName != targetAliasName) {
                    pm.setComponentEnabledSetting(
                        ComponentName(context, aliasName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }

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

    fun formatDate(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
    }

    fun parseStrictDate(input: String): Long? {
        val trimmed = input.trim()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val pos = ParsePosition(0)
        val d = sdf.parse(trimmed, pos)
        return if (d != null && pos.index == trimmed.length) d.time else null
    }

    fun parseStrictDateTime(input: String): Long? {
        val trimmed = input.trim()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        val pos = ParsePosition(0)
        val d = sdf.parse(trimmed, pos)
        return if (d != null && pos.index == trimmed.length) d.time else null
    }

    /**
     * Zero-cost pre-check: checks if the device currently has an active Wi-Fi transport
     * without spawning any root shell process.
     */
    fun isWifiConnectedFast(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Lightweight Root Wi-Fi SSID reader:
     * Uses fast `cmd wifi status` and `iw dev wlan0 link` first.
     * Avoids heavy `dumpsys wifi` unless needed.
     */
    fun getConnectedSsidViaRoot(context: Context? = null): String? {
        if (context != null && !isWifiConnectedFast(context)) {
            return null
        }
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("cmd wifi status\n")
            os.writeBytes("iw dev wlan0 link | grep SSID\n")
            os.writeBytes("exit\n")
            os.flush()

            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()

            val p1 = Regex("""SSID:\s*"([^"]+)"""")
            p1.find(output)?.groupValues?.getOrNull(1)?.let {
                if (it.isNotBlank() && it != "<unknown ssid>" && it != "0x") return it.trim()
            }

            val p2 = Regex("""SSID:\s*([^,\n"]+)""")
            p2.find(output)?.groupValues?.getOrNull(1)?.let {
                val cleaned = it.trim()
                if (cleaned.isNotBlank() && cleaned != "<unknown ssid>" && cleaned != "null" && cleaned != "0x") {
                    return cleaned
                }
            }
            null
        } catch (e: Exception) {
            process?.destroyForcibly()
            null
        }
    }

    suspend fun pollAndMergeRootWifi(context: Context, dao: ElamalDao): String? {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("root_wifi_enabled", true)) return null

        val configuredList = (prefs.getString("work_ssids", "") ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(6)

        val currentSsid = getConnectedSsidViaRoot(context)
        if (configuredList.isEmpty()) return currentSsid

        val now = System.currentTimeMillis()
        val isWorkWifi = currentSsid != null && configuredList.any { it.equals(currentSsid, ignoreCase = true) }

        val latest = dao.getLatestWifiSessionSync()

        if (isWorkWifi && currentSsid != null) {
            cancelWifiDisconnectTimeout(context)

            if (latest != null && latest.status == "PENDING") {
                val gapMillis = now - latest.endTime
                if (latest.isOpen || gapMillis <= 15 * 60_000L) {
                    val mergedSsid = if (latest.ssid.split("+").any { it.equals(currentSsid, ignoreCase = true) }) {
                        latest.ssid
                    } else {
                        "${latest.ssid}+$currentSsid"
                    }
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

    fun syncPeriodicWifiPollSchedule(context: Context) {
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("root_wifi_enabled", true)
        val hasSsids = !(prefs.getString("work_ssids", "").isNullOrBlank())

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_WIFI_PERIODIC_POLL"
        }
        val pi = PendingIntent.getBroadcast(
            context, 404, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled && hasSsids) {
            am.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pi
            )
        } else {
            am.cancel(pi)
        }
    }

    fun showOrUpdateOngoingWorkNotification(context: Context, startMillis: Long, hourlyRate: Double) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "elamal_ongoing_session"
        val channel = NotificationChannel(channelId, "جلسة العمل الجارية", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            context, 501, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockOutIntent = Intent(context, ElamalWidgetReceiver::class.java).apply {
            action = "com.nanomks.elamal.ACTION_NOTIF_CLOCK_OUT"
        }
        val clockOutPi = PendingIntent.getBroadcast(
            context, 502, clockOutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startClockStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(startMillis))
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("جلسة عمل جارية (بدأت $startClockStr)")
            .setContentText("سعر الساعة: ${formatMoney(hourlyRate)} • العداد يعمل تلقائياً")
            .setWhen(startMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_media_pause, "تسجيل الخروج الآن", clockOutPi)
            .build()

        nm.notify(902, notif)
    }

    fun cancelOngoingWorkNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(902)
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
