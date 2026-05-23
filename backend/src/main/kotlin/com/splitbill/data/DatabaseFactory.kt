package com.splitbill.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.util.Properties

object DatabaseFactory {
    
    fun init() {
        val properties = loadProperties()
        
        // Đọc từ local.properties trước, nếu không có thì đọc từ Biến môi trường (System Env)
        val dbUrl = properties.getProperty("db.url") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
        val dbUser = properties.getProperty("db.user") ?: System.getenv("DB_USER") ?: "postgres"
        val dbPassword = properties.getProperty("db.password") ?: System.getenv("DB_PASSWORD") ?: ""

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = 5 // Giới hạn pool size nhỏ để tiết kiệm RAM và tài nguyên Supabase Free tier
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        
        // Tự động tạo các bảng nếu chưa có, và thêm các cột mới nếu có thay đổi schema
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Groups,
                GroupMembers,
                Bills,
                BillSplits
            )
        }
    }

    private fun loadProperties(): Properties {
        val properties = Properties()
        
        // Quét tìm file local.properties ở các thư mục có thể (do gradlew run đôi khi set sai working directory)
        val possiblePaths = listOf(
            "local.properties", 
            "../local.properties", 
            "backend/local.properties",
            System.getProperty("user.dir") + "/local.properties"
        )
        
        for (path in possiblePaths) {
            val localPropertiesFile = File(path)
            if (localPropertiesFile.exists()) {
                println("Đã tìm thấy file cấu hình tại: ${localPropertiesFile.absolutePath}")
                try {
                    FileInputStream(localPropertiesFile).use { properties.load(it) }
                    return properties
                } catch (e: Exception) {
                    println("Lỗi đọc file $path: ${e.message}")
                }
            }
        }
        
        println("CẢNH BÁO: Không tìm thấy file local.properties. Đang sử dụng localhost mặc định...")
        return properties
    }

    // Hàm helper chạy các câu query bất đồng bộ hiệu quả bằng Coroutine Dispatchers.IO
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

// ==========================================
// ĐỊNH NGHĨA CÁC BẢNG (SCHEMA)
// ==========================================

// 1. Bảng User
object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val avatarUrl = varchar("avatar_url", 255).nullable()
    // Thông tin ngân hàng cho VietQR
    val bankCode = varchar("bank_code", 20).nullable()         // Ví dụ: "VCB", "TCB", "MB"
    val accountNumber = varchar("account_number", 30).nullable() // Số tài khoản ngân hàng
    val accountName = varchar("account_name", 100).nullable()   // Tên chủ tài khoản
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

// 2. Bảng Nhóm (Group)
object Groups : Table("groups") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val createdBy = reference("created_by", Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

// 3. Bảng phụ liên kết Thành viên trong Nhóm (Group Member)
object GroupMembers : Table("group_members") {
    val groupId = reference("group_id", Groups.id)
    val userId = reference("user_id", Users.id)
    val joinedAt = datetime("joined_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(groupId, userId)
}

// 4. Bảng Hóa đơn chi tiêu (Bill)
object Bills : Table("bills") {
    val id = uuid("id").autoGenerate()
    val groupId = reference("group_id", Groups.id)
    val description = varchar("description", 255)
    val totalAmount = decimal("total_amount", 15, 2) // Lên tới 999 tỷ đồng, 2 chữ số thập phân
    val paidByUserId = reference("paid_by_user_id", Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

// 5. Chi tiết chia hóa đơn (Ai nợ bao nhiêu trong hóa đơn đó)
object BillSplits : Table("bill_splits") {
    val billId = reference("bill_id", Bills.id)
    val userId = reference("user_id", Users.id)
    val amountOwed = decimal("amount_owed", 15, 2)

    override val primaryKey = PrimaryKey(billId, userId)
}
