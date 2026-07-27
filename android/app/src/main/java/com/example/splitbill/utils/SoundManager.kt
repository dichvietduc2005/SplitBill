package com.example.splitbill.utils

import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {
  private var toneGenerator: ToneGenerator? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    } catch (e: Exception) {
      toneGenerator = null
    }
  }

  fun playSuccessSound() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    } catch (_: Exception) {}
  }

  fun playPaymentDoneSound() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
    } catch (_: Exception) {}
  }
}
