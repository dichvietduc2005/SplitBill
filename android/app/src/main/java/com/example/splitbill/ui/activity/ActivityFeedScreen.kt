package com.example.splitbill.ui.activity

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.ActivityResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ─── Tab & Filter enums ───

enum class ActivityTab(val title: String) {
  ALL("Tất cả"),
  BILLS("Hóa đơn"),
  PAYMENTS("Thanh toán")
}

enum class TimeFilter(val title: String) {
  ALL("Tất cả"),
  THIS_WEEK("Tuần này"),
  THIS_MONTH("Tháng này"),
  LAST_MONTH("Tháng trước")
}

// ─── Main Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
  viewModel: ActivityFeedViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Local UI state
  var selectedTab by rememberSaveable { mutableStateOf(ActivityTab.ALL) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedTimeFilter by rememberSaveable { mutableStateOf(TimeFilter.ALL) }
  var showTimeFilterMenu by remember { mutableStateOf(false) }
  var isRefreshing by remember { mutableStateOf(false) }

  // Reset refreshing when data arrives
  LaunchedEffect(uiState) {
    if (uiState is ActivityFeedUiState.Success) isRefreshing = false
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Lịch sử hoạt động".localized(),
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    when (val state = uiState) {
      is ActivityFeedUiState.Loading -> {
        com.example.splitbill.ui.components.ActivityFeedSkeleton(
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is ActivityFeedUiState.Error -> {
        Box(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(Dimens.SpacingM))
            Text(state.message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(Dimens.SpacingM))
            FilledTonalButton(onClick = { viewModel.loadActivities() }) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(Dimens.SpacingXS))
              Text("Thử lại".localized())
            }
          }
        }
      }
      is ActivityFeedUiState.Success -> {
        // Apply filters
        val filteredActivities = remember(state.activities, selectedTab, searchQuery, selectedTimeFilter) {
          state.activities
            .filter { activity ->
              // Tab filter
              when (selectedTab) {
                ActivityTab.ALL -> true
                ActivityTab.BILLS -> activity.activityType in listOf("BILL_CREATED", "BILL_UPDATED", "BILL_DELETED")
                ActivityTab.PAYMENTS -> activity.activityType == "DEBT_SETTLED"
              }
            }
            .filter { activity ->
              // Search filter
              searchQuery.isBlank() ||
                activity.username.contains(searchQuery, ignoreCase = true) ||
                activity.description.contains(searchQuery, ignoreCase = true)
            }
            .filter { activity ->
              // Time filter
              if (selectedTimeFilter == TimeFilter.ALL) true
              else {
                try {
                  val activityDate = LocalDateTime.parse(activity.createdAt.substring(0, 19)).toLocalDate()
                  val now = LocalDate.now()
                  when (selectedTimeFilter) {
                    TimeFilter.THIS_WEEK -> {
                      val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                      !activityDate.isBefore(startOfWeek)
                    }
                    TimeFilter.THIS_MONTH -> {
                      activityDate.year == now.year && activityDate.monthValue == now.monthValue
                    }
                    TimeFilter.LAST_MONTH -> {
                      val lastMonth = now.minusMonths(1)
                      activityDate.year == lastMonth.year && activityDate.monthValue == lastMonth.monthValue
                    }
                    else -> true
                  }
                } catch (e: Exception) { true }
              }
            }
        }

        // Group by date
        val groupedActivities = remember(filteredActivities) {
          filteredActivities.groupBy { activity ->
            try {
              LocalDateTime.parse(activity.createdAt.substring(0, 19)).toLocalDate()
            } catch (e: Exception) {
              LocalDate.now()
            }
          }.toSortedMap(compareByDescending { it })
        }

        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            viewModel.loadActivities {
              isRefreshing = false
            }
          },
          state = pullRefreshState,
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SpacingM, vertical = Dimens.SpacingS),
            verticalArrangement = Arrangement.spacedBy(0.dp)
          ) {

            // ─── Search bar + Time filter ───
            item {
              Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.SpacingS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
              ) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("Tìm kiếm...", style = MaterialTheme.typography.bodyMedium) },
                  leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                  },
                  trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                      IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Xóa", modifier = Modifier.size(18.dp))
                      }
                    }
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.weight(1f).height(52.dp),
                  textStyle = MaterialTheme.typography.bodyMedium
                )

                // Time filter button
                Box {
                  FilledIconButton(
                    onClick = { showTimeFilterMenu = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                      containerColor = if (selectedTimeFilter != TimeFilter.ALL)
                        MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp)
                  ) {
                    Icon(
                      Icons.Rounded.FilterList,
                      contentDescription = "Lọc theo thời gian",
                      tint = if (selectedTimeFilter != TimeFilter.ALL)
                        MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  DropdownMenu(
                    expanded = showTimeFilterMenu,
                    onDismissRequest = { showTimeFilterMenu = false }
                  ) {
                    TimeFilter.entries.forEach { filter ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            filter.title,
                            fontWeight = if (filter == selectedTimeFilter) FontWeight.Bold else FontWeight.Normal,
                            color = if (filter == selectedTimeFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                          )
                        },
                        onClick = {
                          selectedTimeFilter = filter
                          showTimeFilterMenu = false
                        }
                      )
                    }
                  }
                }
              }
            }

            // ─── Segmented Tab Switcher ───
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = Dimens.SpacingM)
                  .clip(RoundedCornerShape(16.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                ActivityTab.entries.forEach { tab ->
                  val isSelected = selectedTab == tab
                  val tabIcon = when (tab) {
                    ActivityTab.ALL -> Icons.AutoMirrored.Rounded.List
                    ActivityTab.BILLS -> Icons.Rounded.Receipt
                    ActivityTab.PAYMENTS -> Icons.Rounded.Payments
                  }

                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .height(40.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                      )
                      .clickable { selectedTab = tab }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center
                    ) {
                      Icon(
                        imageVector = tabIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Spacer(Modifier.width(4.dp))
                      Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }
            }

            // ─── Active filter chip ───
            if (selectedTimeFilter != TimeFilter.ALL) {
              item {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.SpacingS),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXS)
                ) {
                  Icon(
                    Icons.Rounded.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    selectedTimeFilter.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(Modifier.weight(1f))
                  TextButton(
                    onClick = { selectedTimeFilter = TimeFilter.ALL },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text("Xóa lọc", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }

            // ─── Empty State ───
            if (filteredActivities.isEmpty()) {
              item {
                Box(
                  modifier = Modifier.fillMaxWidth().height(300.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val emptyIcon = when (selectedTab) {
                      ActivityTab.ALL -> Icons.Rounded.History
                      ActivityTab.BILLS -> Icons.Rounded.Receipt
                      ActivityTab.PAYMENTS -> Icons.Rounded.Payments
                    }
                    val emptyTitle = when {
                      searchQuery.isNotBlank() -> "Không tìm thấy kết quả"
                      selectedTab == ActivityTab.BILLS -> "Chưa có hóa đơn nào"
                      selectedTab == ActivityTab.PAYMENTS -> "Chưa có thanh toán nào"
                      else -> "Chưa có hoạt động nào"
                    }
                    val emptyMsg = when {
                      searchQuery.isNotBlank() -> "Thử đổi từ khóa hoặc bộ lọc khác"
                      selectedTab == ActivityTab.BILLS -> "Hóa đơn mới sẽ xuất hiện ở đây"
                      selectedTab == ActivityTab.PAYMENTS -> "Các khoản thanh toán sẽ xuất hiện ở đây"
                      else -> "Các hoạt động trong nhóm sẽ xuất hiện ở đây"
                    }

                    Box(
                      modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        emptyIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                      )
                    }
                    Spacer(Modifier.height(Dimens.SpacingM))
                    Text(
                      emptyTitle,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Dimens.SpacingXS))
                    Text(
                      emptyMsg,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }

            // ─── Date-grouped Activity Items ───
            groupedActivities.forEach { (date, activities) ->
              // Date header
              item(key = "date_$date") {
                DateHeader(date = date)
              }

              // Activity items under this date
              itemsIndexed(
                items = activities,
                key = { _, activity -> activity.id }
              ) { index, activity ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                  kotlinx.coroutines.delay(index * 25L)
                  visible = true
                }
                AnimatedVisibility(
                  visible = visible,
                  enter = Motion.slideUp + fadeIn()
                ) {
                  ActivityItemRow(activity = activity)
                }
              }

              // Small spacing between date groups
              item(key = "spacer_$date") {
                Spacer(Modifier.height(Dimens.SpacingS))
              }
            }

            // Bottom padding
            item { Spacer(Modifier.height(32.dp)) }
          }
        }
      }
    }
  }
}

