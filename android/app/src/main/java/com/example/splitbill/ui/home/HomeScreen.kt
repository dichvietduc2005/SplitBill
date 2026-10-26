package com.example.splitbill.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.PositiveAmount
import com.example.splitbill.theme.NegativeAmount
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.HomeScreenSkeleton
import com.example.splitbill.ui.localization.localized
import java.text.NumberFormat
import java.util.Locale

private const val DEFAULT_AVATAR_URL =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuBCt8xai-5-3g4hezXcYmqmqs57iyZA1E66kiKQA02CKKB8XEGAJsc1iP216eWlIu05aMud5sC8qq6Co1_3m8oHX8KjZQ7O3DXSpMscb2kmizqsUsmd3zH_5VBNXc5e9dAER5T8OX9kBJax6JyzjKGv8eer4UdrAMlaItnXRqCsuMtxnZuUCv9Ufp4tpll4zDbBMF4u8YEfdEOFUET2qhGteWkdqJkgI9Et0maCl2-vVDHpQ9yp83XZOQ"

private val SAMPLE_GROUP_IMAGES = listOf(
  "https://lh3.googleusercontent.com/aida-public/AB6AXuB4cPFJIk1Gd902PFJDi-pJV5eVyitPpP3vy1PtvksLkxM-MCrF-bSJ2YqHr-V0hkT01TM7qS6f7JmcNi54RCOof6A7eKyFRIqxNJxylUaq2HMyuqDnbjflojP8F4weS_6LQ1qROOjwPmTIHArqP97dWo91KMAPBstBQAaMjf9B04ed3LixfGqlX2xfb2tYSQU22cL3kRkqR5tIZ9B_Auq2SqBAqsnf6JTM55-J_aQrtA2JW5HeqAcBRg",
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCjaQ1ZQyrkLNi6O4Be1ksawUkY8wgblezMFAlVUaKBbcAIUhs7hgYa28fku5rE29TB3_0umuGIeWTXMK-JdOXRST8u7XV8Dg8JJkia3x_Y59yvNKMapg6id8vQhTgR_S_iSZ1Mq1O6mY0ZOjkQlEh1rWpp1jpTMf0PRWSzwKRuNQD52jTjBZWHgK6RMGgjQwwuFhQB6ISN8JoAlVY6qREspmjJNgbZfqlfB69MUsdjO4A5IOmOseOMag",
  "https://lh3.googleusercontent.com/aida-public/AB6AXuD03LUYO0uRmL5EfrK07t053I0NHW6ZA6wFUNETsBGpMYmOCTtxvd-69vlR8BCwpIPJxQF-a5udZnhWnWPlZY8S6cnEOjv43GDuAjH6SGEVA6yKFFQI-dI71F_lzeH7NSj8SCfHPT-4XZaBfKWUNeoVfsom-V4Mg0kRPkBsTs_JiVySGqzpf9gpf0uln6tp5wIIBCGfbQESGA1EcLY40T38iAQbBeN-2dPBRh1tRKzfwZl3kkvN-eGq1Q"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeScreenViewModel,
  onNavigateToGroup: (String) -> Unit,
  onNavigateToProfile: () -> Unit,
  onNavigateToGroupsTab: () -> Unit,
  onNavigateToDebtsTab: () -> Unit,
  onCreateExpenseClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var isRefreshing by remember { mutableStateOf(false) }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadHomeData()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Scaffold(
    containerColor = Color(0xFFF9F9F6),
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        viewModel.loadHomeData { isRefreshing = false }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
      when (val state = uiState) {
        is HomeUiState.Loading -> {
          HomeScreenSkeleton(modifier = Modifier.fillMaxSize())
        }
        is HomeUiState.Error -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            com.example.splitbill.ui.components.ErrorState(
              title = "Không thể tải trang chủ".localized(),
              message = state.message,
              onRetry = { viewModel.loadHomeData() }
            )
          }
        }
        is HomeUiState.Success -> {
          val scrollState = rememberScrollState()

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
          ) {
            // 1. SCENIC HERO SECTION (Height ~ 320dp)
            ScenicHeroSection(
              userName = state.userProfile?.username ?: "Bạn",
              avatarUrl = state.userProfile?.avatarUrl ?: DEFAULT_AVATAR_URL,
              onProfileClick = onNavigateToProfile,
              onNotificationClick = { /* Nút thông báo */ }
            )

            // 2. WARM FLOATING SHEET BODY (Bo góc 32dp đè lên Hero Section)
            WarmFloatingSheet(
              totalToReceive = state.totalToReceive,
              totalToPay = state.totalToPay,
              countToReceive = state.countToReceive,
              countToPay = state.countToPay,
              groups = state.recentGroups,
              recentBills = state.recentBills,
              currentUserId = state.currentUserId,
              onCreateExpenseClick = onCreateExpenseClick,
              onNavigateToGroup = onNavigateToGroup,
              onViewAllGroups = onNavigateToGroupsTab,
              onViewAllTransactions = onNavigateToDebtsTab
            )

            Spacer(modifier = Modifier.height(100.dp))
          }
        }
      }
    }
  }
}

