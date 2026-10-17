package com.example.worktracker.data.repository

import com.example.worktracker.data.local.WorkDao
import com.example.worktracker.data.local.WorkDay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkRepository @Inject constructor(
    private val dao: WorkDao
) {
    fun getActiveWorkDays(): Flow<List<WorkDay>> = dao.getActiveWorkDays()
    
    fun getTotalBalance(): Flow<Double?> = dao.getTotalBalance()
    
    suspend fun insertWorkDay(workDay: WorkDay) = dao.insertWorkDay(workDay)
}
