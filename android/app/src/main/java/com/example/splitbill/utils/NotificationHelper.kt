package com.example.splitbill.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

import android.app.PendingIntent
import android.content.Intent
import com.example.splitbill.MainActivity

object NotificationHelper {
  private const val CHANNEL_ID = "splitbill_notifications"
  private const val CHANNEL_NAME = "SplitBill Alerts"

  fun showBillNotification(
    context: Context,
    groupId: String? = null,
    billId: String? = null,
    groupName: String,
    billDescription: String,
    amount: String
  ) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Thông báo từ ứng dụng SplitBill"
      }
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      if (!groupId.isNullOrBlank()) putExtra("groupId", groupId)
      if (!billId.isNullOrBlank()) putExtra("billId", billId)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("SplitBill - Hóa đơn mới! 💸")
      .setContentText("Nhóm \"$groupName\": Thêm hóa đơn \"$billDescription\" trị giá $amount")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