/**
 * 1. Scenic Hero Section with Background image & Gradients
 */
@Composable
private fun ScenicHeroSection(
  userName: String,
  avatarUrl: String,
  onProfileClick: () -> Unit,
  onNotificationClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Cho phép chuyển đổi linh hoạt hoặc tự động theo giờ thực tế
  var selectedTimeOfDay by remember { mutableStateOf<TimeOfDay?>(null) }
  val realTimeOfDay = remember { resolveCurrentTimeOfDay() }
  val currentTimeOfDay = selectedTimeOfDay ?: realTimeOfDay

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(320.dp)
  ) {
    // 1. Nền phong cảnh 2D Day/Night Cycle sống động với mặt trời/trăng chuyển động nhẹ
    DynamicDayNightBackground(
      timeOfDay = currentTimeOfDay,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Nội dung text và thao tác trên Hero Section
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Greeting & Top Actions Bar (Avatar & Notification)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          // Chip trạng thái chu kỳ (Bấm vào để đổi xem thử Bình minh -> Ban ngày -> Hoàng hôn -> Ban đêm)
          Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color.Black.copy(alpha = 0.28f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier.clickable {
              selectedTimeOfDay = when (selectedTimeOfDay) {
                null -> TimeOfDay.DAWN
                TimeOfDay.DAWN -> TimeOfDay.DAY
                TimeOfDay.DAY -> TimeOfDay.SUNSET
                TimeOfDay.SUNSET -> TimeOfDay.NIGHT
                TimeOfDay.NIGHT -> null // Quay về chế độ tự động theo giờ
              }
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "${currentTimeOfDay.emoji} ${currentTimeOfDay.label}" + (if (selectedTimeOfDay != null) " (Xem thử)" else ""),
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = currentTimeOfDay.greetingPrefix,
            color = Color(0xFFF9F9F6).copy(alpha = 0.95f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = userName,
              color = Color.White,
              fontSize = 30.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp
            )
            Text(
              text = currentTimeOfDay.emoji,
              fontSize = 24.sp
            )
          }
        }

        // Notification & Avatar Buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Notification button with badge
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.20f))
              .clickable(onClick = onNotificationClick),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Rounded.Notifications,
              contentDescription = "Thông báo",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
            // Red dot badge
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFBA1A1A))
            )
          }

          // User Avatar
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.35f))
              .clickable(onClick = onProfileClick)
              .padding(2.dp)
              .clip(CircleShape)
          ) {
            AsyncImage(
              model = avatarUrl,
              contentDescription = "Avatar",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      // Quote & Subtitle theo chu kỳ ngày đêm
      Text(
        text = currentTimeOfDay.quote,
        color = Color(0xFFF9F9F6).copy(alpha = 0.95f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 21.sp,
        modifier = Modifier.padding(bottom = 24.dp)
      )
    }
  }
}

/**
 * 2. Warm Floating Sheet Body
 */
