package com.example.splitbill.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEventBus {
  private val _events = MutableSharedFlow<NotificationEvent>(replay = 0)
  val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

  suspend fun emit(event: NotificationEvent) {
    _events.emit(event)
  }
}

data class NotificationEvent(
  val type: String?,
  val groupId: String?
)
