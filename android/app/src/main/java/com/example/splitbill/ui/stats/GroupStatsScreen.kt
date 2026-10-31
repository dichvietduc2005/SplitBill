package com.example.splitbill.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.GroupStatsResponse
import com.example.splitbill.data.api.MemberSpent
import com.example.splitbill.data.api.MonthlySpent
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.theme.LocalSplitBillCustomColors
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.CategoryDonutChart
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.ExportBottomSheet
import com.example.splitbill.ui.components.GroupStatsSkeleton
import com.example.splitbill.ui.components.MonthlyBarChart
import com.example.splitbill.ui.components.formatCompactAmount
import com.example.splitbill.ui.components.formatVndFull
import com.example.splitbill.ui.localization.localized
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)

/* ------------------------------------------------------------------ screen */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupStatsScreen(
  viewModel: GroupStatsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
  val rawBills by viewModel.rawBills.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var showExportSheet by remember { mutableStateOf(false) }
  var period by rememberSaveable { mutableStateOf(StatsPeriod.THIS_MONTH) }
  // Cần id người dùng hiện tại để biết khoản nào "bạn đã trả" và phần chia của bạn
  val currentUserId = remember { runCatching { TokenManager(context).getUserId() }.getOrNull() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      val subtitle = listOfNotNull(
        period.label.localized(),
        period.rangeText()
      ).joinToString(" · ")

      StatsHeader(
        subtitle = subtitle,
        onNavigateBack = onNavigateBack,
        onExportClick = { showExportSheet = true }
      )

      when (val state = uiState) {
        is GroupStatsUiState.Loading -> {
          GroupStatsSkeleton(modifier = Modifier.fillMaxSize())
        }

        is GroupStatsUiState.Error -> {
          EmptyState(
            title = "Không thể tải thống kê".localized(),
            message = state.message,
            emoji = "⚠️",
            modifier = Modifier.fillMaxSize()
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
            modifier = Modifier.fillMaxSize()
          ) {
            GroupStatsContent(
              stats = state.data,
              categoryBreakdown = categoryBreakdown,
              rawBills = rawBills,
              period = period,
              onPeriodChange = { period = it },
              currentUserId = currentUserId,
              onExportClick = { showExportSheet = true },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }

    if (showExportSheet) {
      ExportBottomSheet(
        onDismiss = { showExportSheet = false },
        onExportPdf = { viewModel.exportPdf(context, "Chi_tieu_nhom") },
        onExportCsv = { viewModel.exportCsv(context, "Chi_tieu_nhom") }
      )
    }
  }
}

/* ------------------------------------------------------------------ header */

@Composable
private fun StatsHeader(
  subtitle: String,
  onNavigateBack: () -> Unit,
  onExportClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Nút quay lại kiểu trang chi tiết nhóm: hình tròn nền nhạt + mũi tên mảnh
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
        .clickable(onClick = onNavigateBack),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.ArrowBackIosNew,
        contentDescription = "Quay lại".localized(),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(16.dp)
      )
    }

    Spacer(Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Thống kê nhóm".localized(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(Modifier.width(8.dp))

    // Không dùng avatar: thay bằng nút xuất báo cáo
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onExportClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.FileDownload,
        contentDescription = "Xuất báo cáo".localized(),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

/* ----------------------------------------------------------------- content */

@Composable
private fun GroupStatsContent(
  stats: GroupStatsResponse,
  categoryBreakdown: List<CategorySpending>,
  rawBills: List<BillResponse>,
  period: StatsPeriod,
  onPeriodChange: (StatsPeriod) -> Unit,
  currentUserId: String?,
  onExportClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Dữ liệu thô có sẵn thì tính theo kỳ đang chọn, nếu không thì dùng số liệu máy chủ trả về
  val hasBillData = rawBills.isNotEmpty()

  val filteredBills = remember(period, rawBills) {
    if (period == StatsPeriod.ALL_TIME || rawBills.isEmpty()) {
      rawBills
    } else {
      val keys = period.windowKeys()
      rawBills.filter { billMonthKey(it.createdAt) in keys }
    }
  }

  val totalAmount = remember(filteredBills, hasBillData, stats.totalSpent) {
    if (hasBillData) filteredBills.sumOf { it.normalizedAmount() } else stats.totalSpent
  }

  val breakdown = remember(filteredBills, categoryBreakdown, hasBillData, totalAmount) {
    if (!hasBillData) {
      categoryBreakdown
    } else if (totalAmount <= 0) {
      emptyList()
    } else {
      filteredBills.groupBy { it.category }
        .map { (categoryKey, bills) ->
          val categoryTotal = bills.sumOf { it.normalizedAmount() }
          CategorySpending(
            categoryKey = categoryKey,
            totalAmount = categoryTotal,
            percentage = (categoryTotal / totalAmount * 100).toFloat()
          )
        }
        .sortedByDescending { it.totalAmount }
    }
  }

  val myPaid = remember(filteredBills, hasBillData, stats.userSpent, currentUserId) {
    if (hasBillData) {
      filteredBills.filter { it.paidByUserId == currentUserId }.sumOf { it.normalizedAmount() }
    } else {
      stats.userSpent
    }
  }

  val myShare = remember(filteredBills, hasBillData, stats.userOwed, currentUserId) {
    if (hasBillData) {
      filteredBills.sumOf { bill ->
        val rate = if (bill.exchangeRate > 0) bill.exchangeRate else 1.0
        bill.splits.filter { it.userId == currentUserId }.sumOf { it.amountOwed * rate }
      }
    } else {
      stats.userOwed
    }
  }

  val members = remember(filteredBills, stats.memberSpending, hasBillData) {
    if (!hasBillData) {
      stats.memberSpending
    } else {
      filteredBills.groupBy { it.paidByUserId }
        .map { (userId, bills) ->
          MemberSpent(
            userId = userId,
            username = bills.firstOrNull()?.paidByUsername ?: "",
            amount = bills.sumOf { it.normalizedAmount() }
          )
        }
        .sortedByDescending { it.amount }
    }
  }

  val trend = remember(stats.monthlyTrend) { stats.monthlyTrend.takeLast(6) }
  val recentBills = remember(filteredBills) { filteredBills.sortedByDescending { it.createdAt }.take(3) }

  // Tỷ lệ so với kỳ trước (theo lần), dùng để tránh % vô lý khi kỳ trước quá nhỏ
  val changeRatio = remember(period, rawBills, totalAmount, hasBillData) {
    if (!hasBillData || period == StatsPeriod.ALL_TIME) {
      null
    } else {
      val previousTotal = rawBills
        .filter { billMonthKey(it.createdAt) in period.previousWindowKeys() }
        .sumOf { it.normalizedAmount() }
      if (previousTotal > 0) totalAmount / previousTotal else null
    }
  }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      PeriodPills(selected = period, onSelect = onPeriodChange)
    }

    item {
      TotalSpendCard(
        totalAmount = totalAmount,
        changeRatio = changeRatio,
        period = period
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        SummaryCard(
          title = "Bạn đã chi trả".localized(),
          amount = myPaid,
          icon = Icons.Rounded.AccountBalanceWallet,
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        SummaryCard(
          title = "Phần của bạn".localized(),
          amount = myShare,
          icon = Icons.Rounded.Person,
          accent = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f)
        )
      }
    }

    if (trend.isNotEmpty()) {
      item { TrendCard(trend = trend) }
    }

    item {
      CategoryCard(
        breakdown = breakdown,
        totalAmount = totalAmount,
        period = period
      )
    }

    if (members.isNotEmpty()) {
      item { MemberContributionCard(members = members) }
    }

    if (recentBills.isNotEmpty()) {
      item {
        RecentBillsCard(
          bills = recentBills,
          currentUserId = currentUserId
        )
      }
    }

    item {
      ExportReportButton(onClick = onExportClick)
    }
  }
}

/* ------------------------------------------------------------------- cards */

@Composable
private fun StatCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(
        elevation = 3.dp,
        shape = CardShape,
        spotColor = Color.Black.copy(alpha = 0.06f),
        ambientColor = Color.Black.copy(alpha = 0.04f)
      )
      .clip(CardShape)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(16.dp),
    content = content
  )
}

