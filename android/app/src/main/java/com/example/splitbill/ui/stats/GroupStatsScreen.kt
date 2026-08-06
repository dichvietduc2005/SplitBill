package com.example.splitbill.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.EmptyState
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.platform.LocalContext
import com.example.splitbill.ui.components.ExportBottomSheet
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar

@Composable
fun GroupStatsScreen(
  viewModel: GroupStatsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var showExportSheet by remember { mutableStateOf(false) }

  val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Thống kê chi tiêu nhóm",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        actions = {
          IconButton(onClick = { showExportSheet = true }) {
            Icon(Icons.Default.FileDownload, contentDescription = "Xuất báo cáo", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = { viewModel.loadGroupStats() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    if (showExportSheet) {
      ExportBottomSheet(
        onDismiss = { showExportSheet = false },
        onExportPdf = { viewModel.exportPdf(context, "Chi_tieu_nhom") },
        onExportCsv = { viewModel.exportCsv(context, "Chi_tieu_nhom") }
      )
    }

    when (val state = uiState) {
      is GroupStatsUiState.Loading -> {
        com.example.splitbill.ui.components.GroupStatsSkeleton(
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is GroupStatsUiState.Error -> {
        EmptyState(
          title = "Không thể tải thống kê",
          message = state.message,
          emoji = "⚠️",
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is GroupStatsUiState.Success -> {
        GroupStatsContent(
          stats = state.data,
          categoryBreakdown = categoryBreakdown,
          modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        )
      }
    }
  }
}

@Composable
private fun GroupStatsContent(
  stats: com.example.splitbill.data.api.GroupStatsResponse,
  categoryBreakdown: List<CategorySpending>,
  modifier: Modifier = Modifier
) {
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
        // Tổng chi nhóm
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
          Column {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Tổng chi tiêu nhóm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.totalSpent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
      ) {
        // Bạn đã trả
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Column {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Bạn đã chi trả", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.userSpent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        }

        // Thực tế bạn tiêu
        SplitBillCard(
          modifier = Modifier.weight(1f),
          containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) {
          Column {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Phần của bạn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.userOwed, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // 2. Spending Trend (Bar Chart)
    if (stats.monthlyTrend.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            Text(
              "Chi tiêu hàng tháng của nhóm",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpacingL))

            val maxAmount = stats.monthlyTrend.maxOf { it.amount }.coerceAtLeast(1.0)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
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

    // 3. Chi tiêu của từng thành viên (Horizontal Progress Bars)
    if (stats.memberSpending.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            Text(
              "Tỷ lệ đóng góp chi tiêu",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpacingM))

            val maxMemberSpent = stats.memberSpending.maxOf { it.amount }.coerceAtLeast(1.0)
            stats.memberSpending.forEach { memberSpent ->
              val ratio = (memberSpent.amount / maxMemberSpent).toFloat()
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
                    memberSpent.username,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  AmountText(
                    amount = memberSpent.amount,
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

    // 4. Phân tích chi tiêu theo Danh mục
    if (categoryBreakdown.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            Text(
              "Chi tiêu theo danh mục",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpacingM))

            categoryBreakdown.forEach { catSpending ->
              val category = com.example.splitbill.data.model.BillCategory.fromKey(catSpending.categoryKey)
              val ratio = (catSpending.percentage / 100f).coerceIn(0f, 1f)
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
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(category.bgColor),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = category.icon,
                        contentDescription = category.displayName,
                        tint = category.iconColor,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                    Spacer(Modifier.width(Dimens.SpacingS))
                    Text(
                      category.displayName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    AmountText(
                      amount = catSpending.totalAmount,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                      "${String.format(java.util.Locale.US, "%.1f", catSpending.percentage)}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { animProgress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                  color = category.iconColor,
                  trackColor = category.bgColor
                )
              }
            }
          }
        }
      }
    }
  }
}
