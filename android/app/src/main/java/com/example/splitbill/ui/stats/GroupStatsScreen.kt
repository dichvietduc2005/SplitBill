package com.example.splitbill.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.EmptyState
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
  val rawBills by viewModel.rawBills.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Thống kê chi tiêu nhóm",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        actions = {
          IconButton(onClick = { showExportSheet = true }) {
            Icon(Icons.Rounded.FileDownload, contentDescription = "Xuất báo cáo", tint = MaterialTheme.colorScheme.primary)
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
        var isRefreshing by remember { mutableStateOf(false) }
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            viewModel.loadGroupStats { isRefreshing = false }
          },
          state = pullRefreshState,
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
          GroupStatsContent(
            stats = state.data,
            categoryBreakdown = categoryBreakdown,
            rawBills = rawBills,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

@Composable
private fun GroupStatsContent(
  stats: com.example.splitbill.data.api.GroupStatsResponse,
  categoryBreakdown: List<CategorySpending>,
  rawBills: List<com.example.splitbill.data.api.BillResponse>,
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
            Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(Dimens.SpacingXS))
            Text("Phần của bạn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            AmountText(amount = stats.userOwed, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // 2. Single Unified Chart Card with Segmented Tab Control
    item {
      var selectedChartTab by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(StatsChartTab.CATEGORY)
      }

      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(Dimens.SpacingM)) {
          // --- Segmented Pill Tab Switcher ---
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            StatsChartTab.entries.forEach { tab ->
              val isSelected = selectedChartTab == tab
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .height(36.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.surface
                    else Color.Transparent
                  )
                  .clickable { selectedChartTab = tab }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(Dimens.SpacingM))

          // --- Animated Content Switcher ---
          AnimatedContent(
            targetState = selectedChartTab,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "chart_tab_switcher"
          ) { currentTab ->
            when (currentTab) {
              StatsChartTab.MONTHLY -> {
                Column {
                  Text(
                    "Chi tiêu hàng tháng của nhóm",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(Modifier.height(Dimens.SpacingL))

                  if (stats.monthlyTrend.isEmpty()) {
                    Text(
                      "Chưa có dữ liệu hàng tháng",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(vertical = Dimens.SpacingL)
                    )
                  } else {
                    com.example.splitbill.ui.components.MonthlyBarChart(
                      monthlyData = stats.monthlyTrend
                    )
                  }
                }
              }

              StatsChartTab.CATEGORY -> {
                Column {
                  Text(
                    "Phân bố chi tiêu theo danh mục",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(Modifier.height(Dimens.SpacingS))

                  // Category Month Navigation (< Tất cả thời gian > or < Tháng 08/2026 >)
                  val monthOptions = remember(stats.monthlyTrend) {
                    listOf<String?>(null) + stats.monthlyTrend.map { it.month }
                  }
                  var monthOptionIndex by remember { mutableIntStateOf(0) }
                  val selectedCategoryMonth = monthOptions.getOrNull(monthOptionIndex)

                  if (monthOptions.size > 1) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingXS),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      IconButton(
                        onClick = { if (monthOptionIndex > 0) monthOptionIndex-- },
                        enabled = monthOptionIndex > 0,
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                          contentDescription = "Tháng trước",
                          tint = if (monthOptionIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                      }

                      Text(
                        text = if (selectedCategoryMonth == null) "Tất cả thời gian" else "Tháng $selectedCategoryMonth",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                      )

                      IconButton(
                        onClick = { if (monthOptionIndex < monthOptions.lastIndex) monthOptionIndex++ },
                        enabled = monthOptionIndex < monthOptions.lastIndex,
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                          contentDescription = "Tháng sau",
                          tint = if (monthOptionIndex < monthOptions.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                      }
                    }
                  }

                  val (displayBreakdown, displayTotal) = remember(categoryBreakdown, rawBills, selectedCategoryMonth, stats.totalSpent) {
                    if (selectedCategoryMonth == null) {
                      Pair(categoryBreakdown, stats.totalSpent)
                    } else {
                      val filteredBills = rawBills.filter { bill ->
                        if (bill.createdAt.length >= 7) {
                          val monthStr = bill.createdAt.substring(5, 7) + "/" + bill.createdAt.substring(0, 4)
                          monthStr == selectedCategoryMonth
                        } else false
                      }
                      val monthTotal = filteredBills.sumOf { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) }
                      val breakdown = if (monthTotal > 0) {
                        filteredBills.groupBy { it.category }
                          .map { (catKey, catBills) ->
                            val catTotal = catBills.sumOf { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) }
                            CategorySpending(
                              categoryKey = catKey,
                              totalAmount = catTotal,
                              percentage = ((catTotal / monthTotal) * 100).toFloat()
                            )
                          }
                          .sortedByDescending { it.totalAmount }
                      } else emptyList()
                      Pair(breakdown, monthTotal)
                    }
                  }

                  if (displayBreakdown.isEmpty()) {
                    Text(
                      "Chưa có dữ liệu danh mục cho thời gian này",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(vertical = Dimens.SpacingL)
                    )
                  } else {
                    // Ultra-Clean Donut Chart & Legend
                    com.example.splitbill.ui.components.CategoryDonutChart(
                      categoryBreakdown = displayBreakdown,
                      totalAmount = displayTotal
                    )
                  }
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
  }
}

enum class StatsChartTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  MONTHLY("Theo tháng", Icons.Rounded.BarChart),
  CATEGORY("Theo danh mục", Icons.Rounded.PieChart)
}
