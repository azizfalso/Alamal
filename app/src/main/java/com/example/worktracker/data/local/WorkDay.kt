package com.example.worktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // تاريخ اليوم بصيغة Timestamp
    val isRemote: Boolean = false, // هل هو عمل عن بعد؟
    val isCredit: Boolean = false, // هل هذه سلفة (كريدي) أو دفعة؟
    val startTime: Long? = null,
    val endTime: Long? = null,
    val breakMinutes: Int = 0,
    val amount: Double = 0.0, // الأجر المحسوب أو قيمة الكريدي (يخزن بالسالب للسلفة)
    val note: String = "",
    val isArchived: Boolean = false, // هل تم ترحيل هذا السجل للأرشيف؟
    val archiveBatchId: Long? = null // معرّف الدفعة في الأرشيف
)
