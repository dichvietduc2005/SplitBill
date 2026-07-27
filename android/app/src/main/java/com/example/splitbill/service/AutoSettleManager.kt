package com.example.splitbill.service

import android.content.Context
import android.util.Log
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.SettingsManager
import com.example.splitbill.data.api.SimplifiedDebt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SettleMatch(
  val groupId: String,
  val groupName: String,
  val debt: SimplifiedDebt,
  val transferEvent: BankTransferEvent
)

class AutoSettleManager(
  private val context: Context,
  private val tokenManager: TokenManager,
  private val settingsManager: SettingsManager,
  private val groupRepository: GroupRepository,
  private val billRepository: BillRepository
) {
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _pendingMatch = MutableSharedFlow<SettleMatch>(replay = 0)
  val pendingMatch: SharedFlow<SettleMatch> = _pendingMatch.asSharedFlow()

  init {
    scope.launch {
      BankNotificationBus.events.collect { event ->
        val enabled = settingsManager.autoSettleEnabled.first()
        if (!enabled) {
          Log.d("AutoSettle", "Auto settle disabled, skipping")
          return@collect
        }

        val myUserId = TokenManager.getUserIdFromToken(tokenManager.getToken().first())
        if (myUserId == null) {
          Log.d("AutoSettle", "User not logged in, skipping")
          return@collect
        }

        processAutoSettle(event, myUserId)
      }
    }
  }

  private fun String.removeDiacritics(): String {
    val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace("đ", "d").replace("Đ", "D")
  }

  private suspend fun processAutoSettle(event: BankTransferEvent, myUserId: String) {
    try {
      val groupsResult = groupRepository.getGroups()
      val groups = groupsResult.getOrNull() ?: return

      for (group in groups) {
        // Lấy danh sách bills của nhóm để khớp hóa đơn cụ thể
        val billsResult = billRepository.getBillsForGroup(group.id)
        val bills = billsResult.getOrNull() ?: emptyList()
        
        // 1. Tìm khớp hóa đơn cụ thể (Match Specific Bill)
        if (event.content != null) {
          val cleanContent = event.content.lowercase().removeDiacritics()
          for (bill in bills) {
            // Tôi phải là người chi tiền (người nhận thanh toán)
            if (bill.paidByUserId == myUserId) {
              val cleanBillDesc = bill.description.lowercase().removeDiacritics()
              if (cleanContent.contains(cleanBillDesc) || cleanBillDesc.contains(cleanContent)) {
                for (split in bill.splits) {
                  if (split.userId != myUserId) {
                    val amountDiff = abs(split.amountOwed - event.amount.toDouble())
                    if (amountDiff <= 1000.0) {
                      var nameMatches = true
                      if (event.senderName != null) {
                        val cleanSender = event.senderName.lowercase().removeDiacritics().replace(" ", "")
                        val cleanDebtor = split.username.lowercase().removeDiacritics().replace(" ", "")
                        nameMatches = cleanSender.contains(cleanDebtor) || cleanDebtor.contains(cleanSender)
                      }
                      
                      if (nameMatches) {
                        Log.d("AutoSettle", "Match specific bill: ${split.username} owes ${split.amountOwed} for '${bill.description}'")
                        executeAutoSettle(
                          groupId = group.id,
                          toUserId = myUserId,
                          fromUserId = split.userId,
                          amount = split.amountOwed,
                          note = "Tự động thanh toán hóa đơn '${bill.description}' qua ${event.bankName}",
                          debtorName = split.username,
                          billDesc = bill.description,
                          billId = bill.id
                        )
                        return
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 2. Tìm khớp tổng nợ tối giản (Match Total Simplified Debt)
        val debtsResult = billRepository.getDebtsForGroup(group.id)
        val debtsResponse = debtsResult.getOrNull() ?: continue

        for (debt in debtsResponse.debts) {
          if (debt.toUserId == myUserId) {
            val amountDiff = abs(debt.amount - event.amount.toDouble())
            if (amountDiff <= 1000.0) {
              var nameMatches = false
              if (event.senderName != null) {
                val cleanSender = event.senderName.lowercase().removeDiacritics().replace(" ", "")
                val cleanFromUsername = debt.fromUsername.lowercase().removeDiacritics().replace(" ", "")
                if (cleanSender.contains(cleanFromUsername) || cleanFromUsername.contains(cleanSender)) {
                  nameMatches = true
                }
              }
              if (event.content != null && !nameMatches) {
                val cleanContent = event.content.lowercase().removeDiacritics().replace(" ", "")
                val cleanFromUsername = debt.fromUsername.lowercase().removeDiacritics().replace(" ", "")
                if (cleanContent.contains(cleanFromUsername)) {
                  nameMatches = true
                }
              }

              if (nameMatches) {
                Log.d("AutoSettle", "Match total debt: ${debt.fromUsername} owes ${debt.toUsername} ${debt.amount}")
                executeAutoSettle(
                  groupId = group.id,
                  toUserId = myUserId,
                  fromUserId = debt.fromUserId,
                  amount = debt.amount,
                  note = "Tự động đối soát tổng nợ qua ${event.bankName}",
                  debtorName = debt.fromUsername,
                  billDesc = null
                )
                return
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e("AutoSettle", "Error processing auto settle: ${e.message}", e)
    }
  }

  private suspend fun executeAutoSettle(
    groupId: String,
    toUserId: String,
    fromUserId: String,
    amount: Double,
    note: String,
    debtorName: String,
    billDesc: String?,
    billId: String? = null
  ) {
    val settlementRepository = com.example.splitbill.data.SettlementRepository(tokenManager)
    val result = settlementRepository.createSettlement(
      groupId = groupId,
      toUserId = toUserId,
      amount = amount,
      note = note,
      fromUserId = fromUserId
    )
    if (result.isSuccess) {
      Log.d("AutoSettle", "Successfully auto-recorded settlement!")
      if (billId != null) {
        billRepository.markBillAsPaid(billId, true)
      }
      NotificationEventBus.emit(NotificationEvent("BILL_UPDATED", groupId))
      showAutoSettleNotification(context, debtorName, amount.toLong(), billDesc)
    } else {
      Log.e("AutoSettle", "Error auto-recording settlement: ${result.exceptionOrNull()?.message}")
    }
  }

  private fun showAutoSettleNotification(context: Context, debtorName: String, amount: Long, billDesc: String?) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "auto_settle_channel"
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(channelId, "Tự động đối soát", android.app.NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
    
    val df = java.text.DecimalFormat("#,###")
    val amountStr = "${df.format(amount)} VND"
    val msg = if (billDesc != null) {
        "Đã tự động thanh toán $amountStr từ $debtorName cho hóa đơn '$billDesc'!"
    } else {
        "Đã tự động đối soát thanh toán $amountStr từ $debtorName!"
    }
    
    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setContentTitle("Thanh toán tự động thành công 💸")
        .setContentText(msg)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
        
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
