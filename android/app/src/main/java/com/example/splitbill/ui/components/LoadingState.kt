package com.example.splitbill.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun ShimmerAnimation(
  modifier: Modifier = Modifier,
  shape: androidx.compose.ui.graphics.Shape = SplitBillShapes.large
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

@Composable
fun ErrorState(
  title: String,
  message: String,
  onRetry: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .padding(Dimens.SpacingXL)
      .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      modifier = Modifier.size(80.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.ErrorOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(38.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(Dimens.SpacingL))

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(Dimens.SpacingXS))

    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = Dimens.SpacingM)
    )

    if (onRetry != null) {
      Spacer(modifier = Modifier.height(Dimens.SpacingL))
      FilledTonalButton(
        onClick = onRetry,
        shape = SplitBillShapes.medium,
        contentPadding = PaddingValues(
          horizontal = Dimens.SpacingL,
          vertical = Dimens.SpacingS
        )
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingS))
        Text(text = "Thử lại", style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

// ─── Skeleton Loading Layouts ───────────────────────────────────────────────

/**
 * Skeleton loading state for GroupListScreen matching the editorial Bento Grid design.
 */
@Composable
fun GroupListSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(Color(0xFFF9F9F6))
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 2-Column Bento Grid of Editorial Group Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Left Column
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        EditorialGroupCardSkeleton(imageHeight = 112.dp)
        EditorialGroupCardSkeleton(imageHeight = 136.dp)
      }
      // Right Column
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        EditorialGroupCardSkeleton(imageHeight = 136.dp)
        EditorialGroupCardSkeleton(imageHeight = 112.dp)
      }
    }
  }
}

