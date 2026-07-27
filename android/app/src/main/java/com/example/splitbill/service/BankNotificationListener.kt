package com.example.splitbill.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankNotificationListener : NotificationListenerService() {

  companion object {
    val BANK_PACKAGES = mapOf(
      "com.VCB" to "Vietcombank",
      "com.mbmobile" to "MB Bank",
      "vn.com.techcombank.bb.app" to "Techcombank",
      "com.bidv.smartbanking" to "BIDV",
      "com.vietinbank.ipay" to "VietinBank",
      "com.acb.acbmobile" to "ACB",
      "vn.com.tpb.mb.gprsandroid" to "TPBank",
      "com.vpbank.neo" to "VPBank",
      "com.msb.digital" to "MSB",
      "com.sacombank.mbanking" to "Sacombank",
      "com.vib.vibmobile" to "VIB",
      "com.hdb.mobile" to "HDBank",
      "com.ocb.mobilebanking" to "OCB",
      "com.shb.smartbanking" to "SHB",
      "com.lpb.mobilebanking" to "LienVietPostBank",
      "com.vpbank.cake" to "Cake by VPBank",
      // Test Packages (cho phép test qua tin nhắn tự gửi)
      "com.google.android.apps.messaging" to "Google Messages",
      "com.android.mms" to "System SMS",
      "com.zing.zalo" to "Zalo",
      "org.telegram.messenger" to "Telegram",
      "com.facebook.orca" to "Messenger"
    )
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    if (sbn == null) return

    val packageName = sbn.packageName
    val bankName = BANK_PACKAGES[packageName] ?: return

    val extras = sbn.notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

    val fullText = "$title $text $bigText"
    Log.d("BankListener", "Received bank notification from $packageName ($bankName): $fullText")

    val parsed = BankNotificationParser.parse(fullText)
    if (parsed != null) {
      Log.d("BankListener", "Successfully parsed transfer: amount=${parsed.amount}, sender=${parsed.senderName}, content=${parsed.content}")
      CoroutineScope(Dispatchers.IO).launch {
        BankNotificationBus.emit(
          BankTransferEvent(
            bankName = bankName,
            amount = parsed.amount,
            senderName = parsed.senderName,
            content = parsed.content,
            timestamp = System.currentTimeMillis()
          )
        )
      }
    }
  }
}
