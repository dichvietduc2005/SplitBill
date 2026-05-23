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
)

// Custom Tokens for specific use cases
@Stable
class SplitBillCustomColors(
  val positiveAmount: Color,
  val negativeAmount: Color,
  val isLight: Boolean
)

val LocalSplitBillCustomColors = staticCompositionLocalOf {
  SplitBillCustomColors(
    positiveAmount = Color.Unspecified,
    negativeAmount = Color.Unspecified,
    isLight = true
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

  // Instant color scheme transition for smooth performance (eliminates parallel animation jank)
  val animatedColorScheme = targetColorScheme
  
  // Custom colors setup
  val customColors = if (darkTheme) {
    SplitBillCustomColors(positiveAmount = PositiveAmountDark, negativeAmount = NegativeAmountDark, isLight = false)
  } else {
    SplitBillCustomColors(positiveAmount = PositiveAmount, negativeAmount = NegativeAmount, isLight = true)
  }

  // Edge to edge setup for System Bars
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      // In a real app we might want transparent bars to draw behind them, 
      // but WindowCompat setup helps match theme icons.
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

// Extension to animate Material3 ColorScheme
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
  val duration = 400
  return ColorScheme(
    primary = animateColorAsState(target.primary, tween(duration), label = "primary").value,
    onPrimary = animateColorAsState(target.onPrimary, tween(duration), label = "onPrimary").value,
    primaryContainer = animateColorAsState(target.primaryContainer, tween(duration), label = "primaryContainer").value,
    onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, tween(duration), label = "onPrimaryContainer").value,
    inversePrimary = animateColorAsState(target.inversePrimary, tween(duration), label = "inversePrimary").value,
    secondary = animateColorAsState(target.secondary, tween(duration), label = "secondary").value,
    onSecondary = animateColorAsState(target.onSecondary, tween(duration), label = "onSecondary").value,
    secondaryContainer = animateColorAsState(target.secondaryContainer, tween(duration), label = "secondaryContainer").value,
    onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, tween(duration), label = "onSecondaryContainer").value,
    tertiary = animateColorAsState(target.tertiary, tween(duration), label = "tertiary").value,
    onTertiary = animateColorAsState(target.onTertiary, tween(duration), label = "onTertiary").value,
    tertiaryContainer = animateColorAsState(target.tertiaryContainer, tween(duration), label = "tertiaryContainer").value,
    onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, tween(duration), label = "onTertiaryContainer").value,
    background = animateColorAsState(target.background, tween(duration), label = "background").value,
    onBackground = animateColorAsState(target.onBackground, tween(duration), label = "onBackground").value,
    surface = animateColorAsState(target.surface, tween(duration), label = "surface").value,
    onSurface = animateColorAsState(target.onSurface, tween(duration), label = "onSurface").value,
    surfaceVariant = animateColorAsState(target.surfaceVariant, tween(duration), label = "surfaceVariant").value,
    onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, tween(duration), label = "onSurfaceVariant").value,
    surfaceTint = animateColorAsState(target.surfaceTint, tween(duration), label = "surfaceTint").value,
    inverseSurface = animateColorAsState(target.inverseSurface, tween(duration), label = "inverseSurface").value,
    inverseOnSurface = animateColorAsState(target.inverseOnSurface, tween(duration), label = "inverseOnSurface").value,
    error = animateColorAsState(target.error, tween(duration), label = "error").value,
    onError = animateColorAsState(target.onError, tween(duration), label = "onError").value,
    errorContainer = animateColorAsState(target.errorContainer, tween(duration), label = "errorContainer").value,
    onErrorContainer = animateColorAsState(target.onErrorContainer, tween(duration), label = "onErrorContainer").value,
    outline = animateColorAsState(target.outline, tween(duration), label = "outline").value,
    outlineVariant = animateColorAsState(target.outlineVariant, tween(duration), label = "outlineVariant").value,
    scrim = animateColorAsState(target.scrim, tween(duration), label = "scrim").value,
    surfaceBright = animateColorAsState(target.surfaceBright, tween(duration), label = "surfaceBright").value,
    surfaceContainer = animateColorAsState(target.surfaceContainer, tween(duration), label = "surfaceContainer").value,
    surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, tween(duration), label = "surfaceContainerHigh").value,
    surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, tween(duration), label = "surfaceContainerHighest").value,
    surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, tween(duration), label = "surfaceContainerLow").value,
    surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, tween(duration), label = "surfaceContainerLowest").value,
    surfaceDim = animateColorAsState(target.surfaceDim, tween(duration), label = "surfaceDim").value,
  )
}
