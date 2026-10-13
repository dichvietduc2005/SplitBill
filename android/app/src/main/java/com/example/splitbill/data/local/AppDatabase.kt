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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    GroupEntityLocal::class,
    BillEntityLocal::class,
    BillSplitEntityLocal::class,
    PendingSyncEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun groupDao(): GroupDao
  abstract fun billDao(): BillDao
  abstract fun pendingSyncDao(): PendingSyncDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(database: SupportSQLiteDatabase) {
        // Thêm các cột mới cho bills_cache
        database.execSQL("ALTER TABLE bills_cache ADD COLUMN paidByUsername TEXT NOT NULL DEFAULT 'Thành viên'")
        database.execSQL("ALTER TABLE bills_cache ADD COLUMN currency TEXT NOT NULL DEFAULT 'VND'")
        database.execSQL("ALTER TABLE bills_cache ADD COLUMN exchangeRate REAL NOT NULL DEFAULT 1.0")
        database.execSQL("ALTER TABLE bills_cache ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")

        // Thêm các cột mới cho bill_splits_cache
        database.execSQL("ALTER TABLE bill_splits_cache ADD COLUMN username TEXT NOT NULL DEFAULT 'Thành viên'")
        database.execSQL("ALTER TABLE bill_splits_cache ADD COLUMN isSettled INTEGER NOT NULL DEFAULT 0")

        // Thêm cột idempotencyKey và status cho pending_sync_actions
        database.execSQL("ALTER TABLE pending_sync_actions ADD COLUMN idempotencyKey TEXT")
        database.execSQL("ALTER TABLE pending_sync_actions ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "splitbill_database"
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigrationOnDowngrade()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
