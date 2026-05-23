package com.splitbill.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.util.UUID

data class Bill(
    val id: String,
    val groupId: String,
    val description: String,
    val totalAmount: BigDecimal,
    val paidByUserId: String,
    val createdAt: String
)

data class BillSplit(
    val billId: String,
    val userId: String,
    val amountOwed: BigDecimal
)

object BillRepository {

    // Tạo hóa đơn mới kèm danh sách chia nợ
    suspend fun createBill(
        groupId: String,
        description: String,
        totalAmount: Double,
        paidByUserId: String,
        splits: List<Pair<String, Double>> // List<Pair<userId, amountOwed>>
    ): Bill? = DatabaseFactory.dbQuery {
        val billId = UUID.fromString(
            Bills.insert {
                it[Bills.groupId] = UUID.fromString(groupId)
                it[Bills.description] = description
                it[Bills.totalAmount] = BigDecimal.valueOf(totalAmount)
                it[Bills.paidByUserId] = UUID.fromString(paidByUserId)
            }.resultedValues?.singleOrNull()?.get(Bills.id)?.toString()
                ?: return@dbQuery null
        )

        // Thêm từng phần chia nợ
        for ((userId, amount) in splits) {
            BillSplits.insert {
                it[BillSplits.billId] = billId
                it[BillSplits.userId] = UUID.fromString(userId)
                it[BillSplits.amountOwed] = BigDecimal.valueOf(amount)
            }
        }

        Bills.selectAll().where { Bills.id eq billId }
            .map { resultRowToBill(it) }
            .singleOrNull()
    }

    // Lấy danh sách hóa đơn của một nhóm
    suspend fun getBillsForGroup(groupId: String): List<Bill> = DatabaseFactory.dbQuery {
        Bills.selectAll().where { Bills.groupId eq UUID.fromString(groupId) }
            .orderBy(Bills.createdAt, SortOrder.DESC)
            .map { resultRowToBill(it) }
    }

    // Lấy thông tin hóa đơn theo ID
    suspend fun getBillById(billId: String): Bill? = DatabaseFactory.dbQuery {
        Bills.selectAll().where { Bills.id eq UUID.fromString(billId) }
            .map { resultRowToBill(it) }
            .singleOrNull()
    }

    // Lấy chi tiết chia nợ của một hóa đơn
    suspend fun getSplitsForBill(billId: String): List<BillSplit> = DatabaseFactory.dbQuery {
        BillSplits.selectAll().where { BillSplits.billId eq UUID.fromString(billId) }
            .map {
                BillSplit(
                    billId = it[BillSplits.billId].toString(),
                    userId = it[BillSplits.userId].toString(),
                    amountOwed = it[BillSplits.amountOwed]
                )
            }
    }

    // Lấy TẤT CẢ splits của TẤT CẢ bills trong nhóm (dùng cho thuật toán tối giản nợ)
    suspend fun getAllSplitsForGroup(groupId: String): List<BillSplit> = DatabaseFactory.dbQuery {
        (BillSplits innerJoin Bills)
            .selectAll().where { Bills.groupId eq UUID.fromString(groupId) }
            .map {
                BillSplit(
                    billId = it[BillSplits.billId].toString(),
                    userId = it[BillSplits.userId].toString(),
                    amountOwed = it[BillSplits.amountOwed]
                )
            }
    }

    // Lấy tất cả bills trong nhóm (dùng cho thuật toán tối giản nợ)
    suspend fun getAllBillsForGroup(groupId: String): List<Bill> = DatabaseFactory.dbQuery {
        Bills.selectAll().where { Bills.groupId eq UUID.fromString(groupId) }
            .map { resultRowToBill(it) }
    }

    // Xóa hóa đơn (cascade xóa splits trước)
    suspend fun deleteBill(billId: String): Boolean = DatabaseFactory.dbQuery {
        val uuid = UUID.fromString(billId)
        BillSplits.deleteWhere { BillSplits.billId eq uuid }
        Bills.deleteWhere { Bills.id eq uuid } > 0
    }

    private fun resultRowToBill(row: ResultRow) = Bill(
        id = row[Bills.id].toString(),
        groupId = row[Bills.groupId].toString(),
        description = row[Bills.description],
        totalAmount = row[Bills.totalAmount],
        paidByUserId = row[Bills.paidByUserId].toString(),
        createdAt = row[Bills.createdAt].toString()
    )
}
