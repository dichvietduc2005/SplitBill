package com.example.splitbill.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.GradientOceanEnd
import com.example.splitbill.theme.GradientOceanStart
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun GradientButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = SplitBillShapes.medium,
  gradient: Brush = Brush.horizontalGradient(listOf(GradientOceanStart, GradientOceanEnd)),
  shadowColor: Color = GradientOceanStart,
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  content: @Composable RowScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  
  // Scale down slightly when pressed for a nice interaction feel
  val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "button_scale")

  val backgroundBrush = if (enabled) gradient else Brush.linearGradient(
    listOf(
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
  )

  Surface(
    modifier = modifier
      .scale(scale)
      .clip(shape)
      .shadow(
        elevation = if (enabled && !isPressed) 8.dp else 2.dp,
        shape = shape,
        spotColor = if (enabled) shadowColor else Color.Transparent,
        ambientColor = if (enabled) shadowColor else Color.Transparent
      )
      .clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.foundation.LocalIndication.current,
        enabled = enabled,
        onClick = onClick
      ),
    color = Color.Transparent,
    shape = shape
  ) {
    Box(
      modifier = Modifier
        .background(backgroundBrush)
        .padding(contentPadding),
      contentAlignment = Alignment.Center
    ) {
      ProvideTextStyle(
        value = MaterialTheme.typography.labelLarge.copy(
          color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
      ) {
        androidx.compose.foundation.layout.Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
          content = content
        )
      }
    }
  }
}
