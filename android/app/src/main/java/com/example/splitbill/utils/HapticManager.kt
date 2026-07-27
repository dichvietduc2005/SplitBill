package com.example.splitbill.utils

import android.view.HapticFeedbackConstants
import android.view.View

object HapticManager {
  fun triggerSuccess(view: View?) {
    view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
  }

  fun triggerClick(view: View?) {
    view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
  }
}
