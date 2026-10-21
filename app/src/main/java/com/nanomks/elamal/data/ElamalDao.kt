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

    @Query("SELECT * FROM work_entries WHERE isDeleted = 1 ORDER BY id DESC")
    fun getDeletedEntriesFlow(): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 ORDER BY id DESC, COALESCE(endTime, startTime) DESC")
    suspend fun getAllEntriesSync(): List<WorkEntry>

    @Query("SELECT * FROM work_entries WHERE isDeleted = 0 AND type = 'HOURLY' AND endTime IS NOT NULL AND id != :excludeId AND startTime < :endMillis AND endTime > :startMillis")
    suspend fun findOverlappingHourlyEntries(startMillis: Long, endMillis: Long, excludeId: Long = -1L): List<WorkEntry>

    @Query("SELECT COALESCE(SUM(calculatedAmount), 0.0) FROM work_entries WHERE isDeleted = 0 AND settlementId = :settlementId")
    suspend fun getSumForSettlement(settlementId: Long): Double

    @Query("SELECT COUNT(*) FROM work_entries WHERE settlementId = :settlementId")
    suspend fun getAnyEntriesCountForSettlement(settlementId: Long): Int

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WorkEntry): Long

    @Update
    suspend fun updateEntry(entry: WorkEntry)

    @Query("DELETE FROM work_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM work_entries WHERE isDeleted = 1 AND startTime < :olderThanMillis")
    suspend fun purgeOldSoftDeletedEntries(olderThanMillis: Long)

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

    /**
     * Fixed cascade calculation:
     * Uses dynamic sum of active non-deleted entries attached to the settlement.
     * If all attached entries were deleted (totalAttachedCount > 0 and active sum == 0.0),
     * effectiveEntriesTotal accurately becomes 0.0 instead of keeping stale s.entriesTotal!
     * Only falls back to s.entriesTotal if the settlement never had individual entries linked (legacy).
     */
    @Transaction
    suspend fun recalculateSettlementsCascade() {
        val list = getSettlementsAsc()
        var runningCarry = 0.0
        for (s in list) {
            val dynamicEntriesSum = roundMoney(getSumForSettlement(s.id))
            val totalAttachedIncludingDeleted = getAnyEntriesCountForSettlement(s.id)
            val effectiveEntriesTotal = if (dynamicEntriesSum != 0.0 || totalAttachedIncludingDeleted > 0) {
                dynamicEntriesSum
            } else {
                roundMoney(s.entriesTotal)
            }
            val paidRounded = roundMoney(s.amountPaid)
            val netDue = roundMoney(effectiveEntriesTotal + runningCarry)
            val nextCarry = roundMoney(netDue - paidRounded)
            if (s.entriesTotal != effectiveEntriesTotal ||
                s.previousCarryOver != runningCarry ||
                s.amountPaid != paidRounded ||
                s.nextCarryOver != nextCarry
            ) {
                updateSettlement(
                    s.copy(
                        entriesTotal = effectiveEntriesTotal,
                        previousCarryOver = runningCarry,
                        amountPaid = paidRounded,
                        nextCarryOver = nextCarry
                    )
                )
            }
            runningCarry = nextCarry
        }
    }

    @Transaction
    suspend fun restoreFullDatabaseAtomic(
        entriesToInsert: List<WorkEntry>,
        settlementsToInsert: List<Settlement>,
        wifiToInsert: List<WifiSession>?
    ) {
        clearEntries()
        clearSettlements()
        for (e in entriesToInsert) {
            insertEntry(e)
        }
        for (s in settlementsToInsert) {
            insertSettlement(s)
        }
        if (wifiToInsert != null) {
            clearWifiSessions()
            for (w in wifiToInsert) {
                insertWifiSession(w)
            }
        }
        recalculateSettlementsCascade()
    }

    fun roundMoney(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}

@Database(entities = [WorkEntry::class, Settlement::class, WifiSession::class], version = 5, exportSchema = false)
abstract class ElamalDatabase : RoomDatabase() {
    abstract fun dao(): ElamalDao

    companion object {
        @Volatile
        private var INSTANCE: ElamalDatabase? = null

        private val SAFE_MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN eventUuid TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_eventUuid ON work_entries(eventUuid)")
            }
        }

        private val SAFE_MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN eventUuid TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_eventUuid ON work_entries(eventUuid)")
            }
        }

        private val SAFE_MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_eventUuid ON work_entries(eventUuid)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema between v4 and v5 is identical
            }
        }

        fun getInstance(context: android.content.Context): ElamalDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ElamalDatabase::class.java,
                    "elamal_db"
                )
                    .addMigrations(
                        SAFE_MIGRATION_1_5,
                        SAFE_MIGRATION_2_5,
                        SAFE_MIGRATION_3_5,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