@Composable
private fun WarmFloatingSheet(
  totalToReceive: Double,
  totalToPay: Double,
  countToReceive: Int,
  countToPay: Int,
  groups: List<HomeGroupItem>,
  recentBills: List<BillResponse>,
  currentUserId: String?,
  onCreateExpenseClick: () -> Unit,
  onNavigateToGroup: (String) -> Unit,
  onViewAllGroups: () -> Unit,
  onViewAllTransactions: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .offset(y = (-24).dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
      .background(Color(0xFFFFFFFF))
      .padding(horizontal = 20.dp, vertical = 24.dp)
  ) {
    Column {
      // SECTION: TỔNG QUAN TÀI CHÍNH
      Text(
        text = "TỔNG QUAN",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = Color(0xFF546068),
        modifier = Modifier.padding(bottom = 12.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Card "Bạn được nhận"
        FinancialSummaryCard(
          title = "Bạn được nhận",
          amount = totalToReceive,
          countText = "$countToReceive khoản",
          isIncome = true,
          modifier = Modifier.weight(1f)
        )

        // Card "Bạn cần trả"
        FinancialSummaryCard(
          title = "Bạn cần trả",
          amount = totalToPay,
          countText = "$countToPay khoản",
          isIncome = false,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // PRIMARY ACTION CTA BUTTON: "Tạo khoản chi mới"
      Button(
        onClick = onCreateExpenseClick,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF18313D),
          contentColor = Color(0xFFF9F9F6)
        ),
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(100.dp),
            spotColor = Color(0xFF18313D).copy(alpha = 0.35f)
          )
      ) {
        Icon(
          Icons.Rounded.AddCircle,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Tạo khoản chi mới",
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.2.sp
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // SECTION: NHÓM GẦN ĐÂY
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Nhóm gần đây",
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF011C27)
        )
        Text(
          text = "Xem tất cả",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF546068),
          modifier = Modifier
            .clickable(onClick = onViewAllGroups)
            .padding(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (groups.isEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F1)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Chưa có nhóm nào. Nhấn để tạo nhóm!",
              fontSize = 13.sp,
              color = Color(0xFF546068)
            )
          }
        }
      } else {
        // Horizontal Card Previews
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          itemsIndexed(groups, key = { _, it -> it.group.id }) { index, item ->
            val bannerImage = item.group.bannerUrl?.takeIf { it.isNotBlank() }
              ?: SAMPLE_GROUP_IMAGES[index % SAMPLE_GROUP_IMAGES.size]
            GroupHorizontalCard(
              groupItem = item,
              bannerUrl = bannerImage,
              onClick = { onNavigateToGroup(item.group.id) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // SECTION: GIAO DỊCH GẦN ĐÂY
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Giao dịch gần đây",
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF011C27)
        )
        Text(
          text = "Xem tất cả",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF546068),
          modifier = Modifier
            .clickable(onClick = onViewAllTransactions)
            .padding(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Danh sách các giao dịch gần đây
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (recentBills.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Chưa có giao dịch gần đây",
              fontSize = 14.sp,
              color = Color(0xFF546068)
            )
          }
        } else {
          Column {
            recentBills.take(5).forEachIndexed { idx, bill ->
              TransactionItemRow(
                bill = bill,
                currentUserId = currentUserId,
                onClick = { onNavigateToGroup(bill.groupId) }
              )
              if (idx < minOf(recentBills.size - 1, 4)) {
                HorizontalDivider(
                  thickness = 1.dp,
                  color = Color(0xFFEEEEEB),
                  modifier = Modifier.padding(horizontal = 12.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Thẻ thống kê tài chính "Bạn được nhận" / "Bạn cần trả"
 */
@Composable
private fun FinancialSummaryCard(
  title: String,
  amount: Double,
  countText: String,
  isIncome: Boolean,
  modifier: Modifier = Modifier
) {
  val iconColor = if (isIncome) Color(0xFF239B75) else Color(0xFFBA1A1A)
  val iconVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
  val formattedAmount = formatVndAmount(amount)

  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F1)),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFFD5E1EB).copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
          )
        }
        Text(
          text = title,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF546068),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Column {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = formattedAmount,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = iconColor,
            letterSpacing = (-0.5).sp
          )
          Text(
            text = "đ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = iconColor,
            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
          )
        }
        Text(
          text = countText,
          fontSize = 12.sp,
          color = Color(0xFF8F9799),
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}

/**
 * Thẻ nhóm dạng lướt ngang (Horizontal Card Preview)
 */
@Composable
private fun GroupHorizontalCard(
  groupItem: HomeGroupItem,
  bannerUrl: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F1)),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .width(176.dp)
      .clickable(onClick = onClick)
  ) {
    Column {
      // Banner Image with Member Count Badge
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
      ) {
        AsyncImage(
          model = bannerUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Badge: 6 thành viên
        Box(
          modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF011C27).copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "${groupItem.group.memberCount} thành viên",
            color = Color(0xFFF9F9F6),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // Card Information
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
      ) {
        Text(
          text = groupItem.group.name,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF1A1C1B),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = formatRelativeTime(groupItem.group.createdAt),
          fontSize = 11.sp,
          color = Color(0xFF546068),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        // Stacked Mini Avatars
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          val displayedMembers = groupItem.members.take(3)
          displayedMembers.forEachIndexed { i, member ->
            Box(
              modifier = Modifier
                .offset(x = (-i * 6).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4F4F1))
                .padding(1.dp)
                .clip(CircleShape)
            ) {
              if (!member.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                  model = member.avatarUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD5E1EB)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = member.username.take(1).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18313D)
                  )
                }
              }
            }
          }

          val remaining = groupItem.group.memberCount - displayedMembers.size
          if (remaining > 0) {
            Box(
              modifier = Modifier
                .offset(x = (-displayedMembers.size * 6).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFD5E1EB)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "+$remaining",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF58646D)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Dòng giao dịch gần đây (Divided with soft tonal separation)
 */
@Composable
private fun TransactionItemRow(
  bill: BillResponse,
  currentUserId: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isPayer = bill.paidByUserId == currentUserId
  val splitAmount = bill.splits.find { it.userId == currentUserId }?.amountOwed ?: 0.0

  val categoryEnum = BillCategory.fromKey(bill.category)

  // Xác định số tiền và nhãn hiển thị
  val displayAmount: String
  val amountColor: Color
  val subtitleLabel: String

  if (isPayer) {
    displayAmount = "-${formatVndAmount(bill.totalAmount)}đ"
    amountColor = Color(0xFFBA1A1A)
    subtitleLabel = "Bạn chi"
  } else if (splitAmount > 0) {
    displayAmount = "-${formatVndAmount(splitAmount)}đ"
    amountColor = Color(0xFFBA1A1A)
    subtitleLabel = "Chia ${bill.splits.size} người"
  } else {
    displayAmount = "+${formatVndAmount(bill.totalAmount)}đ"
    amountColor = Color(0xFF239B75)
    subtitleLabel = "Hoàn tiền"
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      // Category Icon Circular Container
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(categoryEnum.bgColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = categoryEnum.icon,
          contentDescription = null,
          tint = categoryEnum.iconColor,
          modifier = Modifier.size(20.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = bill.description.ifBlank { "Hóa đơn không tên" },
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF1A1C1B),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${categoryEnum.displayName} • ${formatShortDate(bill.createdAt)}",
          fontSize = 12.sp,
          color = Color(0xFF546068),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.End,
      modifier = Modifier.padding(start = 8.dp)
    ) {
      Text(
        text = displayAmount,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = amountColor
      )
      Text(
        text = subtitleLabel,
        fontSize = 11.sp,
        color = Color(0xFF8F9799)
      )
    }
  }
}

/**
 * Định dạng tiền tệ VND (dấu chấm ngăn cách phần nghìn)
 */
private fun formatVndAmount(amount: Double): String {
  val formatter = NumberFormat.getNumberInstance(Locale.GERMANY) // Định dạng 1.250.000
  return formatter.format(amount.toLong())
}

/**
 * Định dạng ngày giờ ngắn gọn (vd: "Hôm qua", "24 Th10", "2 giờ trước")
 */
private fun formatRelativeTime(dateStr: String?): String {
  if (dateStr.isNullOrBlank()) return "Gần đây"
  return try {
    if (dateStr.length >= 10) {
      val day = dateStr.substring(8, 10)
      val month = dateStr.substring(5, 7)
      "$day Th$month"
    } else "Gần đây"
  } catch (_: Exception) {
    "Gần đây"
  }
}

private fun formatShortDate(dateStr: String?): String {
  if (dateStr.isNullOrBlank()) return "Hôm nay"
  return try {
    if (dateStr.length >= 10) {
      val day = dateStr.substring(8, 10)
      val month = dateStr.substring(5, 7)
      "$day Th$month"
    } else "Gần đây"
  } catch (_: Exception) {
    "Hôm nay"
  }
}
