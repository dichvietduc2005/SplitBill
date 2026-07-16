package com.splitbill.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.util.UUID

data class Settlement(
    val id: String,
    val groupId: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: BigDecimal,
    val note: String?,
    val createdAt: String
)

class SettlementRepository {

    suspend fun createSettlement(
        groupId: String,
        fromUserId: String,
        toUserId: String,
        amount: Double,
        note: String?
    ): Settlement? = DatabaseFactory.dbQuery {
        val sId = UUID.fromString(
            Settlements.insert {
                it[Settlements.groupId] = UUID.fromString(groupId)
                it[Settlements.fromUserId] = UUID.fromString(fromUserId)
                it[Settlements.toUserId] = UUID.fromString(toUserId)
                it[Settlements.amount] = BigDecimal.valueOf(amount)
                it[Settlements.note] = note
            }.resultedValues?.singleOrNull()?.get(Settlements.id)?.toString()
                ?: return@dbQuery null
        )

        Settlements.selectAll().where { Settlements.id eq sId }
            .map { resultRowToSettlement(it) }
            .singleOrNull()
    }

    suspend fun getSettlementsForGroup(groupId: String): List<Settlement> = DatabaseFactory.dbQuery {
        Settlements.selectAll().where { Settlements.groupId eq UUID.fromString(groupId) }
            .orderBy(Settlements.createdAt, SortOrder.DESC)
            .map { resultRowToSettlement(it) }
    }

    suspend fun deleteSettlement(settlementId: String): Boolean = DatabaseFactory.dbQuery {
        Settlements.deleteWhere { Settlements.id eq UUID.fromString(settlementId) } > 0
    }

    suspend fun getSettlementById(settlementId: String): Settlement? = DatabaseFactory.dbQuery {
        Settlements.selectAll().where { Settlements.id eq UUID.fromString(settlementId) }
            .map { resultRowToSettlement(it) }
            .singleOrNull()
    }

    private fun resultRowToSettlement(row: ResultRow) = Settlement(
        id = row[Settlements.id].toString(),
        groupId = row[Settlements.groupId].toString(),
        fromUserId = row[Settlements.fromUserId].toString(),
        toUserId = row[Settlements.toUserId].toString(),
        amount = row[Settlements.amount],
        note = row[Settlements.note],
        createdAt = row[Settlements.createdAt].toString()
    )
}
