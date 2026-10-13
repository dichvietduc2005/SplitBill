package com.example.splitbill.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val actionType: String, // CREATE_BILL, DELETE_BILL, SETTLE_DEBT
  val payload: String,    // JSON serialized payload
  val idempotencyKey: String? = null, // Khóa chống trùng lặp khi retry
  val status: String = "PENDING",     // PENDING, COMPLETED, FAILED, UNSUPPORTED
  val createdAt: Long = System.currentTimeMillis(),
  val retryCount: Int = 0
)
