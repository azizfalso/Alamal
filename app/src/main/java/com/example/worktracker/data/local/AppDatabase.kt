package com.example.worktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkDay::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao
}
