package com.nanomks.elamal.widget

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.nanomks.elamal.R
import com.nanomks.elamal.data.ElamalDatabase
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.WorkEntry
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ElamalWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidgetUi(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ElamalDatabase.getInstance(context).dao().closeAllOpenWifiSessions()
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }
                ElamalUtils.syncPeriodicWifiPollSchedule(context)
                val activeStart = prefs.getLong("active_clock_in", 0L)
                if (activeStart > 0L) {
                    val rate = prefs.getFloat("hourly_rate", 10f).toDouble()
                    ElamalUtils.showOrUpdateOngoingWorkNotification(context, activeStart, rate)
                }
                refreshAllWidgets(context)
            }
            "com.nanomks.elamal.ACTION_WIDGET_RESET_TAP" -> {
                refreshAllWidgets(context)
            }
            "com.nanomks.elamal.ACTION_FORGOT_CLOCK_OUT" -> {
                val activeStart = prefs.getLong("active_clock_in", 0L)
                if (activeStart > 0L) {
                    showForgotClockOutNotification(context)
                }
            }
            "com.nanomks.elamal.ACTION_WIFI_DISCONNECT_TIMEOUT",
            "com.nanomks.elamal.ACTION_WIFI_PERIODIC_POLL" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = ElamalDatabase.getInstance(context).dao()
                        ElamalUtils.pollAndMergeRootWifi(context, dao)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "com.nanomks.elamal.ACTION_NOTIF_CLOCK_OUT" -> {
                val activeStart = prefs.getLong("active_clock_in", 0L)
                if (activeStart > 0L) {
                    val pendingResult = goAsync()
                    val now = System.currentTimeMillis()
                    val rate = prefs.getFloat("hourly_rate", 10f).toDouble()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = ElamalDatabase.getInstance(context).dao()
                            val pay = ElamalUtils.calculateHourlyPay(activeStart, now, 0, rate)
                            dao.insertEntry(
                                WorkEntry(
                                    type = EntryType.HOURLY,
                                    startTime = activeStart,
                                    endTime = now,
                                    hourlyRateAtEntry = rate,
                                    calculatedAmount = pay,
                                    note = "خروج سريع من الإشعار"
                                )
                            )
                            prefs.edit().putLong("active_clock_in", 0L).commit()
                            ElamalUtils.cancelForgotClockOutAlarm(context)
                            ElamalUtils.cancelOngoingWorkNotification(context)
                            refreshAllWidgets(context)
                            BackupSafManager.performSmartBackup(context, dao)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            "com.nanomks.elamal.ACTION_WIDGET_CLICK" -> {
                val isEnabled = prefs.getBoolean("widget_enabled", true)
                if (!isEnabled) return

                val now = System.currentTimeMillis()
                val lastSave = prefs.getLong("widget_last_save", 0L)
                if (now - lastSave < 8000L) return

                val firstTap = prefs.getLong("widget_first_tap", 0L)
                if (now - firstTap <= 60_000L) {
                    val pendingResult = goAsync()
                    val activeStart = prefs.getLong("active_clock_in", 0L)
                    val rate = prefs.getFloat("hourly_rate", 10f).toDouble()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (activeStart == 0L) {
                                prefs.edit()
                                    .putLong("widget_first_tap", 0L)
                                    .putLong("widget_last_save", now)
                                    .putLong("active_clock_in", now)
                                    .commit()
                                ElamalUtils.scheduleForgotClockOutAlarm(context, now)
                                ElamalUtils.showOrUpdateOngoingWorkNotification(context, now, rate)
                            } else {
                                val uuid = UUID.randomUUID().toString()
                                val queueJson = prefs.getString("widget_event_queue", "[]") ?: "[]"
                                val arr = runCatching { JSONArray(queueJson) }.getOrDefault(JSONArray())
                                val ev = JSONObject().apply {
                                    put("uuid", uuid)
                                    put("startTime", activeStart)
                                    put("endTime", now)
                                    put("rate", rate)
                                }
                                arr.put(ev)
                                prefs.edit().putString("widget_event_queue", arr.toString()).commit()

                                val dao = ElamalDatabase.getInstance(context).dao()
                                val pay = ElamalUtils.calculateHourlyPay(activeStart, now, 0, rate)
                                if (dao.getEntryByUuid(uuid) == null) {
                                    dao.insertEntry(
                                        WorkEntry(
                                            type = EntryType.HOURLY,
                                            startTime = activeStart,
                                            endTime = now,
                                            hourlyRateAtEntry = rate,
                                            calculatedAmount = pay,
                                            note = "تسجيل تلقائي من الودجت",
                                            eventUuid = uuid
                                        )
                                    )
                                }
                                prefs.edit()
                                    .putLong("widget_first_tap", 0L)
                                    .putLong("widget_last_save", now)
                                    .putLong("active_clock_in", 0L)
                                    .commit()
                                ElamalUtils.cancelForgotClockOutAlarm(context)
                                ElamalUtils.cancelOngoingWorkNotification(context)
                                BackupSafManager.performSmartBackup(context, dao)
                            }
                            refreshAllWidgets(context)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    prefs.edit().putLong("widget_first_tap", now).apply()
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val resetIntent = Intent(context, ElamalWidgetReceiver::class.java).apply {
                        action = "com.nanomks.elamal.ACTION_WIDGET_RESET_TAP"
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, 101, resetIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    am.set(AlarmManager.RTC, now + 61_000L, pi)
                    refreshAllWidgets(context)
                }
            }
        }
    }

    private fun showForgotClockOutNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "elamal_alerts"
        val channel = NotificationChannel(channelId, "تنبيهات العمل", NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("تنبيه نسيان الخروج - العمل")
            .setContentText("لديك جلسة عمل جارية تجاوزت الحد المحدد! افتح التطبيق لتسجيل الخروج.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(901, notif)
    }

    companion object {
        fun refreshAllWidgets(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ElamalWidgetReceiver::class.java))
            for (id in ids) {
                updateWidgetUi(context, mgr, id)
            }
        }

        fun updateWidgetUi(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("widget_enabled", true)
            val activeStart = prefs.getLong("active_clock_in", 0L)
            val firstTap = prefs.getLong("widget_first_tap", 0L)
            val now = System.currentTimeMillis()

            val views = RemoteViews(context.packageName, R.layout.widget_elamal)
            if (!enabled) {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#64748B"))
                views.setTextViewText(R.id.widget_status, "معطّل")
            } else if (now - firstTap <= 60_000L) {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#D97706"))
                views.setTextViewText(R.id.widget_status, if (activeStart == 0L) "اضغط مجدداً لتأكيد الدخول (60ث)" else "اضغط مجدداً لتأكيد الخروج (60ث)")
            } else if (activeStart > 0L) {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#059669"))
                views.setTextViewText(R.id.widget_status, "دخول جارٍ.. اضغط للخروج")
            } else {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#0F172A"))
                views.setTextViewText(R.id.widget_status, "اضغط لتسجيل الدخول")
            }

            val clickIntent = Intent(context, ElamalWidgetReceiver::class.java).apply {
                action = "com.nanomks.elamal.ACTION_WIDGET_CLICK"
            }
            val pi = PendingIntent.getBroadcast(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
