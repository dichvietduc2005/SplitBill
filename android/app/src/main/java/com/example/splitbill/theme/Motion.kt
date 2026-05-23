package com.example.splitbill.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object Motion {
  // Durations
  const val DurationFast = 150
  const val DurationMedium = 300
  const val DurationSlow = 500
  const val DurationEntranceLong = 700

  // Stagger
  const val StaggerDelay = 60L // ms between each item
  const val StaggerDelayLong = 100L

  // Easings
  val EasingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
  val EasingDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
  val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

  // Tweens
  fun <T> tweenFast() = tween<T>(durationMillis = DurationFast, easing = EasingStandard)
  fun <T> tweenMedium() = tween<T>(durationMillis = DurationMedium, easing = EasingStandard)
  fun <T> tweenSlow() = tween<T>(durationMillis = DurationSlow, easing = EasingDecelerate)
  fun <T> tweenEntrance() = tween<T>(durationMillis = DurationEntranceLong, easing = EasingDecelerate)

  // Springs
  fun <T> springBouncy() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
  )

  fun <T> springGentle() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
  )

  fun <T> springSnappy() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium
  )

  // Entrance / Exit combos
  fun staggeredSlideIn(index: Int): EnterTransition =
    slideInVertically(
      animationSpec = tween(
        durationMillis = DurationSlow,
        delayMillis = (index * StaggerDelay).toInt(),
        easing = EasingDecelerate
      ),
      initialOffsetY = { it / 3 }
    ) + fadeIn(
      animationSpec = tween(
        durationMillis = DurationMedium,
        delayMillis = (index * StaggerDelay).toInt(),
        easing = EasingStandard
      )
    )

  val slideUp: EnterTransition = slideInVertically(
    animationSpec = tween(DurationSlow, easing = EasingDecelerate),
    initialOffsetY = { it / 4 }
  ) + fadeIn(tween(DurationMedium, easing = EasingStandard))

  val slideOut: ExitTransition = slideOutVertically(
    animationSpec = tween(DurationFast, easing = EasingStandard),
    targetOffsetY = { -it / 6 }
  ) + fadeOut(tween(DurationFast))
}