// ─── Date Header ───

@Composable
private fun DateHeader(date: LocalDate) {
  val today = LocalDate.now()
  val yesterday = today.minusDays(1)

  val dateText = when (date) {
    today -> "Hôm nay"
    yesterday -> "Hôm qua"
    else -> {
      val dayOfWeek = date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.of("vi"))
        .replaceFirstChar { it.uppercaseChar() }
      val formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
      "$dayOfWeek, $formatted"
    }
  }

  Text(
    text = dateText.uppercase(),
    style = MaterialTheme.typography.labelMedium.copy(
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp
    ),
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.SpacingS)
  )
}

// ─── Activity Item Row ───

@Composable
private fun ActivityItemRow(activity: ActivityResponse) {
  val config = getActivityConfig(activity.activityType)

  val formattedTime = remember(activity.createdAt) {
    try {
      val parsed = LocalDateTime.parse(activity.createdAt.substring(0, 19))
      parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) { "" }
  }

  // Extract amount from description (e.g. "Duc đã thêm hóa đơn 'Cơm': 200,000 VND")
  val amountText = remember(activity.description) {
    val regex = Regex("""([\d.,]+)\s*VND""")
    regex.find(activity.description)?.let { match ->
      "${match.groupValues[1]} đ"
    }
  }

  // Extract detail from description — show what bill/payment it was about
  val detailText = remember(activity.description) {
    // Remove the username prefix to get the action part
    val withoutUser = activity.description.removePrefix(activity.username).trimStart()
    // Clean it up — remove leading "đã " if present after username removal
    withoutUser.ifBlank { config.actionLabel }
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.SpacingM, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ─── Avatar / Icon circle ───
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(config.bgColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = config.icon,
          contentDescription = null,
          tint = config.tintColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(Modifier.width(12.dp))

      // ─── Name + Description ───
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = activity.username,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = detailText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      // ─── Amount + Time ───
      Column(horizontalAlignment = Alignment.End) {
        if (amountText != null) {
          Text(
            text = amountText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = config.amountColor
          )
        }
        Spacer(Modifier.height(2.dp))
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
      }


    }
  }
}