@Composable
private fun PeriodPills(
  selected: StatsPeriod,
  onSelect: (StatsPeriod) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    StatsPeriod.entries.forEach { option ->
      val isSelected = option == selected
      Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onSelect(option) }
      ) {
        Text(
          text = option.label.localized(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
          color = if (isSelected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun TotalSpendCard(
  totalAmount: Double,
  changeRatio: Double?,
  period: StatsPeriod
) {
  StatCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Tổng chi tiêu nhóm".localized().uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
      ) {
        Text(
          text = period.label.localized(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    val heroStyle = MaterialTheme.typography.displaySmall.copy(
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface
    )
    if (totalAmount >= 1_000_000_000) {
      // Con số lên tới hàng tỷ: hiển thị rút gọn cho vừa một dòng, kèm số đầy đủ bên dưới
      Text(
        text = "${formatCompactAmount(totalAmount)} ₫",
        style = heroStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = formatVndFull(totalAmount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    } else {
      AmountText(amount = totalAmount, style = heroStyle)
    }

    Spacer(Modifier.height(12.dp))

    ChangeHint(changeRatio = changeRatio)
  }
}

@Composable
private fun ChangeHint(changeRatio: Double?) {
  val customColors = LocalSplitBillCustomColors.current
  val rising = changeRatio != null && changeRatio > 1.005
  val falling = changeRatio != null && changeRatio < 0.995
  val tint = when {
    rising -> customColors.negativeAmount
    falling -> customColors.positiveAmount
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  val icon = when {
    rising -> Icons.Rounded.ArrowUpward
    falling -> Icons.Rounded.ArrowDownward
    else -> Icons.Rounded.Remove
  }

  // Kỳ trước quá nhỏ thì "%" sẽ vô lý (hàng trăm nghìn %), khi đó nói theo lần cho dễ hiểu
  val text = when {
    changeRatio == null -> "Chưa đủ dữ liệu để so sánh".localized()
    rising && changeRatio >= 11.0 -> "Tăng gấp %s lần so với kỳ trước".localized()
      .format(String.format(Locale.GERMANY, "%,.0f", changeRatio))
    rising -> "Tăng %s%% so với kỳ trước".localized()
      .format(String.format(Locale.US, "%.0f", (changeRatio - 1) * 100))
    falling -> "Giảm %s%% so với kỳ trước".localized()
      .format(String.format(Locale.US, "%.0f", (1 - changeRatio) * 100))
    else -> "Không đổi so với kỳ trước".localized()
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    Spacer(Modifier.width(6.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun SummaryCard(
  title: String,
  amount: Double,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier
) {
  StatCard(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(26.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
      }
      Spacer(Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(Modifier.height(10.dp))
    Text(
      text = "${formatCompactAmount(amount)} ₫",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = accent,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun TrendCard(trend: List<MonthlySpent>) {
  StatCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Xu hướng chi tiêu".localized(),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "6 tháng gần nhất".localized(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Icon(
        imageVector = Icons.Rounded.BarChart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(Modifier.height(12.dp))

    MonthlyBarChart(monthlyData = trend)
  }
}

@Composable
private fun CategoryCard(
  breakdown: List<CategorySpending>,
  totalAmount: Double,
  period: StatsPeriod
) {
  StatCard {
    Column {
      Text(
        text = "Chi tiêu theo danh mục".localized(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = period.label.localized(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(Modifier.height(12.dp))

    if (breakdown.isEmpty()) {
      Text(
        text = "Chưa có khoản chi nào trong kỳ này".localized(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
      )
    } else {
      CategoryDonutChart(
        categoryBreakdown = breakdown,
        totalAmount = totalAmount
      )
    }
  }
}

@Composable
private fun MemberContributionCard(members: List<MemberSpent>) {
  StatCard {
    Text(
      text = "Tỷ lệ đóng góp chi tiêu".localized(),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(4.dp))

    val maxMemberSpent = remember(members) { members.maxOf { it.amount }.coerceAtLeast(1.0) }

    members.forEach { memberSpent ->
      val ratio = (memberSpent.amount / maxMemberSpent).toFloat()
      var animProgress by remember(memberSpent.userId, ratio) { mutableStateOf(0f) }
      LaunchedEffect(ratio) {
        animate(
          initialValue = 0f,
          targetValue = ratio,
          animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        ) { value, _ ->
          animProgress = value
        }
      }

      Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = memberSpent.username,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = "${formatCompactAmount(memberSpent.amount)} ₫",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
          )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { animProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
      }
    }
  }
}

@Composable
private fun RecentBillsCard(
  bills: List<BillResponse>,
  currentUserId: String?
) {
  StatCard {
    Text(
      text = "Khoản chi gần đây".localized(),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Phần bạn chi trả trong nhóm".localized(),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(6.dp))

    bills.forEachIndexed { index, bill ->
      RecentBillRow(bill = bill, currentUserId = currentUserId)
      if (index != bills.lastIndex) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
      }
    }
  }
}

@Composable
private fun RecentBillRow(
  bill: BillResponse,
  currentUserId: String?
) {
  val category = BillCategory.fromKey(bill.category)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(13.dp))
        .background(category.bgColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = category.icon,
        contentDescription = category.displayName,
        tint = category.iconColor,
        modifier = Modifier.size(19.dp)
      )
    }

    Spacer(Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = bill.description,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${shortDate(bill.createdAt)} · ${category.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(Modifier.width(8.dp))

    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = "${formatCompactAmount(bill.normalizedAmount())} ₫",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
      Text(
        text = if (bill.paidByUserId == currentUserId) "Bạn đã trả".localized() else bill.paidByUsername,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ExportReportButton(onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Rounded.IosShare,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = "Xuất báo cáo chi tiêu".localized(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/* ------------------------------------------------------------------ period */

/**
 * Kỳ báo cáo của màn thống kê nhóm. [months] = 0 nghĩa là lấy toàn bộ dữ liệu.
 */
private enum class StatsPeriod(val label: String, val months: Int) {
  THIS_MONTH("Tháng này", 1),
  LAST_THREE_MONTHS("3 tháng gần đây", 3),
  ALL_TIME("Tất cả thời gian", 0)
}

/** Khoảng tháng của kỳ hiện tại, dạng "MM/yyyy" giống dữ liệu monthlyTrend */
private fun StatsPeriod.windowKeys(startOffsetMonths: Int = 0): Set<String> =
  (startOffsetMonths until startOffsetMonths + months).map { monthKey(it) }.toSet()

/** Khoảng tháng ngay trước kỳ hiện tại, dùng để tính % tăng giảm */
private fun StatsPeriod.previousWindowKeys(): Set<String> = windowKeys(months)

private fun monthKey(monthsAgo: Int): String {
  val calendar = Calendar.getInstance()
  calendar.add(Calendar.MONTH, -monthsAgo)
  return String.format(Locale.US, "%02d/%04d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
}

/** Ngày bắt đầu – kết thúc của kỳ, ví dụ "01/10 – 29/10" */
private fun StatsPeriod.rangeText(): String? {
  if (this == StatsPeriod.ALL_TIME) return null
  val formatter = SimpleDateFormat("dd/MM", Locale.US)
  val start = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, -(months - 1))
  }
  val end = Calendar.getInstance()
  return "${formatter.format(start.time)} – ${formatter.format(end.time)}"
}

/** Tháng của hóa đơn ("MM/yyyy") để so khớp với monthlyTrend */
private fun billMonthKey(createdAt: String): String? =
  if (createdAt.length >= 7) "${createdAt.substring(5, 7)}/${createdAt.substring(0, 4)}" else null

private fun shortDate(createdAt: String): String =
  if (createdAt.length >= 10) "${createdAt.substring(8, 10)}/${createdAt.substring(5, 7)}" else createdAt

/** Quy đổi số tiền hóa đơn về tiền tệ chung của nhóm */
private fun BillResponse.normalizedAmount(): Double =
  totalAmount * (if (exchangeRate > 0) exchangeRate else 1.0)
