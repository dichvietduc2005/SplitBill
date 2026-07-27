package com.example.splitbill.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.splitbill.MainActivity
import com.example.splitbill.data.FcmTokenManager
import com.example.splitbill.data.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplitBillMessagingService : FirebaseMessagingService() {

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)

    val groupId = remoteMessage.data["groupId"]
    val type = remoteMessage.data["type"]

    // Show system notification
    val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "SplitBill"
    val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
    showNotification(title, body, groupId)

    // Emit event for UI refresh
    CoroutineScope(Dispatchers.Main).launch {
      NotificationEventBus.emit(NotificationEvent(type, groupId))
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    // Send token to backend if logged in
    val tokenManager = TokenManager(applicationContext)
    val fcmTokenManager = FcmTokenManager(tokenManager)
    CoroutineScope(Dispatchers.IO).launch {
      fcmTokenManager.registerToken(token)
    }
  }

  private fun showNotification(title: String, body: String, groupId: String?) {
    val channelId = "splitbill_notifications"
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Thông báo SplitBill",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Thông báo hóa đơn, thanh toán trong nhóm"
      }
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(this, MainActivity::class.java).apply {
      putExtra("groupId", groupId)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
