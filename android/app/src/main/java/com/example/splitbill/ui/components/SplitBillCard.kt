package com.example.splitbill.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun SplitBillCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  containerColor: Color = Color.Unspecified,
  contentColor: Color = Color.Unspecified,
  elevation: androidx.compose.ui.unit.Dp = Dimens.ElevationLevel0,
  showBorder: Boolean = false,
  content: @Composable ColumnScope.() -> Unit
) {
  val haptic = LocalHapticFeedback.current

  // Determine actual colors (using M3 surfaceContainer by default for depth)
  val actualContainerColor = if (containerColor == Color.Unspecified) {
    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f) // Glassmorphism semi-transparent
  } else {
    containerColor
  }
  
  val actualContentColor = if (contentColor == Color.Unspecified) {
    MaterialTheme.colorScheme.onSurface
  } else {
    contentColor
  }

  // Soft glow border for glassmorphism
  val border = if (showBorder) {
    BorderStroke(
      width = 1.dp,
      brush = Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.3f),
          Color.White.copy(alpha = 0.05f)
        )
      )
    )
  } else null

  val baseModifier = if (onClick != null) {
    modifier
      .clip(SplitBillShapes.medium)
      .clickable {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
      }
  } else {
    modifier
  }

  Card(
    modifier = baseModifier.shadow(
      elevation = elevation.coerceAtLeast(8.dp),
      shape = SplitBillShapes.medium,
      spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), // Subtle primary-tinted glow
      ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    ),
    shape = SplitBillShapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = actualContainerColor,
      contentColor = actualContentColor
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Using custom shadow above
    border = border
  ) {
    Column(
      modifier = Modifier.padding(Dimens.SpacingM),
      content = content
    )
  }
}
