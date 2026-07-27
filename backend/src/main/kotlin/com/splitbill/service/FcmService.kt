package com.splitbill.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.splitbill.data.UserRepository
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

class FcmService(private val userRepository: UserRepository) {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    init {
        if (FirebaseApp.getApps().isEmpty()) {
            val possiblePaths = listOf(
                "firebase-service-account.json",
                "backend/firebase-service-account.json",
                "../firebase-service-account.json",
                System.getProperty("user.dir") + "/firebase-service-account.json",
                System.getProperty("user.dir") + "/backend/firebase-service-account.json"
            )
            var fileInputStream: FileInputStream? = null
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists()) {
                    logger.info("Tìm thấy firebase-service-account.json tại: ${file.absolutePath}")
                    try {
                        fileInputStream = FileInputStream(file)
                        break
                    } catch (e: Exception) {
                        logger.error("Lỗi khi mở file $path: ${e.message}")
                    }
                }
            }

            if (fileInputStream != null) {
                try {
                    val options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(fileInputStream))
                        .build()
                    FirebaseApp.initializeApp(options)
                    logger.info("Firebase Admin SDK đã được khởi tạo thành công.")
                } catch (e: Exception) {
                    logger.error("Lỗi khởi tạo Firebase Admin SDK: ${e.message}", e)
                }
            } else {
                logger.warn("CẢNH BÁO: Không tìm thấy firebase-service-account.json. FCM Service sẽ không thể gửi push notification.")
            }
        }
    }

    suspend fun sendToGroup(
        groupId: String,
        excludeUserId: String,
        title: String,
        body: String,
        type: String
    ) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.warn("FirebaseApp chưa được khởi tạo, bỏ qua gửi push notification.")
                return
            }
            val tokens = userRepository.getFcmTokensForGroup(groupId, excludeUserId)
            if (tokens.isEmpty()) {
                logger.info("Không tìm thấy FCM token nào cho nhóm $groupId (trừ user $excludeUserId)")
                return
            }

            logger.info("Gửi thông báo tới ${tokens.size} tokens của nhóm $groupId: $title - $body")
            val notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()

            val message = MulticastMessage.builder()
                .setNotification(notification)
                .putAllData(mapOf(
                    "groupId" to groupId,
                    "type" to type
                ))
                .addAllTokens(tokens)
                .build()

            val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
            logger.info("FCM push thành công: ${response.successCount}, thất bại: ${response.failureCount}")
        } catch (e: Exception) {
            logger.error("Lỗi trong quá trình gửi FCM push: ${e.message}", e)
        }
    }
}
