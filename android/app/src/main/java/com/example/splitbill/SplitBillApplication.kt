package com.example.splitbill

import android.app.Application
import com.example.splitbill.widget.WidgetDataWorker
import com.example.splitbill.worker.SyncBillsWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SplitBillApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    WidgetDataWorker.enqueuePeriodicWork(this)
    SyncBillsWorker.enqueueOfflineSync(this)
  }
}
