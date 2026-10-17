package com.example.worktracker.data.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val sharedPrefs: SharedPreferences
) {
    // أجر الساعة المخفي (15 درهم افتراضياً)
    fun getHourlyRate(): Double = sharedPrefs.getFloat("HOURLY_RATE", 15.0f).toDouble()

    fun setHourlyRate(rate: Double) {
        sharedPrefs.edit().putFloat("HOURLY_RATE", rate.toFloat()).apply()
    }

    // تتبع حالة الدوام (وقت بدء العمل) مصحح
    private val _activeSessionStart = MutableStateFlow<Long?>(
        run {
            val storedTime = sharedPrefs.getLong("ACTIVE_SESSION_START", 0L)
            if (storedTime == 0L) null else storedTime
        }
    )
    val activeSessionStart: StateFlow<Long?> = _activeSessionStart.asStateFlow()

    fun startSession(startTime: Long) {
        sharedPrefs.edit().putLong("ACTIVE_SESSION_START", startTime).apply()
        _activeSessionStart.value = startTime
    }

    fun clearSession() {
        sharedPrefs.edit().remove("ACTIVE_SESSION_START").apply()
        _activeSessionStart.value = null
    }
}
