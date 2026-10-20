package com.nanomks.elamal.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

@Dao
interface ElamalDao {
    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 AND settlementId IS NULL ORDER BY id DESC, COALESCE(endTime, startTime) DESC")
    fun getActiveEntries(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 ORDER BY id DESC, COALESCE(endTime, startTime) DESC")
    fun getAllNonDeletedEntriesFlow(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 ORDER BY id DESC, COALESCE(endTime, startTime) DESC")
    suspend fun getAllEntriesSync(): List<WorkEntry>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 AND type = 'HOURLY' AND endTime IS NOT NULL AND id != :excludeId AND startTime < :endMillis AND endTime > :startMillis")
    suspend fun findOverlappingHourlyEntries(startMillis: Long, endMillis: Long, excludeId: Long = -1L): List<WorkEntry>

    @Query("SELECT COALESCE(SUM(calculatedAmount), 0.0) FROM work_entries WHERE isDeleted = 0 AND settlementId = :settlementId")
    suspend fun getSumForSettlement(settlementId: Long): Double

    @Query("SELECT * FROM work_entries WHERE eventUuid = :uuid LIMIT 1")
    suspend fun getEntryByUuid(uuid: String): WorkEntry?

    @Query("SELECT * FROM settlements ORDER BY timestamp DESC, id DESC")
    fun getAllSettlements(): Flow<List<Settlement>>

    @Query("SELECT * FROM settlements ORDER BY timestamp ASC, id ASC")
    suspend fun getSettlementsAsc(): List<Settlement>

    @Query("SELECT * FROM wifi_sessions WHERE status != 'IGNORED' ORDER BY id DESC, startTime DESC")
    fun getVisibleWifiSessions(): Flow<List<WifiSession>>

    @Query("SELECT * FROM wifi_sessions ORDER BY startTime ASC")
    suspend fun getAllWifiSessionsSync(): List<WifiSession>

    @Query("SELECT * FROM wifi_sessions ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestWifiSessionSync(): WifiSession?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: WorkEntry): Long

    @Update
    suspend fun updateEntry(entry: WorkEntry)

    @Query("DELETE FROM work_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM work_entries WHERE isDeleted = 1")
    suspend fun purgeSoftDeletedEntries()

    @Query("DELETE FROM settlements")
    suspend fun clearSettlements()

    @Query("DELETE FROM wifi_sessions")
    suspend fun clearWifiSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement): Long

    @Update
    suspend fun updateSettlement(settlement: Settlement)

    @Delete
    suspend fun deleteSettlement(settlement: Settlement)

    @Query("UPDATE work_entries SET settlementId = :settlementId WHERE isDeleted = 0 AND settlementId IS NULL AND endTime IS NOT NULL")
    suspend fun attachEntriesToSettlement(settlementId: Long)

    @Query("UPDATE work_entries SET settlementId = NULL WHERE settlementId = :settlementId")
    suspend fun detachEntriesFromSettlement(settlementId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiSession(session: WifiSession): Long

    @Update
    suspend fun updateWifiSession(session: WifiSession)

    @Query("UPDATE wifi_sessions SET status = 'IGNORED' WHERE status = 'PENDING'")
    suspend fun ignoreAllPendingWifi()

    @Transaction
    suspend fun recalculateSettlementsCascade() {
        val list = getSettlementsAsc()
        var runningCarry = 0.0
        for (s in list) {
            val dynamicEntriesSum = roundMoney(getSumForSettlement(s.id))
            val effectiveEntriesTotal = if (dynamicEntriesSum != 0.0) dynamicEntriesSum else s.entriesTotal
            val netDue = roundMoney(effectiveEntriesTotal + runningCarry)
            val nextCarry = roundMoney(netDue - s.amountPaid)
            if (s.entriesTotal != effectiveEntriesTotal || s.previousCarryOver != runningCarry || s.nextCarryOver != nextCarry) {
                updateSettlement(
                    s.copy(
                        entriesTotal = effectiveEntriesTotal,
                        previousCarryOver = runningCarry,
                        nextCarryOver = nextCarry
                    )
                )
            }
            runningCarry = nextCarry
        }
    }

    private fun roundMoney(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}

@Database(entities = [WorkEntry::class, Settlement::class, WifiSession::class], version = 5, exportSchema = false)
abstract class ElamalDatabase : RoomDatabase() {
    abstract fun dao(): ElamalDao

    companion object {
        @Volatile
        private var INSTANCE: ElamalDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema between v4 and v5 is identical; no-op migration keeps all user records intact
            }
        }

        fun getInstance(context: android.content.Context): ElamalDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ElamalDatabase::class.java,
                    "elamal_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
