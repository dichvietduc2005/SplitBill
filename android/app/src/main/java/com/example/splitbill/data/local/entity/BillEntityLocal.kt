package com.example.splitbill.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills_cache")
data class BillEntityLocal(
  @PrimaryKey val id: String,
  val groupId: String,
  val paidBy: String,
  val paidByUsername: String = "Thành viên",
  val title: String,
  val description: String?,
  val totalAmount: Double,
  val billDate: String? = null,
  val currency: String = "VND",
  val exchangeRate: Double = 1.0,
  val category: String? = "GENERAL",
  val receiptImageUrl: String? = null,
  val isPaid: Boolean = false,
  val splitType: String? = "EQUAL",
  val createdAt: String?,
  val isPendingSync: Boolean = false
)
