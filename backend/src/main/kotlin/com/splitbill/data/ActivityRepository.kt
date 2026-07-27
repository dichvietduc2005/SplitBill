package com.splitbill.data

import org.jetbrains.exposed.sql.*
import java.util.UUID

data class ActivityLog(
    val id: String,
    val groupId: String,
    val userId: String,
    val username: String,
    val activityType: String,
    val description: String,
    val createdAt: String
)

class ActivityRepository {

    suspend fun createLog(
        groupId: String,
        userId: String,
        activityType: String,
        description: String
    ): ActivityLog? = DatabaseFactory.dbQuery {
        val aId = UUID.fromString(
            ActivityLogs.insert {
                it[ActivityLogs.groupId] = UUID.fromString(groupId)
                it[ActivityLogs.userId] = UUID.fromString(userId)
                it[ActivityLogs.activityType] = activityType
                it[ActivityLogs.description] = description
            }.resultedValues?.singleOrNull()?.get(ActivityLogs.id)?.toString()
                ?: return@dbQuery null
        )

        (ActivityLogs innerJoin Users)
            .selectAll().where { ActivityLogs.id eq aId }
            .map { resultRowToActivityLog(it) }
            .singleOrNull()
    }

    suspend fun getActivitiesForGroup(
        groupId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<ActivityLog> = DatabaseFactory.dbQuery {
        (ActivityLogs innerJoin Users)
            .selectAll().where { ActivityLogs.groupId eq UUID.fromString(groupId) }
            .orderBy(ActivityLogs.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { resultRowToActivityLog(it) }
    }

    suspend fun countActivitiesForGroup(groupId: String): Long = DatabaseFactory.dbQuery {
        ActivityLogs.selectAll().where { ActivityLogs.groupId eq UUID.fromString(groupId) }
            .count()
    }

    private fun resultRowToActivityLog(row: ResultRow) = ActivityLog(
        id = row[ActivityLogs.id].toString(),
        groupId = row[ActivityLogs.groupId].toString(),
        userId = row[ActivityLogs.userId].toString(),
        username = row[Users.username],
        activityType = row[ActivityLogs.activityType],
        description = row[ActivityLogs.description],
        createdAt = row[ActivityLogs.createdAt].toString()
    )
}
