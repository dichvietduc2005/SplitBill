package com.example.splitbill.di

import android.content.Context
import com.example.splitbill.data.local.AppDatabase
import com.example.splitbill.data.local.dao.BillDao
import com.example.splitbill.data.local.dao.GroupDao
import com.example.splitbill.data.local.dao.PendingSyncDao
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
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return AppDatabase.getDatabase(context)
  }

  @Provides
  fun provideGroupDao(database: AppDatabase): GroupDao {
    return database.groupDao()
  }

  @Provides
  fun provideBillDao(database: AppDatabase): BillDao {
    return database.billDao()
  }

  @Provides
  fun providePendingSyncDao(database: AppDatabase): PendingSyncDao {
    return database.pendingSyncDao()
  }
}
