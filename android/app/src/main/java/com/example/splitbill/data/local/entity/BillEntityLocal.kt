package com.example.splitbill.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills_cache")
data class BillEntityLocal(
  @PrimaryKey val id: String,
  val groupId: String,
  val paidBy: String,
  val title: String,
  val description: String?,
  val totalAmount: Double,
  val billDate: String?,
  val category: String?,
  val receiptImageUrl: String?,
  val splitType: String?,
  val createdAt: String?,
  val isPendingSync: Boolean = false
)