@Composable
fun EditorialGroupCardSkeleton(
  modifier: Modifier = Modifier,
  imageHeight: androidx.compose.ui.unit.Dp = 112.dp
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
    border = BorderStroke(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.8f)),
    shadowElevation = 1.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // Image Thumbnail Shimmer
      ShimmerAnimation(
        modifier = Modifier
          .fillMaxWidth()
          .height(imageHeight),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Group Name
      ShimmerAnimation(
        modifier = Modifier
          .fillMaxWidth(0.8f)
          .height(15.dp),
        shape = RoundedCornerShape(4.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Total spent subtext
      ShimmerAnimation(
        modifier = Modifier
          .fillMaxWidth(0.55f)
          .height(11.dp),
        shape = RoundedCornerShape(4.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Overlapping member avatars
      Row(
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(3) {
          ShimmerAnimation(
            modifier = Modifier.size(20.dp),
            shape = CircleShape
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Divider line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(Color(0xFFF0F0EC))
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Balance status badge
      ShimmerAnimation(
        modifier = Modifier
          .fillMaxWidth(0.65f)
          .height(11.dp),
        shape = RoundedCornerShape(4.dp)
      )
    }
  }
}

/**
 * Skeleton loading state for HomeScreen matching Scenic Hero + Warm Floating Sheet design.
 */
@Composable
fun HomeScreenSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF9F9F6))
  ) {
    // 1. Scenic Hero Section Shimmer (Height ~ 300dp, dark navy)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
        .background(Color(0xFF011C27))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top row: Greeting + Action icons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            ShimmerAnimation(
              modifier = Modifier
                .width(64.dp)
                .height(14.dp),
              shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerAnimation(
              modifier = Modifier
                .width(150.dp)
                .height(28.dp),
              shape = RoundedCornerShape(6.dp)
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ShimmerAnimation(
              modifier = Modifier.size(38.dp),
              shape = CircleShape
            )
            ShimmerAnimation(
              modifier = Modifier.size(42.dp),
              shape = CircleShape
            )
          }
        }

        // Subtitle quote shimmer
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
          ShimmerAnimation(
            modifier = Modifier
              .width(180.dp)
              .height(14.dp),
            shape = RoundedCornerShape(4.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          ShimmerAnimation(
            modifier = Modifier
              .width(120.dp)
              .height(14.dp),
            shape = RoundedCornerShape(4.dp)
          )
        }
      }
    }

    // 2. Warm Floating Sheet Shimmer (Offset over hero section)
    Box(
      modifier = Modifier
        .offset(y = (-24).dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
        .background(Color.White)
        .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
      Column {
        // "TỔNG QUAN" title
        ShimmerAnimation(
          modifier = Modifier
            .width(80.dp)
            .height(12.dp),
          shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2 Financial Summary Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          repeat(2) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFFF9F9F6),
              border = BorderStroke(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(1f)
                .height(100.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                ShimmerAnimation(
                  modifier = Modifier
                    .width(70.dp)
                    .height(12.dp),
                  shape = RoundedCornerShape(4.dp)
                )
                ShimmerAnimation(
                  modifier = Modifier
                    .width(100.dp)
                    .height(20.dp),
                  shape = RoundedCornerShape(4.dp)
                )
                ShimmerAnimation(
                  modifier = Modifier
                    .width(50.dp)
                    .height(10.dp),
                  shape = RoundedCornerShape(4.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CTA Button shimmer
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // "NHÓM GẦN ĐÂY" header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          ShimmerAnimation(
            modifier = Modifier
              .width(120.dp)
              .height(14.dp),
            shape = RoundedCornerShape(4.dp)
          )
          ShimmerAnimation(
            modifier = Modifier
              .width(60.dp)
              .height(12.dp),
            shape = RoundedCornerShape(4.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recent groups horizontal cards shimmer
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          repeat(2) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFFF9F9F6),
              border = BorderStroke(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(1f)
                .height(115.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(10.dp)
              ) {
                ShimmerAnimation(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                  shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerAnimation(
                  modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp),
                  shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerAnimation(
                  modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(10.dp),
                  shape = RoundedCornerShape(4.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun GroupDetailSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(Color(0xFFF9F9F6))
      .fillMaxSize()
  ) {
    // 1. Top Hero Banner Shimmer (Khung ảnh phong cảnh tràn viền)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(260.dp)
        .background(Color(0xFFE2E2DE))
    ) {
      ShimmerAnimation(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(0.dp)
      )
      // Lớp phủ chuyển sắc tối phía dưới
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color(0xFF011C27).copy(alpha = 0.88f))
            )
          )
      )
      // Shimmer Tên nhóm & Thành viên đè lên ảnh
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 20.dp, vertical = 24.dp)
      ) {
        ShimmerAnimation(
          modifier = Modifier
            .width(200.dp)
            .height(26.dp),
          shape = RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(6.dp))
        ShimmerAnimation(
          modifier = Modifier
            .width(130.dp)
            .height(14.dp),
          shape = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.height(10.dp))

        // Avatars stack shimmer
        Row(
          horizontalArrangement = Arrangement.spacedBy((-8).dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(4) {
            ShimmerAnimation(
              modifier = Modifier.size(26.dp),
              shape = CircleShape
            )
          }
          Spacer(Modifier.width(14.dp))
          ShimmerAnimation(
            modifier = Modifier
              .width(70.dp)
              .height(12.dp),
            shape = RoundedCornerShape(4.dp)
          )
        }
      }
    }

    // 2. 3-Column Financial Summary Card Shimmer (Độ khối chuẩn nổi đè lên chân ảnh banner)
    Box(
      modifier = Modifier
        .offset(y = (-14).dp)
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Cột 1
          Column(modifier = Modifier.weight(1f)) {
            ShimmerAnimation(modifier = Modifier.width(45.dp).height(12.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerAnimation(modifier = Modifier.width(75.dp).height(18.dp))
          }
          Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFEEEEEB)))
          // Cột 2
          Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            ShimmerAnimation(modifier = Modifier.width(60.dp).height(12.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerAnimation(modifier = Modifier.width(70.dp).height(18.dp))
          }
          Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFEEEEEB)))
          // Cột 3
          Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            ShimmerAnimation(modifier = Modifier.width(65.dp).height(12.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerAnimation(modifier = Modifier.width(70.dp).height(18.dp))
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
    ) {
      // 3. Tabs Navigation Shimmer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        ShimmerAnimation(modifier = Modifier.width(80.dp).height(16.dp))
        ShimmerAnimation(modifier = Modifier.width(80.dp).height(16.dp))
      }
      Spacer(Modifier.height(12.dp))
      HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEB))

      Spacer(Modifier.height(12.dp))

      // 4. Category Filter Chips Shimmer
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        repeat(4) {
          ShimmerAnimation(
            modifier = Modifier
              .width(72.dp)
              .height(34.dp),
            shape = RoundedCornerShape(100.dp)
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      // 5. Bill list items shimmer in Card container (Độ khối chuẩn HomeScreen)
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          repeat(4) { idx ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                ShimmerAnimation(
                  modifier = Modifier.size(40.dp),
                  shape = CircleShape
                )
                Column {
                  ShimmerAnimation(modifier = Modifier.width(110.dp).height(14.dp))
                  Spacer(Modifier.height(6.dp))
                  ShimmerAnimation(modifier = Modifier.width(80.dp).height(11.dp))
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                ShimmerAnimation(modifier = Modifier.width(75.dp).height(14.dp))
                Spacer(Modifier.height(4.dp))
                ShimmerAnimation(modifier = Modifier.width(40.dp).height(10.dp))
              }
            }
            if (idx < 3) {
              HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFEEEEEB),
                modifier = Modifier.padding(horizontal = 4.dp)
              )
            }
          }
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

@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    // 1. Header Card Skeleton (User Info)
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ShimmerAnimation(
          modifier = Modifier.size(56.dp),
          shape = CircleShape
        )
        Spacer(Modifier.width(Dimens.SpacingM))
        Column {
          ShimmerAnimation(
            modifier = Modifier
              .width(120.dp)
              .height(18.dp)
          )
          Spacer(Modifier.height(Dimens.SpacingXS))
          ShimmerAnimation(
            modifier = Modifier
              .width(180.dp)
              .height(14.dp)
          )
        }
      }
    }

    // 2. VietQR Card Skeleton
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ShimmerAnimation(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp)
          )
          Spacer(Modifier.width(Dimens.SpacingS))
          ShimmerAnimation(
            modifier = Modifier
              .width(140.dp)
              .height(16.dp)
          )
        }
        Spacer(Modifier.height(Dimens.SpacingS))
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
        )
      }
    }

    // 3. Bank Info Card Skeleton (Form)
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)) {
        ShimmerAnimation(
          modifier = Modifier
            .width(130.dp)
            .height(16.dp)
        )
        // Bank Selection box skeleton
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp)
        )
        // Account Number textfield skeleton
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp)
        )
        // Account Name textfield skeleton
        ShimmerAnimation(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp)
        )
      }
    }

    Spacer(Modifier.height(Dimens.SpacingS))

    // 4. Save Button Skeleton
    ShimmerAnimation(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      shape = RoundedCornerShape(12.dp)
    )
  }
}

