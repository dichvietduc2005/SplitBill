package com.example.splitbill.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class BankTransferEvent(
  val bankName: String,
  val amount: Long,
  val senderName: String?,
  val content: String?,
  val timestamp: Long
)

object BankNotificationBus {
  private val _events = MutableSharedFlow<BankTransferEvent>(replay = 0)
  val events: SharedFlow<BankTransferEvent> = _events.asSharedFlow()

  suspend fun emit(event: BankTransferEvent) {
    _events.emit(event)
  }
}
