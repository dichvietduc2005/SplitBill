package com.example.splitbill.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.TokenManager
import java.util.concurrent.TimeUnit

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

  companion object {
    private const val WORK_NAME = "splitbill_widget_periodic_sync"

    fun enqueuePeriodicWork(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val periodicWork = PeriodicWorkRequestBuilder<WidgetDataWorker>(
        15, TimeUnit.MINUTES
      )
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWork
      )
    }

    fun triggerOneTimeUpdate(context: Context) {
      val work = OneTimeWorkRequestBuilder<WidgetDataWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .build()
      WorkManager.getInstance(context).enqueue(work)
    }
  }
}
