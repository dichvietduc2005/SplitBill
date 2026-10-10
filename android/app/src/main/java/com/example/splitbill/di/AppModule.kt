package com.example.splitbill.di

import android.content.Context
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.SettingsManager
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.TokenManager
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
object AppModule {

  @Provides
  @Singleton
  fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
    return TokenManager(context)
  }

  @Provides
  @Singleton
  fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
    return SettingsManager(context)
  }

  @Provides
  @Singleton
  fun provideGroupRepository(
    tokenManager: TokenManager,
    groupDao: GroupDao
  ): GroupRepository {
    return GroupRepository(tokenManager, groupDao)
  }

  @Provides
  @Singleton
  fun provideBillRepository(
    tokenManager: TokenManager,
    billDao: BillDao,
    pendingSyncDao: PendingSyncDao
  ): BillRepository {
    return BillRepository(tokenManager, billDao, pendingSyncDao)
  }

  @Provides
  @Singleton
  fun provideStatsRepository(tokenManager: TokenManager): StatsRepository {
    return StatsRepository(tokenManager)
  }
}
