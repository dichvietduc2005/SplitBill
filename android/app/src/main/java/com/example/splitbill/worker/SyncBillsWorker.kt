package com.example.splitbill.worker

import android.content.Context
import androidx.work.*
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.local.AppDatabase
import com.example.splitbill.widget.WidgetDataWorker
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncBillsWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val database = AppDatabase.getDatabase(appContext)
    val pendingDao = database.pendingSyncDao()
    val tokenManager = TokenManager(appContext)
    val billRepository = BillRepository(
      tokenManager = tokenManager,
      billDao = database.billDao(),
      pendingSyncDao = pendingDao
    )

    val pendingActions = pendingDao.getAllPendingActions()
    if (pendingActions.isEmpty()) {
      return Result.success()
    }

    var hasFailure = false

    for (action in pendingActions) {
      try {
        when (action.actionType) {
          "CREATE_BILL" -> {
            val json = JSONObject(action.payload)
            val groupId = json.getString("groupId")
            val amount = json.getDouble("amount")
            val description = json.getString("description")
            val splitType = json.optString("splitType", "EQUAL")
            val currency = json.optString("currency", "VND")
            val category = json.optString("category", "OTHER")

            val splitsArray = json.getJSONArray("splits")
            val splits = mutableListOf<com.example.splitbill.data.api.BillSplitItem>()
            for (i in 0 until splitsArray.length()) {
              val sObj = splitsArray.getJSONObject(i)
              splits.add(
                com.example.splitbill.data.api.BillSplitItem(
                  userId = sObj.getString("userId"),
                  amount = sObj.optDouble("amount", sObj.optDouble("amountOwed", 0.0))
                )
              )
            }

            val paidByUserId = json.optString("paidByUserId", tokenManager.getUserId() ?: "")
            val exchangeRate = json.optDouble("exchangeRate", 1.0)

            val result = billRepository.createBill(
              groupId = groupId,
              description = description,
              totalAmount = amount,
              paidByUserId = paidByUserId,
              currency = currency,
              exchangeRate = exchangeRate,
              splits = splits,
              category = category,
              clientBillId = action.idempotencyKey
            )

            if (result.isSuccess) {
              pendingDao.deleteAction(action.id)
            } else {
              hasFailure = true
              pendingDao.incrementRetry(action.id)
              if (action.retryCount >= 5) {
                // Tạm dừng retry liên tục để tránh hao tổn pin và nghẽn worker
                pendingDao.updateStatus(action.id, "FAILED")
              }
            }
          }
          else -> {
            // Không xóa nhưng đánh dấu 'UNSUPPORTED' để tránh Worker liên tục retry vô tận tác vụ chưa hỗ trợ
            android.util.Log.w("SyncBillsWorker", "Unsupported actionType '${action.actionType}' with ID ${action.id}, marking UNSUPPORTED to prevent retry loop.")
            pendingDao.updateStatus(action.id, "UNSUPPORTED")
          }
        }
      } catch (e: Exception) {
        hasFailure = true
        pendingDao.incrementRetry(action.id)
        if (action.retryCount >= 5) {
          pendingDao.updateStatus(action.id, "FAILED")
        }
      }
    }

    // Cập nhật lại số liệu cho widget
    WidgetDataWorker.triggerOneTimeUpdate(appContext)

    return if (hasFailure) Result.retry() else Result.success()
  }

  companion object {
    private const val WORK_NAME = "splitbill_sync_pending_bills"

    fun enqueueOfflineSync(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val syncWork = OneTimeWorkRequestBuilder<SyncBillsWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        syncWork
      )
    }
  }
}
