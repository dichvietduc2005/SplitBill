package com.example.splitbill.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
  primary = PrimaryLight,
  onPrimary = OnPrimaryLight,
  primaryContainer = PrimaryContainerLight,
  onPrimaryContainer = OnPrimaryContainerLight,
  secondary = SecondaryLight,
  onSecondary = OnSecondaryLight,
  secondaryContainer = SecondaryContainerLight,
  onSecondaryContainer = OnSecondaryContainerLight,
  tertiary = TertiaryLight,
  onTertiary = OnTertiaryLight,
  tertiaryContainer = TertiaryContainerLight,
  onTertiaryContainer = OnTertiaryContainerLight,
  error = ErrorLight,
  onError = OnErrorLight,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  surfaceContainerLowest = SurfaceContainerLowestLight,
  surfaceContainerLow = SurfaceContainerLowLight,
  surfaceContainer = SurfaceContainerLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  surfaceContainerHighest = SurfaceContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryDark,
  onPrimary = OnPrimaryDark,
  primaryContainer = PrimaryContainerDark,
  onPrimaryContainer = OnPrimaryContainerDark,
  secondary = SecondaryDark,
  onSecondary = OnSecondaryDark,
  secondaryContainer = SecondaryContainerDark,
  onSecondaryContainer = OnSecondaryContainerDark,
  tertiary = TertiaryDark,
  onTertiary = OnTertiaryDark,
  tertiaryContainer = TertiaryContainerDark,
  onTertiaryContainer = OnTertiaryContainerDark,
  error = ErrorDark,
  onError = OnErrorDark,
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  surfaceContainerLowest = SurfaceContainerLowestDark,
  surfaceContainerLow = SurfaceContainerLowDark,
  surfaceContainer = SurfaceContainerDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  surfaceContainerHighest = SurfaceContainerHighestDark
)

// Custom Tokens for specific use cases
@Stable
class SplitBillCustomColors(
  val positiveAmount: Color,
  val negativeAmount: Color,
  val isLight: Boolean,
  // Badges
  val badgeGroupBg: Color,
  val badgeGroupIcon: Color,
  val badgeBillBg: Color,
  val badgeBillIcon: Color,
  val badgeStatsBg: Color,
  val badgeStatsIcon: Color,
  val badgeMemberBg: Color,
  val badgeMemberIcon: Color
)

val LocalSplitBillCustomColors = staticCompositionLocalOf {
  SplitBillCustomColors(
    positiveAmount = Color.Unspecified,
    negativeAmount = Color.Unspecified,
    isLight = true,
    badgeGroupBg = Color.Unspecified,
    badgeGroupIcon = Color.Unspecified,
    badgeBillBg = Color.Unspecified,
    badgeBillIcon = Color.Unspecified,
    badgeStatsBg = Color.Unspecified,
    badgeStatsIcon = Color.Unspecified,
    badgeMemberBg = Color.Unspecified,
    badgeMemberIcon = Color.Unspecified
  )
}

@Composable
fun SplitBillTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true, // Dynamic color is available on Android 12+
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  
  // Decide which target scheme to animate to
  val targetColorScheme = when {
    dynamicColor && isDynamicSupported && darkTheme -> dynamicDarkColorScheme(context)
    dynamicColor && isDynamicSupported && !darkTheme -> dynamicLightColorScheme(context)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  // Instant color scheme transition for smooth performance
  val animatedColorScheme = targetColorScheme
  
  // Custom colors setup
  val customColors = if (darkTheme) {
    SplitBillCustomColors(
      positiveAmount = PositiveAmountDark, 
      negativeAmount = NegativeAmountDark, 
      isLight = false,
      badgeGroupBg = BadgeGroupBgDark,
      badgeGroupIcon = BadgeGroupIconDark,
      badgeBillBg = BadgeBillBgDark,
      badgeBillIcon = BadgeBillIconDark,
      badgeStatsBg = BadgeStatsBgDark,
      badgeStatsIcon = BadgeStatsIconDark,
      badgeMemberBg = BadgeMemberBgDark,
      badgeMemberIcon = BadgeMemberIconDark
    )
  } else {
    SplitBillCustomColors(
      positiveAmount = PositiveAmount, 
      negativeAmount = NegativeAmount, 
      isLight = true,
      badgeGroupBg = BadgeGroupBg,
      badgeGroupIcon = BadgeGroupIcon,
      badgeBillBg = BadgeBillBg,
      badgeBillIcon = BadgeBillIcon,
      badgeStatsBg = BadgeStatsBg,
      badgeStatsIcon = BadgeStatsIcon,
      badgeMemberBg = BadgeMemberBg,
      badgeMemberIcon = BadgeMemberIcon
    )
  }

  // Edge to edge setup for System Bars
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
  }

  CompositionLocalProvider(LocalSplitBillCustomColors provides customColors) {
    MaterialTheme(
      colorScheme = animatedColorScheme,
      typography = Typography,
      shapes = SplitBillShapes,
      content = content
    )
  }
}
