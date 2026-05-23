package com.example.splitbill.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
  private const val CHANNEL_ID = "splitbill_notifications"
  private const val CHANNEL_NAME = "SplitBill Alerts"

  fun showBillNotification(context: Context, groupName: String, billDescription: String, amount: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Thông báo từ ứng dụng SplitBill"
      }
      notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("SplitBill - Hóa đơn mới! 💸")
      .setContentText("Nhóm \"$groupName\": Thêm hóa đơn \"$billDescription\" trị giá $amount")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
