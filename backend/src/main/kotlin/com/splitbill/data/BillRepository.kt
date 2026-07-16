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
    val currency: String,
    val exchangeRate: BigDecimal,
    val createdAt: String
)

data class BillSplit(
    val billId: String,
    val userId: String,
    val amountOwed: BigDecimal
)

class BillRepository {

    // Tạo hóa đơn mới kèm danh sách chia nợ
    suspend fun createBill(
        groupId: String,
        description: String,
        totalAmount: Double,
        paidByUserId: String,
        currency: String,
        exchangeRate: Double,
        splits: List<Pair<String, Double>> // List<Pair<userId, amountOwed>>
    ): Bill? = DatabaseFactory.dbQuery {
        val billId = UUID.fromString(
            Bills.insert {
                it[Bills.groupId] = UUID.fromString(groupId)
                it[Bills.description] = description
                it[Bills.totalAmount] = BigDecimal.valueOf(totalAmount)
                it[Bills.paidByUserId] = UUID.fromString(paidByUserId)
                it[Bills.currency] = currency
                it[Bills.exchangeRate] = BigDecimal.valueOf(exchangeRate)
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

    // Lấy danh sách hóa đơn của một nhóm — CÓ PHÂN TRANG
    suspend fun getBillsForGroup(groupId: String, limit: Int = 50, offset: Int = 0): List<Bill> = DatabaseFactory.dbQuery {
        Bills.selectAll().where { Bills.groupId eq UUID.fromString(groupId) }
            .orderBy(Bills.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { resultRowToBill(it) }
    }

    // Đếm tổng số hóa đơn trong nhóm (cho phân trang)
    suspend fun countBillsForGroup(groupId: String): Long = DatabaseFactory.dbQuery {
        Bills.selectAll().where { Bills.groupId eq UUID.fromString(groupId) }
            .count()
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

    // Cập nhật hóa đơn và cập nhật lại danh sách chia nợ
    suspend fun updateBill(
        billId: String,
        description: String,
        totalAmount: Double,
        paidByUserId: String,
        currency: String,
        exchangeRate: Double,
        splits: List<Pair<String, Double>> // List<Pair<userId, amountOwed>>
    ): Bill? = DatabaseFactory.dbQuery {
        val uuid = UUID.fromString(billId)
        
        // 1. Cập nhật bảng Bills
        Bills.update({ Bills.id eq uuid }) {
            it[Bills.description] = description
            it[Bills.totalAmount] = BigDecimal.valueOf(totalAmount)
            it[Bills.paidByUserId] = UUID.fromString(paidByUserId)
            it[Bills.currency] = currency
            it[Bills.exchangeRate] = BigDecimal.valueOf(exchangeRate)
        }

        // 2. Xóa các splits cũ
        BillSplits.deleteWhere { BillSplits.billId eq uuid }

        // 3. Thêm các splits mới
        for ((userId, amount) in splits) {
            BillSplits.insert {
                it[BillSplits.billId] = uuid
                it[BillSplits.userId] = UUID.fromString(userId)
                it[BillSplits.amountOwed] = BigDecimal.valueOf(amount)
            }
        }

        // 4. Trả về thông tin bill sau khi cập nhật
        Bills.selectAll().where { Bills.id eq uuid }
            .map { resultRowToBill(it) }
            .singleOrNull()
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
        currency = row[Bills.currency],
        exchangeRate = row[Bills.exchangeRate],
        createdAt = row[Bills.createdAt].toString()
    )
}
