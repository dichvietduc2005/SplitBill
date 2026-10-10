package com.example.splitbill.data.local.dao

import androidx.room.*
import com.example.splitbill.data.local.entity.BillEntityLocal
import com.example.splitbill.data.local.entity.BillSplitEntityLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
  @Query("SELECT * FROM bills_cache WHERE groupId = :groupId ORDER BY createdAt DESC")
  fun observeBillsForGroup(groupId: String): Flow<List<BillEntityLocal>>

  @Query("SELECT * FROM bills_cache WHERE groupId = :groupId ORDER BY createdAt DESC")
  suspend fun getBillsForGroup(groupId: String): List<BillEntityLocal>

  @Query("SELECT * FROM bill_splits_cache WHERE billId IN (:billIds)")
  suspend fun getSplitsForBills(billIds: List<String>): List<BillSplitEntityLocal>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBills(bills: List<BillEntityLocal>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBill(bill: BillEntityLocal)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSplits(splits: List<BillSplitEntityLocal>)

  @Query("DELETE FROM bill_splits_cache WHERE billId IN (:billIds)")
  suspend fun deleteSplitsForBills(billIds: List<String>)

  @Transaction
  suspend fun syncBillsWithSplits(groupId: String, bills: List<BillEntityLocal>, splits: List<BillSplitEntityLocal>) {
    val existingBills = getBillsForGroup(groupId)
    val existingIds = existingBills.map { it.id }
    if (existingIds.isNotEmpty()) {
      deleteSplitsForBills(existingIds)
    }
    insertBills(bills)
    if (splits.isNotEmpty()) {
      insertSplits(splits)
    }
  }

  @Query("DELETE FROM bills_cache WHERE id = :billId")
  suspend fun deleteBill(billId: String)
}
