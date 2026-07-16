package com.example.splitbill.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.UserStatsResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.components.EmptyState

@Composable
fun StatsScreen(
  viewModel: StatsViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Thống kê chi tiêu",
        actions = {
          IconButton(onClick = { viewModel.loadStats() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    when (val state = uiState) {
      is StatsUiState.Loading -> {
        Box(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      }
      is StatsUiState.Error -> {
        EmptyState(
          title = "Không thể tải thống kê",
          message = state.message,
          emoji = "⚠️",
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is StatsUiState.Success -> {
        StatsContent(
          stats = state.data,
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
    }
  }
}

@Composable
private fun StatsContent(stats: UserStatsResponse, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(Dimens.SpacingM),
    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
  ) {
    // 1. Bento Grid - Summary Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
      ) {
        // Tổng chi tiêu
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
          Column {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Bạn đã chi tiêu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.totalSpent, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
      ) {
        // Bạn nợ
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.errorContainer
        ) {
          Column {
            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Bạn đang nợ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.totalOwedToOthers, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), isDebt = true)
          }
        }

        // Được nợ
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Column {
            Icon(Icons.Default.CallReceived, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Bạn được nợ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.totalOthersOweToMe, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), isDebt = false)
          }
        }
      }
    }

    // 2. Biểu đồ xu hướng chi tiêu hàng tháng (Bar Chart)
    if (stats.monthlyTrend.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            Text(
              "Xu hướng chi tiêu hàng tháng",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpacingL))

            val maxAmount = stats.monthlyTrend.maxOf { it.amount }.coerceAtLeast(1.0)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = Dimens.SpacingS),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.Bottom
            ) {
              stats.monthlyTrend.forEach { trend ->
                val ratio = (trend.amount / maxAmount).toFloat()
                var animHeight by remember { mutableStateOf(0f) }
                LaunchedEffect(ratio) {
                  animate(
                    initialValue = 0f,
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                  ) { value, _ ->
                    animHeight = value
                  }
                }

                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = if (trend.amount >= 1000) "${(trend.amount / 1000).toInt()}k" else "${trend.amount.toInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(Modifier.height(4.dp))
                  Box(
                    modifier = Modifier
                      .fillMaxHeight(0.75f)
                      .fillMaxWidth(0.4f)
                      .graphicsLayer { scaleY = animHeight; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
                      .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                      .background(
                        Brush.verticalGradient(
                          listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                          )
                        )
                      )
                  )
                  Spacer(Modifier.height(Dimens.SpacingS))
                  Text(
                    text = trend.month,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
        }
      }
    }

    // 3. Chi tiêu theo nhóm (Horizontal Progress Bars)
    if (stats.spentByGroup.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            Text(
              "Chi tiêu theo nhóm",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpacingM))

            val maxGroupSpent = stats.spentByGroup.maxOf { it.amount }.coerceAtLeast(1.0)
            stats.spentByGroup.forEach { groupSpent ->
              val ratio = (groupSpent.amount / maxGroupSpent).toFloat()
              var animProgress by remember { mutableStateOf(0f) }
              LaunchedEffect(ratio) {
                animate(
                  initialValue = 0f,
                  targetValue = ratio,
                  animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                ) { value, _ ->
                  animProgress = value
                }
              }

              Column(modifier = Modifier.padding(vertical = Dimens.SpacingS)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    groupSpent.groupName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  AmountText(
                    amount = groupSpent.amount,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { animProgress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                  color = MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
