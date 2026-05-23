package com.splitbill.data

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

data class User(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val avatarUrl: String?,
    // Thông tin ngân hàng cho VietQR
    val bankCode: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null
)

object UserRepository {
    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id].toString(),
        username = row[Users.username],
        email = row[Users.email],
        passwordHash = row[Users.passwordHash],
        avatarUrl = row[Users.avatarUrl],
        bankCode = row[Users.bankCode],
        accountNumber = row[Users.accountNumber],
        accountName = row[Users.accountName]
    )

    suspend fun findUserById(id: String): User? = DatabaseFactory.dbQuery {
        Users.selectAll().where { Users.id eq UUID.fromString(id) }
            .map { resultRowToUser(it) }
            .singleOrNull()
    }

    suspend fun findUserByUsername(username: String): User? = DatabaseFactory.dbQuery {
        Users.selectAll().where { Users.username eq username }
            .map { resultRowToUser(it) }
            .singleOrNull()
    }

    suspend fun findUserByEmail(email: String): User? = DatabaseFactory.dbQuery {
        Users.selectAll().where { Users.email eq email }
            .map { resultRowToUser(it) }
            .singleOrNull()
    }

    suspend fun createUser(username: String, email: String, passwordHash: String): User? = DatabaseFactory.dbQuery {
        val insertStatement = Users.insert {
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
        }
        insertStatement.resultedValues?.singleOrNull()?.let { resultRowToUser(it) }
    }

    suspend fun updateBankInfo(userId: String, bankCode: String, accountNumber: String, accountName: String): Boolean = DatabaseFactory.dbQuery {
        val updated = Users.update({ Users.id eq UUID.fromString(userId) }) {
            it[Users.bankCode] = bankCode
            it[Users.accountNumber] = accountNumber
            it[Users.accountName] = accountName
        }
        updated > 0
    }
}
