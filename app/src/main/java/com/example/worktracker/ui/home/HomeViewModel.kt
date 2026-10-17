package com.example.worktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktracker.data.local.WorkDay
import com.example.worktracker.data.repository.WorkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    val activeDays: StateFlow<List<WorkDay>> = repository.getActiveWorkDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double?> = repository.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // دالة تجريبية لاختبار الواجهة قبل بناء نوافذ الإضافة الحقيقية
    fun addTestRecord(amount: Double, isCredit: Boolean) {
        viewModelScope.launch {
            repository.insertWorkDay(
                WorkDay(
                    date = System.currentTimeMillis(),
                    amount = amount,
                    isCredit = isCredit
                )
            )
        }
    }
}
