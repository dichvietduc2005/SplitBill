package com.splitbill.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

data class GroupInvite(
    val id: String,
    val groupId: String,
    val inviteCode: String,
    val createdBy: String,
    val expiresAt: String,
    val maxUses: Int?,
    val useCount: Int,
    val createdAt: String
)

class InviteRepository {

    suspend fun createInvite(
        groupId: String,
        inviteCode: String,
        createdBy: String,
        expiresAt: LocalDateTime,
        maxUses: Int? = null
    ): GroupInvite? = DatabaseFactory.dbQuery {
        val inviteId = UUID.fromString(
            GroupInvites.insert {
                it[GroupInvites.groupId] = UUID.fromString(groupId)
                it[GroupInvites.inviteCode] = inviteCode
                it[GroupInvites.createdBy] = UUID.fromString(createdBy)
                it[GroupInvites.expiresAt] = expiresAt
                it[GroupInvites.maxUses] = maxUses
            }.resultedValues?.singleOrNull()?.get(GroupInvites.id)?.toString()
                ?: return@dbQuery null
        )

        GroupInvites.selectAll().where { GroupInvites.id eq inviteId }
            .map { resultRowToInvite(it) }
            .singleOrNull()
    }

    suspend fun findByCode(code: String): GroupInvite? = DatabaseFactory.dbQuery {
        GroupInvites.selectAll().where { GroupInvites.inviteCode eq code }
            .map { resultRowToInvite(it) }
            .singleOrNull()
    }

    suspend fun getInviteById(inviteId: String): GroupInvite? = DatabaseFactory.dbQuery {
        GroupInvites.selectAll().where { GroupInvites.id eq UUID.fromString(inviteId) }
            .map { resultRowToInvite(it) }
            .singleOrNull()
    }

    suspend fun incrementUseCount(inviteId: String): Boolean = DatabaseFactory.dbQuery {
        val uuid = UUID.fromString(inviteId)
        val currentInvite = GroupInvites.selectAll().where { GroupInvites.id eq uuid }
            .singleOrNull() ?: return@dbQuery false
        
        val newUseCount = currentInvite[GroupInvites.useCount] + 1
        GroupInvites.update({ GroupInvites.id eq uuid }) {
            it[GroupInvites.useCount] = newUseCount
        } > 0
    }

    suspend fun getActiveInvites(groupId: String): List<GroupInvite> = DatabaseFactory.dbQuery {
        val now = LocalDateTime.now()
        GroupInvites.selectAll()
            .where { (GroupInvites.groupId eq UUID.fromString(groupId)) and (GroupInvites.inviteCode.isNotNull()) } // standard check
            .map { resultRowToInvite(it) }
            .filter { LocalDateTime.parse(it.expiresAt.substring(0, 19)) > now } // robust datetime parsing
    }

    suspend fun deleteInvite(inviteId: String): Boolean = DatabaseFactory.dbQuery {
        GroupInvites.deleteWhere { GroupInvites.id eq UUID.fromString(inviteId) } > 0
    }

    private fun resultRowToInvite(row: ResultRow) = GroupInvite(
        id = row[GroupInvites.id].toString(),
        groupId = row[GroupInvites.groupId].toString(),
        inviteCode = row[GroupInvites.inviteCode],
        createdBy = row[GroupInvites.createdBy].toString(),
        expiresAt = row[GroupInvites.expiresAt].toString(),
        maxUses = row[GroupInvites.maxUses],
        useCount = row[GroupInvites.useCount],
        createdAt = row[GroupInvites.createdAt].toString()
    )
}
