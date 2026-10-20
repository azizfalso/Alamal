package com.nanomks.elamal.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EntryType {
    HOURLY, REMOTE, CREDIT
}

@Entity(tableName = "work_entries", indices = [Index(value = ["eventUuid"], unique = true)])
data class WorkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    val startTime: Long,
    val endTime: Long?,
    val breakMinutes: Int = 0,
    val hourlyRateAtEntry: Double = 10.0,
    val calculatedAmount: Double = 0.0,
    val note: String = "",
    val settlementId: Long? = null,
    val isDeleted: Boolean = false,
    val eventUuid: String? = null
)

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val entriesTotal: Double,
    val previousCarryOver: Double,
    val amountPaid: Double,
    val nextCarryOver: Double,
    val note: String = ""
)

@Entity(tableName = "wifi_sessions")
data class WifiSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val startTime: Long,
    val endTime: Long,
    val status: String = "PENDING", // PENDING, ADDED, IGNORED
    val isOpen: Boolean = false
)



