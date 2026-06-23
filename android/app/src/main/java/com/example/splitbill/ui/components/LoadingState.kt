package com.example.splitbill.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun ShimmerAnimation(
  modifier: Modifier = Modifier,
  shape: androidx.compose.ui.graphics.Shape = SplitBillShapes.medium
) {
  val shimmerColors = listOf(
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
  )

  val transition = rememberInfiniteTransition(label = "shimmer_transition")
  val translateAnim = transition.animateFloat(
    initialValue = -500f,
    targetValue = 1500f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_translate"
  )

  val brush = Brush.linearGradient(
    colors = shimmerColors,
    start = Offset(x = translateAnim.value - 200f, y = translateAnim.value - 200f),
    end = Offset(x = translateAnim.value, y = translateAnim.value)
  )

  Box(
    modifier = modifier
      .background(brush = brush, shape = shape)
  )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String? = null) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 4.dp,
        modifier = Modifier.size(48.dp)
      )
      if (message != null) {
        Spacer(modifier = Modifier.height(Dimens.SpacingM))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun EmptyState(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  emoji: String = "💸"
) {
  // Bounce animation for the emoji
  val infiniteTransition = rememberInfiniteTransition(label = "empty_state_bounce")
  val offset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bounce"
  )

  Column(
    modifier = modifier.padding(Dimens.SpacingXL),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(120.dp)
        .graphicsLayer { translationY = offset }
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), SplitBillShapes.extraLarge),
      contentAlignment = Alignment.Center
    ) {
      Text(emoji, style = MaterialTheme.typography.displayLarge)
    }
    
    Spacer(modifier = Modifier.height(Dimens.SpacingL))
    
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(Dimens.SpacingS))
    
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
  }
}

// ─── Skeleton Loading Layouts ───────────────────────────────────────────────

@Composable
fun GroupListSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    repeat(4) {
      SplitBillCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          ShimmerAnimation(
            modifier = Modifier.size(40.dp),
            shape = SplitBillShapes.small
          )
          Spacer(Modifier.width(Dimens.SpacingM))
          Column(modifier = Modifier.weight(1f)) {
            ShimmerAnimation(
              modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
            )
            Spacer(Modifier.height(Dimens.SpacingS))
            ShimmerAnimation(
              modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(12.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun GroupDetailSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    // Summary card skeleton
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          repeat(3) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              ShimmerAnimation(
                modifier = Modifier
                  .width(60.dp)
                  .height(12.dp)
              )
              Spacer(Modifier.height(Dimens.SpacingXS))
              ShimmerAnimation(
                modifier = Modifier
                  .width(48.dp)
                  .height(20.dp)
              )
            }
          }
        }
        Spacer(Modifier.height(Dimens.SpacingM))
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = SplitBillShapes.medium
        )
      }
    }

    // Bill title skeleton
    ShimmerAnimation(
      modifier = Modifier
        .width(120.dp)
        .height(18.dp)
    )

    // Bill cards skeleton
    repeat(3) {
      SplitBillCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            ShimmerAnimation(
              modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(16.dp)
            )
            Spacer(Modifier.height(Dimens.SpacingXS))
            ShimmerAnimation(
              modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(12.dp)
            )
          }
          ShimmerAnimation(
            modifier = Modifier
              .width(72.dp)
              .height(20.dp)
          )
        }
      }
    }
  }
}

@Composable
fun DebtSummarySkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    // Header card
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ShimmerAnimation(modifier = Modifier.size(24.dp), shape = SplitBillShapes.small)
          Spacer(Modifier.width(Dimens.SpacingS))
          ShimmerAnimation(modifier = Modifier.width(140.dp).height(22.dp))
        }
        Spacer(Modifier.height(Dimens.SpacingM))
        ShimmerAnimation(modifier = Modifier.fillMaxWidth().height(1.dp))
        Spacer(Modifier.height(Dimens.SpacingS))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
          ShimmerAnimation(modifier = Modifier.width(180.dp).height(14.dp))
          ShimmerAnimation(modifier = Modifier.width(80.dp).height(14.dp))
        }
      }
    }

    // Debt cards
    repeat(3) {
      SplitBillCard(modifier = Modifier.fillMaxWidth()) {
        Column {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShimmerAnimation(modifier = Modifier.size(40.dp), shape = SplitBillShapes.small)
            Spacer(Modifier.width(Dimens.SpacingS))
            Column(Modifier.weight(1f)) {
              ShimmerAnimation(modifier = Modifier.width(100.dp).height(14.dp))
              Spacer(Modifier.height(4.dp))
              ShimmerAnimation(modifier = Modifier.width(40.dp).height(10.dp))
              Spacer(Modifier.height(4.dp))
              ShimmerAnimation(modifier = Modifier.width(80.dp).height(14.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
              ShimmerAnimation(modifier = Modifier.size(20.dp), shape = SplitBillShapes.small)
              Spacer(Modifier.height(4.dp))
              ShimmerAnimation(modifier = Modifier.width(80.dp).height(18.dp))
            }
          }
          Spacer(Modifier.height(Dimens.SpacingM))
          ShimmerAnimation(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = SplitBillShapes.medium
          )
        }
      }
    }
  }
}
