package com.nanomks.elamal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

@Dao
interface ElamalDao {
    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 AND settlementId IS NULL ORDER BY startTime DESC")
    fun getActiveEntries(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAllNonDeletedEntriesFlow(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 ORDER BY startTime DESC")
    suspend fun getAllEntriesSync(): List<WorkEntry>

    @Query("SELECT * FROM work_entries WHERE eventUuid = :uuid LIMIT 1")
    suspend fun getEntryByUuid(uuid: String): WorkEntry?

    @Query("SELECT * FROM settlements ORDER BY timestamp DESC")
    fun getAllSettlements(): Flow<List<Settlement>>

    @Query("SELECT * FROM settlements ORDER BY timestamp ASC")
    suspend fun getSettlementsAsc(): List<Settlement>

    @Query("SELECT * FROM wifi_sessions WHERE status != 'IGNORED' ORDER BY startTime DESC")
    fun getVisibleWifiSessions(): Flow<List<WifiSession>>

    @Query("SELECT * FROM wifi_sessions ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestWifiSessionSync(): WifiSession?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: WorkEntry): Long

    @Update
    suspend fun updateEntry(entry: WorkEntry)

    @Query("DELETE FROM work_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM settlements")
    suspend fun clearSettlements()

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
            val netDue = roundMoney(s.entriesTotal + runningCarry)
            val nextCarry = roundMoney(netDue - s.amountPaid)
            if (s.previousCarryOver != runningCarry || s.nextCarryOver != nextCarry) {
                updateSettlement(s.copy(previousCarryOver = runningCarry, nextCarryOver = nextCarry))
            }
            runningCarry = nextCarry
        }
    }

    private fun roundMoney(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}

@Database(entities = [WorkEntry::class, Settlement::class, WifiSession::class], version = 4, exportSchema = false)
abstract class ElamalDatabase : RoomDatabase() {
    abstract fun dao(): ElamalDao

    companion object {
        @Volatile
        private var INSTANCE: ElamalDatabase? = null

        fun getInstance(context: android.content.Context): ElamalDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ElamalDatabase::class.java,
                    "elamal_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
