package com.example.worktracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    // جلب كل السجلات غير المؤرشفة مرتبة من الأحدث للأقدم
    @Query("SELECT * FROM work_days WHERE isArchived = 0 ORDER BY date DESC")
    fun getActiveWorkDays(): Flow<List<WorkDay>>

    // حساب المجموع الكامل (يجمع الأجور مع الكريدي السالب تلقائياً)
    @Query("SELECT SUM(amount) FROM work_days WHERE isArchived = 0")
    fun getTotalBalance(): Flow<Double?>

    @Insert
    suspend fun insertWorkDay(workDay: WorkDay)

    @Update
    suspend fun updateWorkDay(workDay: WorkDay)

    @Delete
    suspend fun deleteWorkDay(workDay: WorkDay)
}
