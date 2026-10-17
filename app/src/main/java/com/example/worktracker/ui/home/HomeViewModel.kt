package com.example.worktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktracker.data.local.WorkDay
import com.example.worktracker.data.preferences.UserPreferences
import com.example.worktracker.data.repository.WorkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val activeDays: StateFlow<List<WorkDay>> = repository.getActiveWorkDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double?> = repository.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeSessionStart: StateFlow<Long?> = userPreferences.activeSessionStart

    fun toggleClockIn() {
        val currentStart = activeSessionStart.value
        if (currentStart == null) {
            // تسجيل الدخول
            userPreferences.startSession(System.currentTimeMillis())
        } else {
            // تسجيل الخروج والحساب التلقائي
            val endTime = System.currentTimeMillis()
            val durationHours = (endTime - currentStart) / (1000.0 * 60 * 60)
            val hourlyRate = userPreferences.getHourlyRate()
            
            // اقتطاع عشريتين فقط من الأجر (مثل 45.50)
            val calculatedAmount = Math.round((durationHours * hourlyRate) * 100) / 100.0

            viewModelScope.launch {
                repository.insertWorkDay(
                    WorkDay(
                        date = currentStart,
                        startTime = currentStart,
                        endTime = endTime,
                        amount = calculatedAmount,
                        note = "دوام آلي",
                        isCredit = false,
                        isRemote = false
                    )
                )
            }
            userPreferences.clearSession()
        }
    }

    fun addRealRecord(amount: Double, note: String, isCredit: Boolean, isRemote: Boolean) {
        viewModelScope.launch {
            repository.insertWorkDay(
                WorkDay(
                    date = System.currentTimeMillis(),
                    amount = amount,
                    note = note,
                    isCredit = isCredit,
                    isRemote = isRemote
                )
            )
        }
    }
}