// ─── Activity Config ───

private data class ActivityConfig(
  val icon: ImageVector,
  val bgColor: Color,
  val tintColor: Color,
  val actionLabel: String,
  val amountColor: Color
)

@Composable
private fun getActivityConfig(activityType: String): ActivityConfig {
  return when (activityType) {
    "BILL_CREATED" -> ActivityConfig(
      icon = Icons.Rounded.Receipt,
      bgColor = Color(0xFF1A3D1A),
      tintColor = Color(0xFF66BB6A),
      actionLabel = "đã thêm hóa đơn",
      amountColor = MaterialTheme.colorScheme.error
    )
    "BILL_UPDATED" -> ActivityConfig(
      icon = Icons.Rounded.Edit,
      bgColor = Color(0xFF1A2A4D),
      tintColor = Color(0xFF42A5F5),
      actionLabel = "đã cập nhật hóa đơn",
      amountColor = MaterialTheme.colorScheme.primary
    )
    "BILL_DELETED" -> ActivityConfig(
      icon = Icons.Rounded.DeleteForever,
      bgColor = Color(0xFF4D1A1A),
      tintColor = Color(0xFFEF5350),
      actionLabel = "đã xóa hóa đơn",
      amountColor = MaterialTheme.colorScheme.error
    )
    "DEBT_SETTLED" -> ActivityConfig(
      icon = Icons.Rounded.Payments,
      bgColor = Color(0xFF3D3D1A),
      tintColor = Color(0xFFFFCA28),
      actionLabel = "đã thanh toán",
      amountColor = Color(0xFF66BB6A)
    )
    "MEMBER_JOINED" -> ActivityConfig(
      icon = Icons.Rounded.PersonAdd,
      bgColor = Color(0xFF2D1A4D),
      tintColor = Color(0xFFAB47BC),
      actionLabel = "đã tham gia nhóm",
      amountColor = MaterialTheme.colorScheme.onSurface
    )
    else -> ActivityConfig(
      icon = Icons.Rounded.Notifications,
      bgColor = MaterialTheme.colorScheme.surfaceVariant,
      tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
      actionLabel = "hoạt động",
      amountColor = MaterialTheme.colorScheme.onSurface
    )
  }
}
