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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun SplitBillCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
  elevation: androidx.compose.ui.unit.Dp = Dimens.ElevationLevel1,
  showBorder: Boolean = false,
  content: @Composable ColumnScope.() -> Unit
) {
  val border = if (showBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
  
  val cardModifier = if (onClick != null) {
    modifier
      .clip(SplitBillShapes.medium)
      .clickable(onClick = onClick)
  } else {
    modifier
  }

  Card(
    modifier = cardModifier,
    shape = SplitBillShapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = containerColor,
      contentColor = contentColor
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    border = border
  ) {
    Column(
      modifier = Modifier.padding(Dimens.SpacingM),
      content = content
    )
  }
}
