package com.splitbill.service

import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties

class StorageService {
    private val logger = LoggerFactory.getLogger(StorageService::class.java)
    private val baseUploadPath = "uploads"

    private val supabaseUrl: String
    private val supabaseKey: String
    private val httpClient = HttpClient.newHttpClient()

    init {
        val rootDir = File(baseUploadPath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        val props = loadLocalProperties()
        supabaseUrl = props.getProperty("supabase.url") 
            ?: System.getenv("SUPABASE_URL") 
            ?: "https://qvqcvwepxluzatmzgiix.supabase.co"
        supabaseKey = props.getProperty("supabase.key") 
            ?: System.getenv("SUPABASE_KEY") 
            ?: ""
        
        logger.info("StorageService initialized. Supabase URL: $supabaseUrl")
    }

    private fun loadLocalProperties(): Properties {
        val properties = Properties()
        val possiblePaths = listOf(
            "local.properties", 
            "../local.properties", 
            "backend/local.properties",
            System.getProperty("user.dir") + "/local.properties"
        )
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                try {
                    file.inputStream().use { properties.load(it) }
                    break
                } catch (e: Exception) {
                    logger.warn("Failed to read properties file $path: ${e.message}")
                }
            }
        }
        return properties
    }

    /**
     * Upload receipt image to Supabase Storage bucket 'receipts', with local fallback.
     */
    fun saveReceipt(groupId: String, billId: String, fileBytes: ByteArray): String {
        val fileName = "${billId}_${System.currentTimeMillis()}.jpg"
        val path = "$groupId/$fileName"
        
        val publicUrl = uploadToSupabaseBucket("receipts", path, fileBytes, "image/jpeg")
        if (publicUrl != null) {
            logger.info("Saved receipt to Supabase Storage: $publicUrl")
            return publicUrl
        }

        // Local fallback
        val groupDir = File("$baseUploadPath/$groupId")
        if (!groupDir.exists()) {
            groupDir.mkdirs()
        }
        val destinationFile = File(groupDir, fileName)
        destinationFile.writeBytes(fileBytes)
        logger.info("Fallback: saved receipt locally: ${destinationFile.absolutePath}")
        return "/uploads/$groupId/$fileName"
    }

    /**
     * Upload user avatar to Supabase Storage bucket 'avatars', with local fallback.
     */
    fun saveAvatar(userId: String, fileBytes: ByteArray, mimeType: String = "image/jpeg"): String {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val fileName = "${userId}_${System.currentTimeMillis()}.$ext"
        
        val publicUrl = uploadToSupabaseBucket("avatars", fileName, fileBytes, mimeType)
        if (publicUrl != null) {
            logger.info("Saved avatar to Supabase Storage: $publicUrl")
            return publicUrl
        }

        // Local fallback
        val avatarDir = File("$baseUploadPath/avatars")
        if (!avatarDir.exists()) {
            avatarDir.mkdirs()
        }
        val destinationFile = File(avatarDir, fileName)
        destinationFile.writeBytes(fileBytes)
        logger.info("Fallback: saved avatar locally: ${destinationFile.absolutePath}")
        return "/uploads/avatars/$fileName"
    }

    private fun uploadToSupabaseBucket(bucket: String, filePath: String, fileBytes: ByteArray, contentType: String): String? {
        if (supabaseKey.isBlank()) {
            return null
        }
        return try {
            val targetUri = URI.create("$supabaseUrl/storage/v1/object/$bucket/$filePath")
            val reqBuilder = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(java.time.Duration.ofSeconds(3))
                .header("Content-Type", contentType)
                .header("x-upsert", "true")

            if (supabaseKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $supabaseKey")
                reqBuilder.header("apikey", supabaseKey)
            }

            val request = reqBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes)).build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                "$supabaseUrl/storage/v1/object/public/$bucket/$filePath"
            } else {
                logger.warn("Supabase Storage upload returned status ${response.statusCode()}: ${response.body()}")
                // Also check if public upload without key works
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to upload to Supabase Storage bucket $bucket: ${e.message}", e)
            null
        }
    }
}
