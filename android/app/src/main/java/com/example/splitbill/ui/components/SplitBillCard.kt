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
  elevation: androidx.compose.ui.unit.Dp = 16.dp, // Default larger elevation
  shape: androidx.compose.ui.graphics.Shape = SplitBillShapes.large,
  content: @Composable ColumnScope.() -> Unit
) {
  val haptic = LocalHapticFeedback.current

  // Glassmorphism removed in favor of solid Bento colors.
  val actualContainerColor = if (containerColor == Color.Unspecified) {
    MaterialTheme.colorScheme.surfaceContainerLowest 
  } else {
    containerColor
  }
  
  val actualContentColor = if (contentColor == Color.Unspecified) {
    MaterialTheme.colorScheme.onSurface
  } else {
    contentColor
  }

  val shadowModifier = modifier.shadow(
    elevation = elevation,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.22f), // Stronger shadow
    ambientColor = Color.Black.copy(alpha = 0.08f)
  )

  val finalModifier = if (onClick != null) {
    shadowModifier
      .clip(shape)
      .clickable {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
      }
  } else {
    shadowModifier
  }

  Card(
    modifier = finalModifier,
    shape = shape,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) // Faint, modern border to define edges without being harsh
    ),
    colors = CardDefaults.cardColors(
      containerColor = actualContainerColor,
      contentColor = actualContentColor
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Handled by custom shadow modifier above
  ) {
    Column(
      modifier = Modifier.padding(Dimens.SpacingM),
      content = content
    )
  }
}
