package com.splitbill.data

import org.jetbrains.exposed.sql.*
import java.util.UUID

data class Group(
    val id: String,
    val name: String,
    val createdBy: String,
    val createdAt: String
)

data class GroupMember(
    val userId: String,
    val username: String,
    val email: String,
    val joinedAt: String
)

object GroupRepository {

    // Tạo nhóm mới và tự động thêm người tạo làm thành viên
    suspend fun createGroup(name: String, createdByUserId: String): Group? = DatabaseFactory.dbQuery {
        val groupId = UUID.fromString(
            Groups.insert {
                it[Groups.name] = name
                it[Groups.createdBy] = UUID.fromString(createdByUserId)
            }.resultedValues?.singleOrNull()?.get(Groups.id)?.toString()
                ?: return@dbQuery null
        )

        // Tự động thêm người tạo vào nhóm
        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = UUID.fromString(createdByUserId)
        }

        Groups.selectAll().where { Groups.id eq groupId }
            .map { resultRowToGroup(it) }
            .singleOrNull()
    }

    // Lấy danh sách nhóm mà user tham gia
    suspend fun getGroupsForUser(userId: String): List<Group> = DatabaseFactory.dbQuery {
        (Groups innerJoin GroupMembers)
            .selectAll().where { GroupMembers.userId eq UUID.fromString(userId) }
            .map { resultRowToGroup(it) }
    }

    // Lấy thông tin một nhóm theo ID
    suspend fun getGroupById(groupId: String): Group? = DatabaseFactory.dbQuery {
        Groups.selectAll().where { Groups.id eq UUID.fromString(groupId) }
            .map { resultRowToGroup(it) }
            .singleOrNull()
    }

    // Thêm thành viên vào nhóm
    suspend fun addMember(groupId: String, userId: String): Boolean = DatabaseFactory.dbQuery {
        // Kiểm tra xem đã là thành viên chưa
        val exists = GroupMembers.selectAll().where {
            (GroupMembers.groupId eq UUID.fromString(groupId)) and
            (GroupMembers.userId eq UUID.fromString(userId))
        }.count() > 0

        if (exists) return@dbQuery false

        GroupMembers.insert {
            it[GroupMembers.groupId] = UUID.fromString(groupId)
            it[GroupMembers.userId] = UUID.fromString(userId)
        }
        true
    }

    // Kiểm tra user có phải thành viên nhóm không
    suspend fun isMember(groupId: String, userId: String): Boolean = DatabaseFactory.dbQuery {
        GroupMembers.selectAll().where {
            (GroupMembers.groupId eq UUID.fromString(groupId)) and
            (GroupMembers.userId eq UUID.fromString(userId))
        }.count() > 0
    }

    // Lấy danh sách thành viên trong nhóm (kèm username, email)
    suspend fun getMembers(groupId: String): List<GroupMember> = DatabaseFactory.dbQuery {
        (GroupMembers innerJoin Users)
            .selectAll().where { GroupMembers.groupId eq UUID.fromString(groupId) }
            .map {
                GroupMember(
                    userId = it[Users.id].toString(),
                    username = it[Users.username],
                    email = it[Users.email],
                    joinedAt = it[GroupMembers.joinedAt].toString()
                )
            }
    }

    // Đếm số thành viên trong nhóm
    suspend fun getMemberCount(groupId: String): Int = DatabaseFactory.dbQuery {
        GroupMembers.selectAll()
            .where { GroupMembers.groupId eq UUID.fromString(groupId) }
            .count().toInt()
    }

    private fun resultRowToGroup(row: ResultRow) = Group(
        id = row[Groups.id].toString(),
        name = row[Groups.name],
        createdBy = row[Groups.createdBy].toString(),
        createdAt = row[Groups.createdAt].toString()
    )
}
