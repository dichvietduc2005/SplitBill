package com.example.splitbill.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.TokenManager

class WidgetDataWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return try {
      val tokenManager = TokenManager(appContext)
      val statsRepository = StatsRepository(tokenManager)
      val statsResult = statsRepository.getUserStats()

      if (statsResult.isSuccess) {
        val stats = statsResult.getOrNull()!!
        val prefs = appContext.getSharedPreferences("splitbill_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
          .putLong("owed_to_others", stats.totalOwedToOthers.toLong())
          .putLong("others_owe_me", stats.totalOthersOweToMe.toLong())
          .apply()

        SplitBillWidget().updateAll(appContext)
      }
      Result.success()
    } catch (e: Exception) {
      Result.retry()
    }
  }
}
