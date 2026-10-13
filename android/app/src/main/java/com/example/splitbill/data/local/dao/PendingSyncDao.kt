package com.example.splitbill.data.local.dao

import androidx.room.*
import com.example.splitbill.data.local.entity.PendingSyncEntity

@Dao
interface PendingSyncDao {
  @Query("SELECT * FROM pending_sync_actions WHERE status = 'PENDING' ORDER BY createdAt ASC")
  suspend fun getAllPendingActions(): List<PendingSyncEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAction(action: PendingSyncEntity): Long

  @Query("DELETE FROM pending_sync_actions WHERE id = :id")
  suspend fun deleteAction(id: Long)

  @Query("UPDATE pending_sync_actions SET retryCount = retryCount + 1 WHERE id = :id")
  suspend fun incrementRetry(id: Long)

  @Query("UPDATE pending_sync_actions SET status = :status WHERE id = :id")
  suspend fun updateStatus(id: Long, status: String)

  @Query("SELECT COUNT(*) FROM pending_sync_actions WHERE status = 'PENDING'")
  suspend fun getPendingCount(): Int
}
