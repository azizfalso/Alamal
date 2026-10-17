package com.example.worktracker.di

import android.content.Context
import androidx.room.Room
import com.example.worktracker.data.local.AppDatabase
import com.example.worktracker.data.local.WorkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "work_tracker_db"
        ).build()
    }

    @Provides
    fun provideWorkDao(database: AppDatabase): WorkDao {
        return database.workDao()
    }
}
