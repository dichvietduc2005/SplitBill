package com.example.splitbill.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.splitbill.data.local.dao.BillDao
import com.example.splitbill.data.local.dao.GroupDao
import com.example.splitbill.data.local.dao.PendingSyncDao
import com.example.splitbill.data.local.entity.BillEntityLocal
import com.example.splitbill.data.local.entity.BillSplitEntityLocal
import com.example.splitbill.data.local.entity.GroupEntityLocal
import com.example.splitbill.data.local.entity.PendingSyncEntity

@Database(
  entities = [
    GroupEntityLocal::class,
    BillEntityLocal::class,
    BillSplitEntityLocal::class,
    PendingSyncEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun groupDao(): GroupDao
  abstract fun billDao(): BillDao
  abstract fun pendingSyncDao(): PendingSyncDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "splitbill_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
