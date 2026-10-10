package com.example.splitbill.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_splits_cache")
data class BillSplitEntityLocal(
  @PrimaryKey(autoGenerate = true) val localId: Long = 0,
  val billId: String,
  val userId: String,
  val amountOwed: Double,
  val percentage: Double? = null
)