@Composable
fun ActivityFeedSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    repeat(4) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(80.dp)
      ) {
        ShimmerAnimation(
          modifier = Modifier.size(36.dp),
          shape = CircleShape
        )
        Spacer(Modifier.width(Dimens.SpacingM))
        SplitBillCard(modifier = Modifier.weight(1f)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              ShimmerAnimation(modifier = Modifier.width(100.dp).height(14.dp))
              ShimmerAnimation(modifier = Modifier.width(70.dp).height(10.dp))
            }
            Spacer(Modifier.height(Dimens.SpacingS))
            ShimmerAnimation(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
          }
        }
      }
    }
  }
}

@Composable
fun GroupStatsSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    // Total spent card skeleton
    SplitBillCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        ShimmerAnimation(modifier = Modifier.width(120.dp).height(12.dp))
        Spacer(Modifier.height(Dimens.SpacingS))
        ShimmerAnimation(modifier = Modifier.width(160.dp).height(28.dp))
      }
    }

    // Members contribution grid skeleton
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.BentoGap)
    ) {
      repeat(2) {
        SplitBillCard(modifier = Modifier.weight(1f)) {
          Column {
            ShimmerAnimation(modifier = Modifier.size(36.dp), shape = CircleShape)
            Spacer(Modifier.height(Dimens.SpacingS))
            ShimmerAnimation(modifier = Modifier.width(80.dp).height(12.dp))
            Spacer(Modifier.height(Dimens.SpacingXXS))
            ShimmerAnimation(modifier = Modifier.width(60.dp).height(16.dp))
          }
        }
      }
    }

    // Chart card skeleton
    SplitBillCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
      Column {
        ShimmerAnimation(modifier = Modifier.width(140.dp).height(16.dp))
        Spacer(Modifier.height(Dimens.SpacingM))
        ShimmerAnimation(modifier = Modifier.fillMaxWidth().height(120.dp))
      }
    }
  }
}

